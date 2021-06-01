package com.bakdata.conquery.commands;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Predicate;

import javax.validation.Validator;
import javax.ws.rs.PathParam;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.cps.CPSTypeIdResolver;
import com.bakdata.conquery.io.jackson.InternalOnly;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.jackson.MutableInjectableValues;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.io.jersey.RESTServer;
import com.bakdata.conquery.io.mina.BinaryJacksonCoder;
import com.bakdata.conquery.io.mina.CQProtocolCodecFilter;
import com.bakdata.conquery.io.mina.ChunkReader;
import com.bakdata.conquery.io.mina.ChunkWriter;
import com.bakdata.conquery.io.mina.MinaAttributes;
import com.bakdata.conquery.io.mina.NetworkSession;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.auth.AuthorizationController;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.forms.frontendconfiguration.FormScanner;
import com.bakdata.conquery.models.i18n.I18n;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.jobs.ReactingJob;
import com.bakdata.conquery.models.messages.SlowMessage;
import com.bakdata.conquery.models.messages.network.MessageToManagerNode;
import com.bakdata.conquery.models.messages.network.NetworkMessageContext;
import com.bakdata.conquery.models.query.concept.ConceptQuery;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.IdResolveContext;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.models.worker.Worker;
import com.bakdata.conquery.resources.ResourcesProvider;
import com.bakdata.conquery.resources.admin.AdminServlet;
import com.bakdata.conquery.resources.admin.ShutdownTask;
import com.bakdata.conquery.resources.admin.rest.AdminDatasetResource;
import com.bakdata.conquery.resources.api.MeResource;
import com.bakdata.conquery.resources.unprotected.AuthServlet;
import com.bakdata.conquery.tasks.ClearFilterSourceSearch;
import com.bakdata.conquery.tasks.PermissionCleanupTask;
import com.bakdata.conquery.tasks.QueryCleanupTask;
import com.bakdata.conquery.tasks.ReportConsistencyTask;
import com.bakdata.conquery.util.io.ConqueryMDC;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.SimpleType;
import com.google.common.base.Throwables;
import io.dropwizard.jersey.DropwizardResourceConfig;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.setup.Environment;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Discriminator;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.tags.Tag;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.jetbrains.annotations.NotNull;

/**
 * Central node of Conquery. Hosts the frontend, api, meta data and takes care of query distribution to
 * {@link ShardNode}s and respectively the {@link Worker}s hosted on them. The {@link ManagerNode} can also
 * forward queries or results to statistic backends. Finally it collects the results of queries for access over the api.
 */
@Slf4j
@Getter
public class ManagerNode extends IoHandlerAdapter implements Managed {

	public static final String DEFAULT_NAME = "manager";

	private final String name;

	private IoAcceptor acceptor;
	private MetaStorage storage;
	private JobManager jobManager;
	private Validator validator;
	private ConqueryConfig config;
	private AdminServlet admin;
	private AuthorizationController authController;
	private ScheduledExecutorService maintenanceService;
	private DatasetRegistry datasetRegistry;
	private Environment environment;
	private List<ResourcesProvider> providers = new ArrayList<>();

	// Resources without authentication
	private DropwizardResourceConfig unprotectedAuthApi;
	private DropwizardResourceConfig unprotectedAuthAdmin;

	// For registering form providers
	private FormScanner formScanner;
	/**
	 * Flags if the instance name should be a prefix for the instances storage.
	 */
	@Getter
	@Setter
	private boolean useNameForStoragePrefix = false;

	public ManagerNode() {
		this(DEFAULT_NAME);
	}

	public ManagerNode(@NonNull String name) {
		this.name = name;
	}

	public void run(ConqueryConfig config, Environment environment) throws InterruptedException {
		this.config = config;


		this.environment = environment;
		this.validator = environment.getValidator();

		OpenAPI oasApi = new OpenAPI().info(new Info()
													.title("Conquery API")
													.version("2.0.0")
													.description("Api to use Conquery."))
									  .addTagsItem(new Tag().name("api"));

		ModelConverters.getInstance().addPackageToSkip("com.fasterxml.jackson.databind");

		ModelConverters.getInstance().addConverter(new GenericJsonObjectModelConverter());

		// Adds CPS-subtypes to model
		ModelConverters.getInstance().addConverter(new CPSSubTypeSchemaResolver());
		ModelConverters.getInstance().addConverter(new CPSBaseSchemaResolver());

		ModelConverters.getInstance().addConverter(new IdRefPathParamResolver());


		SwaggerConfiguration oasApiConfig = new SwaggerConfiguration()
													.openAPI(oasApi)
													.prettyPrint(true)
													.filterClass(ConquerySpecFilter.class.getCanonicalName())
													.resourcePackages(Set.of(AdminDatasetResource.class.getPackageName()));

		environment.jersey().register(new OpenApiResource().openApiConfiguration(oasApiConfig));

		datasetRegistry = new DatasetRegistry(config.getCluster().getEntityBucketSize());

		//inject datasets into the objectmapper
		((MutableInjectableValues) environment.getObjectMapper().getInjectableValues())
				.add(IdResolveContext.class, datasetRegistry);


		this.jobManager = new JobManager("ManagerNode", config.isFailOnError());

		this.formScanner = new FormScanner();

		config.initialize(this);

		// Initialization of internationalization
		I18n.init();

		RESTServer.configure(config, environment.jersey().getResourceConfig());


		this.maintenanceService = environment.lifecycle()
											 .scheduledExecutorService("Maintenance Service")
											 .build();

		environment.lifecycle().manage(this);

		loadNamespaces();

		loadMetaStorage();

		authController = new AuthorizationController(storage, config.getAuthorization());
		environment.lifecycle().manage(authController);

		unprotectedAuthAdmin = AuthServlet.generalSetup(environment.metrics(), config, environment.admin(), environment.getObjectMapper());
		unprotectedAuthApi = AuthServlet.generalSetup(environment.metrics(), config, environment.servlets(), environment.getObjectMapper());

		// Create AdminServlet first to make it available to the realms
		admin = new AdminServlet(this);

		authController.externalInit(this, config.getAuthentication());


		// Register default components for the admin interface
		admin.register(this);

		log.info("Registering ResourcesProvider");
		for (Class<? extends ResourcesProvider> resourceProvider : CPSTypeIdResolver.listImplementations(ResourcesProvider.class)) {
			try {
				ResourcesProvider provider = resourceProvider.getConstructor().newInstance();
				provider.registerResources(this);
				providers.add(provider);
			}
			catch (Exception e) {
				log.error("Failed to register Resource {}", resourceProvider, e);
			}
		}

		try {
			formScanner.execute(null, null);
		}
		catch (Exception e) {
			Throwables.throwIfUnchecked(e);
			throw new RuntimeException(e);
		}
		environment.admin().addTask(formScanner);
		environment.admin().addTask(
				new QueryCleanupTask(storage, Duration.of(
						config.getQueries().getOldQueriesTime().getQuantity(),
						config.getQueries().getOldQueriesTime().getUnit().toChronoUnit()
				)));
		environment.admin().addTask(new PermissionCleanupTask(storage));
		environment.admin().addTask(new ClearFilterSourceSearch());
		environment.admin().addTask(new ReportConsistencyTask(datasetRegistry));

		ShutdownTask shutdown = new ShutdownTask();
		environment.admin().addTask(shutdown);
		environment.lifecycle().addServerLifecycleListener(shutdown);
	}

	private static Class<? extends Identifiable<?>> extractIdentifableClass(AnnotatedType type) {
		if (type.getType() instanceof Class && Identifiable.class.isAssignableFrom((Class<?>) type.getType())) {
			return (Class<? extends Identifiable<?>>) type.getType();
		}

		if (type.getType() instanceof SimpleType && ((SimpleType) type.getType()).isTypeOrSubTypeOf(Identifiable.class)) {
			return (Class<? extends Identifiable<?>>) ((SimpleType) type.getType()).getRawClass();
		}

		if (type.getType() instanceof ParameterizedType
			&& Identifiable.class.isAssignableFrom(((Class<?>) ((ParameterizedType) type.getType()).getRawType()))) {
			return (Class<? extends Identifiable<?>>) ((ParameterizedType) type.getType()).getRawType();
		}

		return null;
	}

	private static <T> Class<T> extractClass(AnnotatedType type) {

		if (type.getType() instanceof Class) {
			return (Class<T>) type.getType();
		}

		if (type.getType() instanceof JavaType) {
			return (Class<T>) ((JavaType) type.getType()).getRawClass();
		}

		if (type.getType() instanceof ParameterizedType) {
			return (Class<T>) ((ParameterizedType) type.getType()).getRawType();
		}

		return null;
	}

	@NotNull
	private static Schema<Id<? extends Identifiable<?>>> createIdSchema(Class<? extends Id<?>> idClass) {
		final Schema<Id<? extends Identifiable<?>>> idSchema = new Schema<>();

		idSchema.setName(idClass.getSimpleName());
		idSchema.setType("string");
		idSchema.setFormat("[a-z0-9](.[a-z0-9]+)*");
		return idSchema;
	}

	private void loadMetaStorage() {
		log.info("Started meta storage");
		this.storage =
				new MetaStorage(validator, config.getStorage(), ConqueryCommand.getStoragePathParts(useNameForStoragePrefix, getName()), datasetRegistry);
		this.storage.loadData();
		log.info("MetaStorage loaded {}", this.storage);

		datasetRegistry.setMetaStorage(this.storage);
		for (Namespace sn : datasetRegistry.getDatasets()) {
			sn.getStorage().setMetaStorage(storage);
		}
	}

	public void loadNamespaces() {
		for (NamespaceStorage namespaceStorage : config.getStorage()
													   .loadNamespaceStorages(ConqueryCommand.getStoragePathParts(useNameForStoragePrefix, getName()))) {
			Namespace
					ns =
					new Namespace(namespaceStorage, config.isFailOnError(), config.configureObjectMapper(Jackson.BINARY_MAPPER)
																				  .writerWithView(InternalOnly.class));
			datasetRegistry.add(ns);
		}
	}

	@Override
	public void sessionOpened(IoSession session) throws Exception {
		ConqueryMDC.setLocation("ManagerNode[" + session.getLocalAddress().toString() + "]");
		log.info("New client {} connected, waiting for identity", session.getRemoteAddress());
	}

	@Override
	public void sessionClosed(IoSession session) throws Exception {
		ConqueryMDC.setLocation("ManagerNode[" + session.getLocalAddress().toString() + "]");
		log.info("Client '{}' disconnected ", session.getAttribute(MinaAttributes.IDENTIFIER));
	}

	@Override
	public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
		ConqueryMDC.setLocation("ManagerNode[" + session.getLocalAddress().toString() + "]");
		log.error("caught exception", cause);
	}

	@Override
	public void messageReceived(IoSession session, Object message) throws Exception {
		ConqueryMDC.setLocation("ManagerNode[" + session.getLocalAddress().toString() + "]");
		if (message instanceof MessageToManagerNode) {
			MessageToManagerNode mrm = (MessageToManagerNode) message;
			log.trace("ManagerNode received {} from {}", message.getClass().getSimpleName(), session.getRemoteAddress());
			ReactingJob<MessageToManagerNode, NetworkMessageContext.ManagerNodeNetworkContext>
					job =
					new ReactingJob<>(mrm, new NetworkMessageContext.ManagerNodeNetworkContext(
							jobManager,
							new NetworkSession(session),
							datasetRegistry, config.getCluster().getBackpressure()
					));

			// TODO: 01.07.2020 FK: distribute messages/jobs to their respective JobManagers (if they have one)
			if (mrm.isSlowMessage()) {
				((SlowMessage) mrm).setProgressReporter(job.getProgressReporter());
				jobManager.addSlowJob(job);
			}
			else {
				jobManager.addFastJob(job);
			}
		}
		else {
			log.error("Unknown message type {} in {}", message.getClass(), message);
			return;
		}
	}

	@Override
	public void start() throws Exception {
		acceptor = new NioSocketAcceptor();

		ObjectMapper om = Jackson.BINARY_MAPPER.copy();
		config.configureObjectMapper(om);
		BinaryJacksonCoder coder = new BinaryJacksonCoder(datasetRegistry, validator, om);
		acceptor.getFilterChain().addLast("codec", new CQProtocolCodecFilter(new ChunkWriter(coder), new ChunkReader(coder, om)));
		acceptor.setHandler(this);
		acceptor.getSessionConfig().setAll(config.getCluster().getMina());
		acceptor.bind(new InetSocketAddress(config.getCluster().getPort()));
		log.info("Started ManagerNode @ {}", acceptor.getLocalAddress());
	}

	@Override
	public void stop() throws Exception {
		jobManager.close();

		datasetRegistry.close();

		try {
			acceptor.dispose();
		}
		catch (Exception e) {
			log.error(acceptor + " could not be closed", e);
		}

		for (ResourcesProvider provider : providers) {
			try {
				provider.close();
			}
			catch (Exception e) {
				log.error(provider + " could not be closed", e);
			}

		}
		try {
			storage.close();
		}
		catch (Exception e) {
			log.error(storage + " could not be closed", e);
		}
	}

	private static class IdRefPathParamResolver implements ModelConverter {
		public Schema resolve(AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain) {

			if(type.isResolveAsRef()){
				return chain.next().resolve(type,context,chain);
			}

			//TODO NsIdRef is not working yet
			if (type.getCtxAnnotations() != null && Arrays.stream(type.getCtxAnnotations())
														  .anyMatch(InternalOnly.class::isInstance)) {
				return null;
			}

			// All PathParams are treated as Id, all NsIdRef are treated as Id
			if (type.getCtxAnnotations() == null
				|| Arrays.stream(type.getCtxAnnotations())
						 .noneMatch(((Predicate<Annotation>) PathParam.class::isInstance).or(NsIdRef.class::isInstance))) {
				return chain.next().resolve(type, context, chain);
			}
			//TODO IdRef-Collection is not handled yet

			Class<? extends Identifiable<?>> clazz = ManagerNode.extractIdentifableClass(type);

			if (clazz == null) {
				return chain.next().resolve(type, context, chain);
			}

			// We create the schema for Ids manually as annotations always coerce them to object which makes apis terrible

			final Class<Id<?>> idClass = Id.findIdClass(clazz);
			final Schema<Id<? extends Identifiable<?>>> idSchema = ManagerNode.createIdSchema(idClass);

			context.defineModel(idSchema.getName(), idSchema);

			// I wanted to also put the concrete Id-Type into the models, but then it starts resolving them as strings
			return new Schema().$ref(idSchema.getName());
		}
	}

	private static class CPSSubTypeSchemaResolver implements ModelConverter {
		public Schema resolve(AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain) {

			if(type.isResolveAsRef()){
				return chain.next().resolve(type,context,chain);
			}

			final Class<?> base = ManagerNode.extractClass(type);

			if (base == null || base.getAnnotation(CPSType.class) == null) {
				return chain.hasNext() ? chain.next().resolve(type, context, chain) : null;
			}

			final CPSType cpsType = base.getAnnotation(CPSType.class);

			final JsonTypeInfo typeInfo = cpsType.base().getAnnotation(JsonTypeInfo.class);

			final ModelConverter next = chain.next();

			final Schema<?> resolved = next.resolve(new AnnotatedType(type.getType()).resolveAsRef(false), context, chain);

			if(resolved.getRequired() == null || !resolved.getRequired().contains(typeInfo.property())) {
				resolved.addRequiredItem(typeInfo.property());
			}

			final ComposedSchema out = new ComposedSchema();

			out.addAllOfItem(resolved);
			final Schema baseRefResolved = context.resolve(new AnnotatedType(cpsType.base()).resolveAsRef(true));
			out.addAllOfItem(baseRefResolved);

			context.defineModel(base.getSimpleName(), out, type, resolved.getName());

			return next.resolve(type, context, chain);
		}

	}

		private static class CPSBaseSchemaResolver implements ModelConverter {
		@Override
		public Schema resolve(AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain) {

			if(type.isResolveAsRef()){
				return chain.next().resolve(type,context,chain);
			}

			final Class<?> base = ManagerNode.extractClass(type);

			if (base == null || base.getAnnotation(CPSBase.class) == null) {
				return chain.next().resolve(type, context, chain);
			}

			final Set<Class<?>> implementations = CPSTypeIdResolver.listImplementations((Class) base);

			final Discriminator discriminator = new Discriminator();

			final JsonTypeInfo typeInfo = base.getAnnotation(JsonTypeInfo.class);

			if (typeInfo != null) {
				discriminator.propertyName(typeInfo.property());
			}

			final ModelConverter next = chain.next();

			// We cannot trust the type to be not a ref so we force it to be one for our evaluation
			final AnnotatedType concreteType = new AnnotatedType(type.getType()).resolveAsRef(false);

			final Schema<?> resolvedBase = next.resolve(concreteType, context, chain);

			if (resolvedBase == null) {
				return new Schema();
			}

			// Already resolved it so, we won't do that again.
			if(resolvedBase instanceof ComposedSchema){
				return resolvedBase;
			}

			final ComposedSchema outSchema = new ComposedSchema();
			outSchema.properties(resolvedBase.getProperties());

			// force evaluation of all subtypes
			for (Class<?> clazz : implementations) {
				final String name = clazz.getSimpleName();
				final String cpsId = clazz.getAnnotation(CPSType.class).id();

				// Force evaluation of subtype
				context.resolve(new AnnotatedType(clazz));

				// Add discriminator and parent-child relationship to schema
				discriminator.mapping(cpsId, name);

				outSchema.addOneOfItem(new Schema().$ref(name));
			}

			outSchema.discriminator(discriminator);

			context.defineModel(base.getSimpleName(), outSchema, concreteType, resolvedBase.getName());

			return next.resolve(type, context, chain);
		}
	}

	private static class GenericJsonObjectModelConverter implements ModelConverter {
		@Override
		public Schema resolve(AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain) {

			if (type.getType().getTypeName().contains("ObjectNode")) {
				return context.resolve(type.type(Object.class));
			}

			if (type.getType().getTypeName().contains("JsonNode")) {
				return context.resolve(type.type(Object.class));
			}

			if (chain.hasNext()) {
				return chain.next().resolve(type, context, chain);
			}

			return null;
		}
	}
}

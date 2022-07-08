package com.bakdata.conquery.resources.admin;

import static com.bakdata.conquery.resources.ResourceConstants.ADMIN_SERVLET_PATH;
import static com.bakdata.conquery.resources.ResourceConstants.ADMIN_UI_SERVLET_PATH;

import java.util.Collections;

import javax.validation.Validator;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.io.freemarker.Freemarker;
import com.bakdata.conquery.io.jackson.IdRefPathParamConverterProvider;
import com.bakdata.conquery.io.jackson.PathParamInjector;
import com.bakdata.conquery.io.jersey.IdParamConverter;
import com.bakdata.conquery.io.jersey.RESTServer;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.web.AuthCookieFilter;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.resources.admin.rest.AdminConceptsResource;
import com.bakdata.conquery.resources.admin.rest.AdminDatasetProcessor;
import com.bakdata.conquery.resources.admin.rest.AdminDatasetResource;
import com.bakdata.conquery.resources.admin.rest.AdminDatasetsResource;
import com.bakdata.conquery.resources.admin.rest.AdminProcessor;
import com.bakdata.conquery.resources.admin.rest.AdminResource;
import com.bakdata.conquery.resources.admin.rest.AdminTablesResource;
import com.bakdata.conquery.resources.admin.rest.AuthOverviewResource;
import com.bakdata.conquery.resources.admin.rest.GroupResource;
import com.bakdata.conquery.resources.admin.rest.PermissionResource;
import com.bakdata.conquery.resources.admin.rest.RoleResource;
import com.bakdata.conquery.resources.admin.rest.UIProcessor;
import com.bakdata.conquery.resources.admin.rest.UserResource;
import com.bakdata.conquery.resources.admin.ui.AdminUIResource;
import com.bakdata.conquery.resources.admin.ui.AuthOverviewUIResource;
import com.bakdata.conquery.resources.admin.ui.ConceptsUIResource;
import com.bakdata.conquery.resources.admin.ui.DatasetsUIResource;
import com.bakdata.conquery.resources.admin.ui.GroupUIResource;
import com.bakdata.conquery.resources.admin.ui.RoleUIResource;
import com.bakdata.conquery.resources.admin.ui.TablesUIResource;
import com.bakdata.conquery.resources.admin.ui.UserUIResource;
import io.dropwizard.jersey.DropwizardResourceConfig;
import io.dropwizard.jersey.jackson.JacksonMessageBodyProvider;
import io.dropwizard.views.ViewMessageBodyWriter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.servlet.ServletContainer;

/**
 * Organizational class to provide a single implementation point for configuring
 * the admin servlet container and registering resources for it.
 */
@Getter
@Slf4j
public class AdminServlet {

	public static final String ADMIN_UI = "admin-ui";
	private final AdminProcessor adminProcessor;
	private final DropwizardResourceConfig jerseyConfig;
	private final AdminDatasetProcessor adminDatasetProcessor;
	private final DropwizardResourceConfig jerseyConfigUI;

	public AdminServlet(ManagerNode manager) {
		jerseyConfig = new DropwizardResourceConfig(manager.getEnvironment().metrics());
		jerseyConfig.setUrlPattern("/admin");
		jerseyConfigUI = new DropwizardResourceConfig(manager.getEnvironment().metrics());
		jerseyConfigUI.setUrlPattern("/admin-ui");

		RESTServer.configure(manager.getConfig(), jerseyConfig);

		manager.getEnvironment().admin().addServlet(ADMIN_SERVLET_PATH, new ServletContainer(jerseyConfig)).addMapping("/" + ADMIN_SERVLET_PATH + "/*");
		manager.getEnvironment().admin().addServlet(ADMIN_UI_SERVLET_PATH, new ServletContainer(jerseyConfigUI)).addMapping("/" + ADMIN_UI_SERVLET_PATH + "/*");

		jerseyConfig.register(new JacksonMessageBodyProvider(manager.getEnvironment().getObjectMapper()));
		// freemarker support
		jerseyConfigUI.register(new ViewMessageBodyWriter(manager.getEnvironment().metrics(), Collections.singleton(Freemarker.HTML_RENDERER)));


		adminProcessor = new AdminProcessor(
				manager.getConfig(),
				manager.getStorage(),
				manager.getDatasetRegistry(),
				manager.getJobManager(),
				manager.getMaintenanceService(),
				manager.getValidator()
		);

		adminDatasetProcessor = new AdminDatasetProcessor(
				manager.getConfig(),
				manager.getValidator(),
				manager.getDatasetRegistry(),
				manager.getJobManager()
		);

		jerseyConfig.register(new AbstractBinder() {
			@Override
			protected void configure() {
				bind(manager.getDatasetRegistry()).to(DatasetRegistry.class);
				bind(manager.getStorage()).to(MetaStorage.class);
				bind(manager.getValidator()).to(Validator.class);
				bind(manager.getJobManager()).to(JobManager.class);
				bind(manager.getConfig()).to(ConqueryConfig.class);
				bindAsContract(AdminProcessor.class);
				bindAsContract(AdminDatasetProcessor.class);
				bind(manager.getDatasetRegistry()).to(DatasetRegistry.class);
			}
		});

		jerseyConfig.register(new AbstractBinder() {
			@Override
			protected void configure() {
				bind(manager.getDatasetRegistry()).to(DatasetRegistry.class);
				bind(manager.getStorage()).to(MetaStorage.class);
				bind(manager.getValidator()).to(Validator.class);
				bindAsContract(AdminProcessor.class);
				bindAsContract(UIProcessor.class);
			}
		});

		jerseyConfigUI.register(new AbstractBinder() {
			@Override
			protected void configure() {
				bind(manager.getDatasetRegistry()).to(DatasetRegistry.class);
				bind(manager.getStorage()).to(MetaStorage.class);
			}
		});

		jerseyConfig.register(AdminPermissionFilter.class);
		jerseyConfigUI.register(AdminPermissionFilter.class);
		jerseyConfig.register(IdRefPathParamConverterProvider.class);
		jerseyConfigUI.register(IdRefPathParamConverterProvider.class);
		jerseyConfig.register(PathParamInjector.class);

		// register features
		final AuthCookieFilter authCookieFilter = manager.getConfig().getAuthentication().getAuthCookieFilter();
		jerseyConfig
				.register(new MultiPartFeature())
				.register(IdParamConverter.Provider.INSTANCE)
				.register(authCookieFilter)
				.register(manager.getAuthController().getAuthenticationFilter());


		jerseyConfigUI
				.register(authCookieFilter)
				.register(manager.getAuthController().getRedirectingAuthFilter());
	}

	public void register() {

		// register root resources
		jerseyConfig
				.register(AdminDatasetResource.class)
				.register(AdminDatasetsResource.class)
				.register(AdminConceptsResource.class)
				.register(AdminTablesResource.class)
				.register(RoleResource.class)
				.register(UserResource.class)
				.register(GroupResource.class)
				.register(PermissionResource.class)
				.register(AuthOverviewResource.class)
				.register(AdminResource.class);

		jerseyConfigUI
				.register(AdminUIResource.class)
				.register(RoleUIResource.class)
				.register(UserUIResource.class)
				.register(GroupUIResource.class)
				.register(DatasetsUIResource.class)
				.register(TablesUIResource.class)
				.register(ConceptsUIResource.class)
				.register(AuthOverviewUIResource.class);

	}
}

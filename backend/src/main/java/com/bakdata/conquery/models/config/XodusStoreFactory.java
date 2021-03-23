package com.bakdata.conquery.models.config;

import static com.bakdata.conquery.io.storage.StoreInfo.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.validation.Valid;
import javax.validation.Validator;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.commands.ShardNode;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.storage.ConqueryStore;
import com.bakdata.conquery.io.storage.IStoreInfo;
import com.bakdata.conquery.io.storage.IdentifiableStore;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.io.storage.StoreInfo;
import com.bakdata.conquery.io.storage.WorkerStorage;
import com.bakdata.conquery.io.storage.xodus.stores.BigStore;
import com.bakdata.conquery.io.storage.xodus.stores.CachedStore;
import com.bakdata.conquery.io.storage.xodus.stores.SerializingStore;
import com.bakdata.conquery.io.storage.xodus.stores.SingletonStore;
import com.bakdata.conquery.io.storage.xodus.stores.WeakCachedStore;
import com.bakdata.conquery.io.storage.xodus.stores.XodusStore;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.Role;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.concepts.StructureNode;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.forms.configs.FormConfig;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.mapping.PersistentIdMap;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.SingletonNamespaceCollection;
import com.bakdata.conquery.models.worker.WorkerInformation;
import com.bakdata.conquery.models.worker.WorkerToBucketsMap;
import com.bakdata.conquery.util.io.ConqueryMDC;
import com.bakdata.conquery.util.io.FileUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Multimaps;
import io.dropwizard.util.Duration;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.Environments;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
@ToString
@CPSType(id = "XODUS", base = StoreFactory.class)
public class XodusStoreFactory implements StoreFactory {

	private Path directory = Path.of("storage");

	private boolean validateOnWrite = false;
	@NotNull
	@Valid
	private XodusConfig xodus = new XodusConfig();

	private boolean useWeakDictionaryCaching = true;
	@NotNull
	private Duration weakCacheDuration = Duration.hours(48);

	@Min(1)
	private int nThreads = Runtime.getRuntime().availableProcessors();

	/**
	 * Flag for the {@link SerializingStore} whether to delete values from the underlying store, that cannot be mapped to an object anymore.
	 */
	private boolean removeUnreadableFromStore = false;

	/**
	 * When set, all values that could not be deserialized from the persistent store, are dump into individual files.
	 */
	private Optional<File> unreadableDataDumpDirectory = Optional.empty();

	@JsonIgnore
	private transient Validator validator;

	@JsonIgnore
	private BiMap<File, Environment> activeEnvironments = HashBiMap.create();

	@JsonIgnore
	private transient Multimap<Environment, XodusStore> openStoresInEnv = Multimaps.synchronizedSetMultimap(MultimapBuilder.hashKeys().hashSetValues().build());

	@Override
	public void init(ManagerNode managerNode) {
		validator = managerNode.getValidator();
	}

	@Override
	public void init(ShardNode shardNode) {
		validator = shardNode.getValidator();
	}

	@Override
	@SneakyThrows
	public Collection<NamespaceStorage> loadNamespaceStorages(ManagerNode managerNode, Path pathName) {
		@NonNull File baseDir = getStorageDir(pathName);

		if (baseDir.mkdirs()) {
			log.warn("Had to create Storage Dir at `{}`", getDirectory());
		}

		ConcurrentLinkedQueue<NamespaceStorage> storages = new ConcurrentLinkedQueue<>();

		ExecutorService loaders = Executors.newFixedThreadPool(getNThreads());


		for (File directory : baseDir.listFiles((file, name) -> name.startsWith("dataset_"))) {
			loaders.submit(() -> {
				ConqueryMDC.setLocation(directory.toString());

				if (!environmentHasStores(directory.toPath())) {
					log.warn("No valid NamespaceStorage found.");
					return;
				}

				NamespaceStorage namespaceStorage = new NamespaceStorage(validator, this, directory.toPath());
				namespaceStorage.loadData();

				storages.add(namespaceStorage);

				ConqueryMDC.clearLocation();
			});
		}


		loaders.shutdown();
		while (!loaders.awaitTermination(1, TimeUnit.MINUTES)) {
			log.debug("Still waiting for Datasets to load. {} already finished.", storages);
		}

		log.info("All NamespaceStores loaded: {}", storages);
		return storages;
	}

	@Override
	@SneakyThrows
	public Collection<WorkerStorage> loadWorkerStorages(ShardNode shardNode, Path pathName) {
		@NonNull File baseDir = getStorageDir(pathName);

		if (baseDir.mkdirs()) {
			log.warn("Had to create Storage Dir at `{}`", baseDir);
		}


		ConcurrentLinkedQueue<WorkerStorage> storages = new ConcurrentLinkedQueue<>();
		ExecutorService loaders = Executors.newFixedThreadPool(getNThreads());


		for (File directory : baseDir.listFiles((file, name) -> name.startsWith("worker_"))) {

			loaders.submit(() -> {
				//TODO maybe i need to relativize directory?
				ConqueryMDC.setLocation(directory.toString());

				if (!environmentHasStores(directory.toPath())) {
					log.warn("No valid WorkerStorage found.");
					return;
				}

				WorkerStorage workerStorage = new WorkerStorage(validator, this, directory.toPath());
				workerStorage.loadData();

				storages.add(workerStorage);

				ConqueryMDC.clearLocation();
			});
		}

		loaders.shutdown();
		while (!loaders.awaitTermination(1, TimeUnit.MINUTES)) {
			log.debug("Waiting for Worker storages to load. {} are already finished.", storages.size());
		}

		log.info("All WorkerStores loaded: {}", storages);
		return storages;
	}

	private boolean environmentHasStores(Path pathName) {
		Environment env = findEnvironment(pathName);
		boolean exists = env.computeInTransaction(t -> env.storeExists(StoreInfo.DATASET.getName(), t));

		if (!exists) {
			closeEnvironment(env);
			return false;
		}

		return !env.computeInTransaction(env::getAllStoreNames).isEmpty();
	}

	@Override
	public SingletonStore<Dataset> createDatasetStore(Path pathName) {
		return DATASET.singleton(createStore(findEnvironment(pathName), validator, DATASET));
	}

	@Override
	public IdentifiableStore<SecondaryIdDescription> createSecondaryIdDescriptionStore(CentralRegistry centralRegistry, Path pathName) {
		return SECONDARY_IDS.identifiable(createStore(findEnvironment(pathName), validator, SECONDARY_IDS), centralRegistry);
	}

	@Override
	public IdentifiableStore<Table> createTableStore(CentralRegistry centralRegistry, Path pathName) {
		return TABLES.identifiable(createStore(findEnvironment(pathName), validator, TABLES), centralRegistry);
	}

	@Override
	public IdentifiableStore<Dictionary> createDictionaryStore(CentralRegistry centralRegistry, Path pathName) {
		if (useWeakDictionaryCaching) {
			return StoreInfo.DICTIONARIES.identifiableCachedStore(createBigWeakStore(findEnvironment(pathName), validator, StoreInfo.DICTIONARIES), centralRegistry);
		}
		else {
			return StoreInfo.DICTIONARIES.identifiable(createBigStore(findEnvironment(pathName), validator, StoreInfo.DICTIONARIES), centralRegistry);
		}
	}

	@Override
	public IdentifiableStore<Concept<?>> createConceptStore(CentralRegistry centralRegistry, Path pathName) {
		return CONCEPTS.identifiable(createStore(findEnvironment(pathName), validator, CONCEPTS), centralRegistry);
	}

	@Override
	public IdentifiableStore<Import> createImportStore(CentralRegistry centralRegistry, Path pathName) {
		return IMPORTS.identifiable(createStore(findEnvironment(pathName), validator, IMPORTS), centralRegistry);
	}

	@Override
	public IdentifiableStore<CBlock> createCBlockStore(CentralRegistry centralRegistry, Path pathName) {
		return C_BLOCKS.identifiable(createStore(findEnvironment(pathName), validator, C_BLOCKS), centralRegistry);
	}

	@Override
	public IdentifiableStore<Bucket> createBucketStore(CentralRegistry centralRegistry, Path pathName) {
		return BUCKETS.identifiable(createStore(findEnvironment(pathName), validator, BUCKETS), centralRegistry);
	}

	@Override
	public SingletonStore<WorkerInformation> createWorkerInformationStore(Path pathName) {
		return WORKER.singleton(createStore(findEnvironment(pathName), validator, WORKER));
	}

	@Override
	public SingletonStore<PersistentIdMap> createIdMappingStore(Path pathName) {
		return ID_MAPPING.singleton(createStore(findEnvironment(pathName), validator, ID_MAPPING));
	}

	@Override
	public SingletonStore<WorkerToBucketsMap> createWorkerToBucketsStore(Path pathName) {
		return WORKER_TO_BUCKETS.singleton(createStore(findEnvironment(pathName), validator, WORKER_TO_BUCKETS));
	}

	@Override
	public SingletonStore<StructureNode[]> createStructureStore(Path pathName, SingletonNamespaceCollection centralRegistry) {
		return STRUCTURE.singleton(createStore(findEnvironment(pathName), validator, STRUCTURE), centralRegistry);
	}

	@Override
	public IdentifiableStore<ManagedExecution<?>> createExecutionsStore(CentralRegistry centralRegistry, DatasetRegistry datasetRegistry, Path pathName) {
		return EXECUTIONS.identifiable(createStore(findEnvironment(pathName.resolve("executions")), validator, EXECUTIONS), centralRegistry, datasetRegistry);
	}

	@Override
	public IdentifiableStore<FormConfig> createFormConfigStore(CentralRegistry centralRegistry, Path pathName) {
		return FORM_CONFIG.identifiable(createStore(findEnvironment(pathName.resolve("formConfigs")), validator, FORM_CONFIG), centralRegistry);
	}

	@Override
	public IdentifiableStore<User> createUserStore(CentralRegistry centralRegistry, Path pathName) {
		return AUTH_USER.identifiable(createStore(findEnvironment(pathName.resolve("users")), validator, AUTH_USER), centralRegistry);
	}

	@Override
	public IdentifiableStore<Role> createRoleStore(CentralRegistry centralRegistry, Path pathName) {
		return AUTH_ROLE.identifiable(createStore(findEnvironment(pathName.resolve("roles")), validator, AUTH_ROLE), centralRegistry);
	}


	@Override
	public IdentifiableStore<Group> createGroupStore(CentralRegistry centralRegistry, Path pathName) {
		return AUTH_GROUP.identifiable(createStore(findEnvironment(pathName.resolve("groups")), validator, AUTH_GROUP), centralRegistry);
	}

	/**
	 * Resolve path relative to directory.
	 */
	@NonNull
	@JsonIgnore
	private File getStorageDir(Path pathName) {
		return getDirectory().resolve(pathName).toFile();
	}

	private Environment findEnvironment(Path pathName) {
		synchronized (activeEnvironments) {
			File path = getStorageDir(pathName);
			return activeEnvironments.computeIfAbsent(path, (p) -> Environments.newInstance(p, getXodus().createConfig()));
		}
	}

	private void storeClosed(XodusStore store) {
		final Environment environment = store.getEnvironment();
		final Collection<XodusStore> stores = openStoresInEnv.get(environment);

		stores.remove(store);

		if (stores.isEmpty()) {
			closeEnvironment(environment);
		}
	}

	private void removedStore(XodusStore store) {
		final Environment environment = store.getEnvironment();
		final Collection<XodusStore> stores = openStoresInEnv.get(environment);

		environment.executeInTransaction(txn -> environment.removeStore(store.getStore().getName(), txn));

		stores.remove(store);

		if (stores.isEmpty()) {
			closeEnvironment(environment);
			removeEnvironment(environment);
		}
	}

	private void closeEnvironment(@NonNull Environment env) {
		synchronized (activeEnvironments) {

			if (activeEnvironments.inverse().remove(env) == null) {
				log.warn("Environment[{}] was not registered.", env.getLocation());
			}

			env.close();
		}
	}

	private void removeEnvironment(Environment env) {
		if(activeEnvironments.containsValue(env)){
			throw new IllegalStateException(String.format("Trying to remove Environment(%s) that was not closed.", env.getLocation()) );
		}

		log.info("Deleting environment: {}", env.getLocation());
		try {
			FileUtil.deleteRecursive(Path.of(env.getLocation()));
		}
		catch (IOException e) {
			log.error("Cannot delete directory of removed Environment[{}]", env.getLocation(), log.isDebugEnabled() ? e : null);
		}
	}

	public <KEY, VALUE> ConqueryStore<KEY, VALUE> createStore(Environment environment, Validator validator, StoreInfo storeId) {
		synchronized (openStoresInEnv) {
			return new CachedStore<>(
					new SerializingStore<>(
							this,
							createXodusStore(environment, storeId),
							validator,
							storeId
					));
		}
	}

	public XodusStore createXodusStore(Environment environment, IStoreInfo storeInfo) {
		final XodusStore xodusStore = new XodusStore(environment, storeInfo, this::storeClosed, this::removedStore);

		openStoresInEnv.get(environment)
					   .add(xodusStore);

		return xodusStore;
	}

	public <KEY, VALUE> ConqueryStore<KEY, VALUE> createBigStore(Environment environment, Validator validator, StoreInfo storeId) {
		synchronized (openStoresInEnv) {

			return storeId.cached(
					new BigStore<>(this, validator, environment, storeId)
			);
		}
	}

	public <KEY, VALUE> ConqueryStore<KEY, VALUE> createBigWeakStore(Environment environment, Validator validator, StoreInfo storeId) {
		synchronized (openStoresInEnv) {

			return new WeakCachedStore<>(
					new BigStore<>(this, validator, environment, storeId),
					getWeakCacheDuration()
			);
		}
	}
}

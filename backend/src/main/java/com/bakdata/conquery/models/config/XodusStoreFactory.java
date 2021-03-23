package com.bakdata.conquery.models.config;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.commands.ShardNode;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.storage.*;
import com.bakdata.conquery.io.storage.xodus.stores.*;
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
import com.google.common.collect.*;
import io.dropwizard.util.Duration;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.Environments;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import javax.validation.Valid;
import javax.validation.Validator;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.bakdata.conquery.io.storage.StoreInfo.*;

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
    private transient Multimap<Environment, jetbrains.exodus.env.Store> openStoresInEnv = Multimaps.synchronizedSetMultimap(MultimapBuilder.hashKeys().hashSetValues().build());

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
        env.computeInTransaction(t -> env.getAllStoreNames(t));
        if (!exists) {
            closeEnvironment(env);
        }
        return exists;
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
        } else {
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

	@NonNull
    @JsonIgnore
    /**
     * Returns this.directory if the list is empty.
     */
    private File getStorageDir(Path pathName) {
        return getDirectory().resolve(pathName).toFile();
    }

   private Environment findEnvironment(Path pathName) {
        synchronized (activeEnvironments) {
            @NonNull File path = getStorageDir(pathName);
            return activeEnvironments.computeIfAbsent(path, (p) -> Environments.newInstance(p, getXodus().createConfig()));
        }
    }

    private void closeEnvironment(Environment env) {
        synchronized (activeEnvironments) {
            if (env == null) {
                return;
            }

            if(activeEnvironments.remove(activeEnvironments.inverse().get(env)) == null){
                return;
            }
            env.close();
        }
    }

    private void removeEnvironment(Environment env) {
        log.info("Deleting environment: {}", env.getLocation());
        try {
            FileUtil.deleteRecursive(Path.of(env.getLocation()));
        }catch (IOException e) {
            log.error("Cannot delete directory of removed environment: {}", env.getLocation(), log.isDebugEnabled()? e : null);
        }
    }

    public <KEY, VALUE> Store<KEY, VALUE> createStore(Environment environment, Validator validator, StoreInfo storeId) {
        synchronized (openStoresInEnv) {
            return new CachedStore<KEY, VALUE>(
                    new SerializingStore<KEY, VALUE>(
                            this,
                            new XodusStore(environment, storeId, openStoresInEnv.get(environment), this::closeEnvironment, this::removeEnvironment),
                            validator,
                            storeId
                    ));
        }
    }

    public <KEY, VALUE> Store<KEY, VALUE> createBigStore(Environment environment, Validator validator, StoreInfo storeId) {
        synchronized (openStoresInEnv) {

            return storeId.cached(
                    new BigStore<>(this, validator, environment, storeId, openStoresInEnv.get(environment), this::closeEnvironment, this::removeEnvironment)
            );
        }
    }

    public <KEY, VALUE> Store<KEY, VALUE> createBigWeakStore(Environment environment, Validator validator, StoreInfo storeId) {
        synchronized (openStoresInEnv) {

            return new WeakCachedStore<>(
                    new BigStore<>(this, validator, environment, storeId, openStoresInEnv.get(environment), this::closeEnvironment, this::removeEnvironment),
                    getWeakCacheDuration()
            );
        }
    }
}

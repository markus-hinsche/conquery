package com.bakdata.conquery.models.config;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.commands.ShardNode;
import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.storage.IdentifiableStore;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.io.storage.WorkerStorage;
import com.bakdata.conquery.io.storage.xodus.stores.SingletonStore;
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
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@CPSBase
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
public interface StoreFactory {

	default void init(ManagerNode managerNode) {
	}

	;

	default void init(ShardNode shardNode) {
	}


	Collection<NamespaceStorage> loadNamespaceStorages(ManagerNode managerNode, Path pathName);

	Collection<WorkerStorage> loadWorkerStorages(ShardNode shardNode, Path pathName);

	// NamespacedStorage (Important for serdes communication between manager and shards)
	SingletonStore<Dataset> createDatasetStore(Path pathName);

	IdentifiableStore<SecondaryIdDescription> createSecondaryIdDescriptionStore(CentralRegistry centralRegistry, Path pathName);

	IdentifiableStore<Table> createTableStore(CentralRegistry centralRegistry, Path pathName);

	IdentifiableStore<Dictionary> createDictionaryStore(CentralRegistry centralRegistry, Path pathName);

	IdentifiableStore<Concept<?>> createConceptStore(CentralRegistry centralRegistry, Path pathName);

	IdentifiableStore<Import> createImportStore(CentralRegistry centralRegistry, Path pathName);

	// WorkerStorage
	IdentifiableStore<CBlock> createCBlockStore(CentralRegistry centralRegistry, Path pathName);

	IdentifiableStore<Bucket> createBucketStore(CentralRegistry centralRegistry, Path pathName);

	SingletonStore<WorkerInformation> createWorkerInformationStore(Path pathName);

	// NamespaceStorage
	SingletonStore<PersistentIdMap> createIdMappingStore(Path pathName);

	SingletonStore<WorkerToBucketsMap> createWorkerToBucketsStore(Path pathName);

	SingletonStore<StructureNode[]> createStructureStore(Path pathName, SingletonNamespaceCollection centralRegistry);

	// MetaStorage
	IdentifiableStore<ManagedExecution<?>> createExecutionsStore(CentralRegistry centralRegistry, DatasetRegistry datasetRegistry, Path pathName);

	IdentifiableStore<FormConfig> createFormConfigStore(CentralRegistry centralRegistry, Path pathName);

	IdentifiableStore<User> createUserStore(CentralRegistry centralRegistry, Path pathName);

	IdentifiableStore<Role> createRoleStore(CentralRegistry centralRegistry, Path pathName);

	IdentifiableStore<Group> createGroupStore(CentralRegistry centralRegistry, Path pathName);
}

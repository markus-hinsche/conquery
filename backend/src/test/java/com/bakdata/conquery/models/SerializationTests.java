package com.bakdata.conquery.models;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Stream;

import javax.validation.Validator;

import com.bakdata.conquery.apiv1.IdLabel;
import com.bakdata.conquery.apiv1.MeProcessor;
import com.bakdata.conquery.apiv1.auth.PasswordCredential;
import com.bakdata.conquery.apiv1.forms.export_form.AbsoluteMode;
import com.bakdata.conquery.apiv1.forms.export_form.ExportForm;
import com.bakdata.conquery.apiv1.query.ArrayConceptQuery;
import com.bakdata.conquery.apiv1.query.ConceptQuery;
import com.bakdata.conquery.apiv1.query.concept.filter.CQTable;
import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.apiv1.query.concept.specific.CQOr;
import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.commands.ShardNode;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.jackson.MutableInjectableValues;
import com.bakdata.conquery.io.jackson.serializer.SerializationTestUtil;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.apitoken.ApiToken;
import com.bakdata.conquery.models.auth.apitoken.ApiTokenData;
import com.bakdata.conquery.models.auth.apitoken.Scopes;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.Role;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
import com.bakdata.conquery.models.auth.permissions.ExecutionPermission;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.ValidityDate;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeConnector;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.dictionary.MapDictionary;
import com.bakdata.conquery.models.error.ConqueryError;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.events.stores.primitive.BitSetStore;
import com.bakdata.conquery.models.events.stores.primitive.IntegerDateStore;
import com.bakdata.conquery.models.events.stores.primitive.ShortArrayStore;
import com.bakdata.conquery.models.events.stores.root.ColumnStore;
import com.bakdata.conquery.models.events.stores.specific.DateRangeTypeCompound;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.forms.configs.FormConfig;
import com.bakdata.conquery.models.forms.frontendconfiguration.FormConfigProcessor;
import com.bakdata.conquery.models.forms.managed.AbsoluteFormQuery;
import com.bakdata.conquery.models.forms.util.Alignment;
import com.bakdata.conquery.models.forms.util.Resolution;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.IdMapSerialisationTest;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.GroupId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.identifiable.mapping.EntityIdMap;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.MultilineEntityResult;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.util.NonPersistentStoreFactory;
import com.bakdata.conquery.util.dict.SuccinctTrie;
import com.bakdata.conquery.util.dict.SuccinctTrieTest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.powerlibraries.io.In;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import io.dropwizard.jersey.validation.Validators;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.util.CharArrayBuffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@Slf4j
public class SerializationTests {

	private Validator validator = Validators.newValidator();
	private ConqueryConfig config = new ConqueryConfig();
	private DatasetRegistry datasetRegistry;
	private MetaStorage metaStorage;

	private ObjectMapper managerInternalMapper;
	private ObjectMapper shardInternalMapper;
	private ObjectMapper apiMapper;


	@BeforeEach
	public void before() {
		datasetRegistry = new DatasetRegistry(0);
		metaStorage = new MetaStorage(new NonPersistentStoreFactory(), datasetRegistry);

		// Prepare manager node internal mapper
		final ManagerNode managerNode = mock(ManagerNode.class);
		when(managerNode.getConfig()).thenReturn(config);
		when(managerNode.getValidator()).thenReturn(validator);
		when(managerNode.getDatasetRegistry()).thenReturn(datasetRegistry);
		when(managerNode.getStorage()).thenReturn(metaStorage);

		when(managerNode.createInternalObjectMapper()).thenCallRealMethod();
		managerInternalMapper = managerNode.createInternalObjectMapper();

		metaStorage.openStores(managerInternalMapper);
		metaStorage.loadData();

		// Prepare shard node internal mapper
		final ShardNode shardNode = mock(ShardNode.class);
		when(shardNode.getConfig()).thenReturn(config);
		when(shardNode.getValidator()).thenReturn(validator);

		when(shardNode.createInternalObjectMapper()).thenCallRealMethod();
		shardInternalMapper = shardNode.createInternalObjectMapper();

		// Prepare api response mapper
		doCallRealMethod().when(managerNode).customizeApiObjectMapper(any(ObjectMapper.class));
		apiMapper = Jackson.copyMapperAndInjectables(Jackson.MAPPER);
		managerNode.customizeApiObjectMapper(apiMapper);
	}


	@Test
	public void dataset() throws IOException, JSONException {
		Dataset dataset = new Dataset();
		dataset.setName("dataset");

		SerializationTestUtil
				.forType(Dataset.class)
				.objectMappers(managerInternalMapper, shardInternalMapper)
				.test(dataset);
	}

	@Test
	public void passwordCredential() throws IOException, JSONException {
		PasswordCredential credential = new PasswordCredential(new String("testPassword").toCharArray());

		SerializationTestUtil
				.forType(PasswordCredential.class)
				.objectMappers(managerInternalMapper)
				.test(credential);
	}

	@Test
	public void role() throws IOException, JSONException {
		Role mandator = new Role("company", "company", metaStorage);

		SerializationTestUtil
				.forType(Role.class)
				.objectMappers(managerInternalMapper, apiMapper)
				.test(mandator);
	}

	/*
	 * Only way to add permission without a storage.
	 */
	@Test
	public void user() throws IOException, JSONException {
		User user = new User("user", "user", metaStorage);
		user.addPermission(DatasetPermission.onInstance(Ability.READ, new DatasetId("test")));
		user.addPermission(ExecutionPermission.onInstance(Ability.READ, new ManagedExecutionId(new DatasetId("dataset"), UUID.randomUUID())));
		Role role = new Role("company", "company", metaStorage);
		user.addRole(role);

		CentralRegistry registry = metaStorage.getCentralRegistry();
		registry.register(role);

		SerializationTestUtil
				.forType(User.class)
				.objectMappers(managerInternalMapper, apiMapper)
				.registry(registry)
				.test(user);
	}

	@Test
	public void group() throws IOException, JSONException {
		Group group = new Group("group", "group", metaStorage);
		group.addPermission(DatasetPermission.onInstance(Ability.READ, new DatasetId("test")));
		group.addPermission(ExecutionPermission.onInstance(Ability.READ, new ManagedExecutionId(new DatasetId("dataset"), UUID.randomUUID())));
		group.addRole(new Role("company", "company", metaStorage));

		Role role = new Role("company", "company", metaStorage);
		group.addRole(role);
		User user = new User("userName", "userLabel", metaStorage);
		group.addMember(user);

		CentralRegistry registry = metaStorage.getCentralRegistry();
		registry.register(role);
		registry.register(user);

		SerializationTestUtil
				.forType(Group.class)
				.objectMappers(managerInternalMapper, apiMapper)
				.test(group);
	}


	@Test
	public void bucketCompoundDateRange() throws JSONException, IOException {
		Dataset dataset = new Dataset();
		dataset.setName("datasetName");

		Table table = new Table();

		Column startCol = new Column();
		startCol.setName("startCol");
		startCol.setType(MajorTypeId.DATE);
		startCol.setTable(table);

		Column endCol = new Column();
		endCol.setLabel("endLabel");
		endCol.setName("endCol");
		endCol.setType(MajorTypeId.DATE);
		endCol.setTable(table);


		Column compoundCol = new Column();
		compoundCol.setName("compoundCol");
		compoundCol.setType(MajorTypeId.DATE_RANGE);
		compoundCol.setTable(table);

		table.setColumns(new Column[]{startCol, endCol, compoundCol});
		table.setDataset(dataset);
		table.setName("tableName");


		Import imp = new Import(table);
		imp.setName("importTest");


		DateRangeTypeCompound
				compoundStore =
				new DateRangeTypeCompound(startCol.getName(), endCol.getName(), new BitSetStore(BitSet.valueOf(new byte[]{0b1000}), new BitSet(), 4));
		//0b1000 is a binary representation of 8 so that the 4th is set to make sure that BitSet length is 4.

		ColumnStore startStore = new IntegerDateStore(new ShortArrayStore(new short[]{1, 2, 3, 4}, Short.MIN_VALUE));
		ColumnStore endStore = new IntegerDateStore(new ShortArrayStore(new short[]{5, 6, 7, 8}, Short.MIN_VALUE));

		Bucket bucket = new Bucket(0, 1, 4, new ColumnStore[]{startStore, endStore, compoundStore}, Collections.emptySet(), new int[0], new int[0], imp);

		compoundStore.setParent(bucket);


		CentralRegistry registry = metaStorage.getCentralRegistry();

		registry.register(dataset);
		registry.register(startCol);
		registry.register(endCol);
		registry.register(compoundCol);
		registry.register(table);
		registry.register(imp);
		registry.register(bucket);


		final Validator validator = Validators.newValidator();

		SerializationTestUtil
				.forType(Bucket.class)
				.objectMappers(managerInternalMapper, shardInternalMapper)
				.registry(registry)
				.injectables(new Injectable() {
					@Override
					public MutableInjectableValues inject(MutableInjectableValues values) {
						return values.add(Validator.class, validator);
					}
				})
				.test(bucket);

	}


	@Test
	public void table() throws JSONException, IOException {
		Dataset dataset = new Dataset();
		dataset.setName("datasetName");

		Table table = new Table();

		Column column = new Column();
		column.setLabel("colLabel");
		column.setName("colName");
		column.setType(MajorTypeId.STRING);
		column.setTable(table);


		table.setColumns(new Column[]{column});
		table.setDataset(dataset);
		table.setLabel("tableLabel");
		table.setName("tableName");


		CentralRegistry registry = metaStorage.getCentralRegistry();

		registry.register(dataset);
		registry.register(table);
		registry.register(column);

		SerializationTestUtil
				.forType(Table.class)
				.objectMappers(managerInternalMapper, shardInternalMapper, apiMapper)
				.registry(registry)
				.test(table);
	}

	@Test
	public void treeConcept() throws IOException, JSONException {
		Dataset dataset = new Dataset();
		dataset.setName("datasetName");

		TreeConcept concept = new TreeConcept();
		concept.setDataset(dataset);
		concept.setLabel("conceptLabel");
		concept.setName("conceptName");

		Table table = new Table();

		Column column = new Column();
		column.setLabel("colLabel");
		column.setName("colName");
		column.setType(MajorTypeId.STRING);
		column.setTable(table);

		Column dateColumn = new Column();
		dateColumn.setLabel("colLabel2");
		dateColumn.setName("colName2");
		dateColumn.setType(MajorTypeId.DATE);
		dateColumn.setTable(table);


		table.setColumns(new Column[]{column, dateColumn});
		table.setDataset(dataset);
		table.setLabel("tableLabel");
		table.setName("tableName");

		column.setTable(table);

		ConceptTreeConnector connector = new ConceptTreeConnector();
		connector.setConcept(concept);
		connector.setLabel("connLabel");
		connector.setName("connName");
		connector.setColumn(column);

		concept.setConnectors(List.of(connector));

		ValidityDate valDate = new ValidityDate();
		valDate.setColumn(dateColumn);
		valDate.setConnector(connector);
		valDate.setLabel("valLabel");
		valDate.setName("valName");
		connector.setValidityDates(List.of(valDate));

		CentralRegistry registry = metaStorage.getCentralRegistry();

		registry.register(dataset);
		registry.register(concept);
		registry.register(column);
		registry.register(dateColumn);
		registry.register(table);
		registry.register(connector);
		registry.register(valDate);

		concept.setValidator(validator);

		SerializationTestUtil
				.forType(TreeConcept.class)
				.objectMappers(managerInternalMapper, shardInternalMapper, apiMapper)
				.registry(registry)
				.test(concept);
	}

	@Test
	public void persistentIdMap() throws JSONException, IOException {
		SerializationTestUtil.forType(EntityIdMap.class)
							 .objectMappers(managerInternalMapper)
							 .test(IdMapSerialisationTest.createTestPersistentMap());

	}

	@Test
	public void formConfig() throws JSONException, IOException {
		final CentralRegistry registry = metaStorage.getCentralRegistry();

		final Dataset dataset = new Dataset("test-dataset");

		registry.register(dataset);

		ExportForm form = new ExportForm();
		AbsoluteMode mode = new AbsoluteMode();
		form.setTimeMode(mode);
		mode.setForm(form);
		mode.setFeatures(List.of(new CQConcept()));

		ObjectMapper mapper = FormConfigProcessor.getMAPPER();
		JsonNode values = mapper.valueToTree(form);
		FormConfig formConfig = new FormConfig(form.getClass().getAnnotation(CPSType.class).id(), values);
		formConfig.setDataset(dataset);

		SerializationTestUtil
				.forType(FormConfig.class)
				.objectMappers(managerInternalMapper, apiMapper)
				.registry(registry)
				.test(formConfig);
	}

	@Test
	public void managedQuery() throws JSONException, IOException {

		final CentralRegistry registry = metaStorage.getCentralRegistry();

		final Dataset dataset = new Dataset("test-dataset");

		final User user = new User("test-user", "test-user", metaStorage);

		registry.register(dataset);
		registry.register(user);

		metaStorage.updateUser(user);

		ManagedQuery execution = new ManagedQuery(null, user, dataset);
		execution.setTags(new String[]{"test-tag"});

		SerializationTestUtil.forType(ManagedExecution.class)
							 .objectMappers(managerInternalMapper, apiMapper)
							 .registry(registry)
							 .test(execution);
	}

	@Test
	public void cqConcept() throws JSONException, IOException {

		final Dataset dataset = new Dataset();
		dataset.setName("dataset");

		final TreeConcept concept = new TreeConcept();
		concept.setName("concept");
		concept.setDataset(dataset);

		final ConceptTreeConnector connector = new ConceptTreeConnector();
		connector.setConcept(concept);
		concept.setConnectors(List.of(connector));

		final CQConcept cqConcept = new CQConcept();
		cqConcept.setElements(List.of(concept));
		cqConcept.setLabel("Label");

		final CQTable cqTable = new CQTable();
		cqTable.setConnector(connector);
		cqTable.setFilters(List.of());
		cqTable.setConcept(cqConcept);

		cqConcept.setTables(List.of(cqTable));

		final CentralRegistry registry = metaStorage.getCentralRegistry();
		registry.register(dataset);
		registry.register(concept);
		registry.register(connector);

		SerializationTestUtil
				.forType(CQConcept.class)
				.objectMappers(managerInternalMapper, shardInternalMapper, apiMapper)
				.registry(registry)
				.test(cqConcept);
	}

	@Test
	public void executionCreationPlanError() throws JSONException, IOException {
		ConqueryError error = new ConqueryError.ExecutionCreationPlanError();

		SerializationTestUtil
				.forType(ConqueryError.class)
				.objectMappers(managerInternalMapper, shardInternalMapper, apiMapper)
				.test(error);
	}

	@Test
	public void executionCreationResolveError() throws JSONException, IOException {
		ConqueryError error = new ConqueryError.ExecutionCreationResolveError(new DatasetId("test"));

		SerializationTestUtil
				.forType(ConqueryError.class)
				.objectMappers(managerInternalMapper, shardInternalMapper, apiMapper)
				.test(error);
	}


	@Test
	public void executionQueryJobError() throws JSONException, IOException {
		log.info("Beware, this test will print an ERROR message.");
		ConqueryError error = new ConqueryError.ExecutionJobErrorWrapper(new Entity(5), new ConqueryError.UnknownError(null));

		SerializationTestUtil
				.forType(ConqueryError.class)
				.objectMappers(managerInternalMapper, shardInternalMapper, apiMapper)
				.test(error);
	}

	@Test
	public void meInformation() throws IOException, JSONException {
		User user = new User("name", "labe", metaStorage);

		MeProcessor.FEMeInformation info = MeProcessor.FEMeInformation.builder()
																	  .userName(user.getLabel())
																	  .hideLogoutButton(false)
																	  .groups(List.of(new IdLabel<>(new GroupId("test_group"), "test_group_label")))
																	  .datasetAbilities(Map.of(new DatasetId("testdataset"), new MeProcessor.FEDatasetAbility(true)))
																	  .build();

		SerializationTestUtil
				.forType(MeProcessor.FEMeInformation.class)
				.objectMappers(managerInternalMapper, apiMapper)
				.test(info);
	}

	@Test
	public void testFormQuery() throws IOException, JSONException {
		CQConcept concept = new CQConcept();
		final TreeConcept testConcept = new TreeConcept();
		Dataset dataset = new Dataset();
		dataset.setName("testDataset");
		testConcept.setDataset(dataset);
		testConcept.setName("concept");
		final ConceptTreeConnector connector = new ConceptTreeConnector();
		connector.setConcept(testConcept);
		connector.setName("connector1");

		testConcept.setConnectors(List.of(connector));

		concept.setElements(Collections.singletonList(testConcept));
		CQTable[] tables = {new CQTable()};
		connector.setTable(new Table());
		tables[0].setConnector(connector);
		tables[0].setConcept(concept);
		concept.setTables(Arrays.asList(tables));
		ConceptQuery subQuery = new ConceptQuery(concept);


		CQOr features = new CQOr();
		features.setChildren(Collections.singletonList(concept));


		AbsoluteFormQuery query = new AbsoluteFormQuery(
				subQuery,
				CDateRange.exactly(LocalDate.now()).toSimpleRange(),
				ArrayConceptQuery.createFromFeatures(Collections.singletonList(features)),
				List.of(
						ExportForm.ResolutionAndAlignment.of(Resolution.COMPLETE, Alignment.NO_ALIGN),
						ExportForm.ResolutionAndAlignment.of(Resolution.QUARTERS, Alignment.QUARTER)
				)
		);

		CentralRegistry centralRegistry = metaStorage.getCentralRegistry();
		centralRegistry.register(dataset);
		centralRegistry.register(testConcept);
		centralRegistry.register(connector);

		SerializationTestUtil
				.forType(AbsoluteFormQuery.class)
				.objectMappers(managerInternalMapper, shardInternalMapper, apiMapper)
				.registry(centralRegistry)
				.test(query);
	}

	@Test
	public void testApiTokenData() throws JSONException, IOException {
		final CharArrayBuffer buffer = new CharArrayBuffer(5);
		buffer.append("testtest");
		final ApiToken apiToken = new ApiToken(buffer);
		final ApiTokenData
				apiTokenData =
				new ApiTokenData(
						UUID.randomUUID(),
						apiToken.hashToken(),
						"tokenName",
						new UserId("tokenUser"),
						LocalDate.now(),
						LocalDate.now().plus(1, ChronoUnit.DAYS),
						EnumSet.of(Scopes.DATASET),
						metaStorage
				);


		SerializationTestUtil
				.forType(ApiTokenData.class)
				.objectMappers(managerInternalMapper, apiMapper)
				.test(apiTokenData);
	}

	@Test
	void testMapDictionary() throws IOException, JSONException {

		MapDictionary map = new MapDictionary(Dataset.PLACEHOLDER, "dictionary");

		map.add("a".getBytes());
		map.add("b".getBytes());
		map.add("c".getBytes());

		final CentralRegistry registry = metaStorage.getCentralRegistry();
		registry.register(Dataset.PLACEHOLDER);

		SerializationTestUtil
				.forType(MapDictionary.class)
				.objectMappers(managerInternalMapper, shardInternalMapper)
				.registry(registry)
				.test(map);
	}

	@Test
	public void serialize() throws IOException, JSONException {
		final CentralRegistry registry = metaStorage.getCentralRegistry();

		final Dataset dataset = new Dataset();
		dataset.setName("dataset");

		final TreeConcept concept = new TreeConcept();
		concept.setDataset(dataset);
		concept.setName("concept");

		final ConceptTreeConnector connector = new ConceptTreeConnector();
		connector.setName("connector");

		connector.setConcept(concept);
		concept.setConnectors(List.of(connector));

		final Table table = new Table();
		table.setName("table");
		table.setDataset(dataset);

		final Import imp = new Import(table);
		imp.setName("import");

		final Bucket bucket = new Bucket(0, 0, 0, new ColumnStore[0], Collections.emptySet(), new int[10], new int[10], imp);


		final CBlock cBlock = CBlock.createCBlock(connector, bucket, 10);

		registry.register(dataset)
				.register(table)
				.register(concept)
				.register(connector)
				.register(bucket)
				.register(imp);

		SerializationTestUtil.forType(CBlock.class)
							 .objectMappers(shardInternalMapper)
							 .registry(registry)
							 .test(cBlock);
	}

	@Test
	public void testSuccinctTrie()
			throws IOException, JSONException {

		final CentralRegistry registry = metaStorage.getCentralRegistry();
		registry.register(Dataset.PLACEHOLDER);

		SuccinctTrie dict = new SuccinctTrie(Dataset.PLACEHOLDER, "testDict");

		In.resource(SuccinctTrieTest.class, "SuccinctTrieTest.data").streamLines()
		  .forEach(value -> dict.put(value.getBytes()));

		dict.compress();
		SerializationTestUtil
				.forType(Dictionary.class)
				.objectMappers(managerInternalMapper, shardInternalMapper)
				.registry(registry)
				.test(dict);
	}


	@Test
	public void testBiMapSerialization() throws JSONException, IOException {
		BiMap<String, String> map = HashBiMap.create();
		map.put("a", "1");
		map.put("b", "2");
		SerializationTestUtil
				.forType(new TypeReference<BiMap<String, String>>() {
				})
				.objectMappers(apiMapper, managerInternalMapper)
				.test(map);
	}

	@Test
	public void testNonStrictNumbers() throws JSONException, IOException {
		SerializationTestUtil.forType(Double.class)
							 .objectMappers(apiMapper, managerInternalMapper).test(Double.NaN, null);
		SerializationTestUtil.forType(Double.class)
							 .objectMappers(apiMapper, managerInternalMapper).test(Double.NEGATIVE_INFINITY, null);
		SerializationTestUtil.forType(Double.class)
							 .objectMappers(apiMapper, managerInternalMapper).test(Double.POSITIVE_INFINITY, null);
		SerializationTestUtil.forType(Double.class)
							 .objectMappers(apiMapper, managerInternalMapper).test(Double.MAX_VALUE);
		SerializationTestUtil.forType(Double.class)
							 .objectMappers(apiMapper, managerInternalMapper).test(Double.MIN_VALUE);
		SerializationTestUtil
				.forType(EntityResult.class)
				.objectMappers(apiMapper, managerInternalMapper)
				.test(
						new MultilineEntityResult(4, List.of(
								new Object[]{0, 1, 2},
								new Object[]{Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY}
						)),
						new MultilineEntityResult(4, List.of(
								new Object[]{0, 1, 2},
								new Object[]{null, null, null}
						))
				);
	}

	public static Stream<Range<Integer>> rangeData() {
		final int SEED = 7;
		Random random = new Random(SEED);
		return Stream
				.generate(() -> {
					int first = random.nextInt();
					int second = random.nextInt();

					if (first < second) {
						return Range.of(first, second);
					}
					return Range.of(second, first);
				})
				.filter(Range::isOrdered)
				.flatMap(range -> Stream.of(
						range,
						Range.exactly(range.getMin()),
						Range.atMost(range.getMin()),
						Range.atLeast(range.getMin())
				))
				.filter(Range::isOrdered)
				.limit(100);
	}

	@ParameterizedTest
	@MethodSource("rangeData")
	public void test(Range<Integer> range) throws IOException, JSONException {
		SerializationTestUtil
				.forType(new TypeReference<Range<Integer>>() {
				})
				.objectMappers(apiMapper, managerInternalMapper, shardInternalMapper)
				.test(range);
	}

}

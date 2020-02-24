package com.bakdata.conquery.integration.json;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.forms.Form;
import com.bakdata.conquery.integration.common.LoadingUtil;
import com.bakdata.conquery.integration.common.RequiredData;
import com.bakdata.conquery.integration.common.ResourceFile;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.ConfigurationException;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.forms.managed.ManagedForm;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.identifiable.mapping.IdMappingConfig;
import com.bakdata.conquery.models.identifiable.mapping.IdMappingState;
import com.bakdata.conquery.models.query.ExecutionManager;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.QueryToCSVRenderer;
import com.bakdata.conquery.models.worker.Namespaces;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.github.powerlibraries.io.In;
import com.google.common.util.concurrent.ListeningExecutorService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;

@Slf4j
@Getter
@Setter
@CPSType(id = "FORM_TEST", base = ConqueryTestSpec.class)
public class FormTest extends ConqueryTestSpec {
	
	private static final PrintSettings PRINT_SETTINGS = new PrintSettings(true);

	private static final ListeningExecutorService POOL = ConqueryConfig
		.getInstance().getQueries().getExecutionPool()
		.createService("Form Executor %d");
	
	/*
	 * parse form as json first, because it may contain namespaced ids, that can only be resolved after
	 * concepts and tables have been imported.
	 */		
	@JsonProperty("form")
	@NotNull
	private JsonNode rawForm;
	
	@NotEmpty @Valid
	private Map<String, ResourceFile> expectedCsv;

	@Valid
	@NotNull
	private RequiredData content;
	@NotNull
	@JsonProperty("concepts")
	private ArrayNode rawConcepts;

	@JsonIgnore
	private Form form;

	@JsonIgnore
	private IdMappingConfig idMapping;

	@Override
	public void importRequiredData(StandaloneSupport support) throws Exception {

		LoadingUtil.importTables(support, content);
		support.waitUntilWorkDone();
		log.info("{} IMPORT TABLES", getLabel());

		importConcepts(support);
		support.waitUntilWorkDone();
		log.info("{} IMPORT CONCEPTS", getLabel());

		LoadingUtil.importTableContents(support, content);
		support.waitUntilWorkDone();
		log.info("{} IMPORT TABLE CONTENTS", getLabel());
		LoadingUtil.importPreviousQueries(support, content, support.getTestUser());

		support.waitUntilWorkDone();
		form = parseForm(support);
		MasterMetaStorage storage = support.getStandaloneCommand().getMaster().getStorage();
		form.init(storage.getNamespaces(), support.getTestUser());
		log.info("{} FORM INIT", getLabel());
		idMapping = support.getConfig().getIdMapping();
	}

	@Override
	public void executeTest(StandaloneSupport support) throws Exception {
		Namespaces namespaces = support.getNamespace().getNamespaces();
		MasterMetaStorage storage = support.getNamespace().getStorage().getMetaStorage();
		UserId userId = support.getTestUser().getId();
		DatasetId dataset = support.getNamespace().getDataset().getId();

		ManagedExecution<?> managedForm = ExecutionManager.runQuery(storage, namespaces, form, userId, dataset);

		managedForm.awaitDone(10, TimeUnit.MINUTES);
		if (managedForm.getState() != ExecutionState.DONE) {
			if (managedForm.getState() == ExecutionState.FAILED) {
				fail(getLabel() + " Query failed");
			}
			else {
				fail(getLabel() + " not finished after 10 min");
			}
		}

		log.info("{} QUERIES EXECUTED", getLabel());

		checkResults((ManagedForm) managedForm, support.getTestUser());
	}

	private void checkResults(ManagedForm managedForm, User user) throws IOException {
		QueryToCSVRenderer renderer = new QueryToCSVRenderer();
		Map<String, List<ManagedQuery>> managedMapping = managedForm.getSubQueries();
		IdMappingState mappingState = idMapping
			.initToExternal(user, managedForm);
		for (Map.Entry<String, List<ManagedQuery>> managed : managedMapping.entrySet()) {
			log.info("{} CSV TESTING: {}", getLabel(), managed.getKey());
			List<String> actual = renderer
				.toCSV(
					PRINT_SETTINGS,
					managed.getValue(),
					mappingState)
				.collect(Collectors.toList());
			
			assertThat(actual)
				.as("Checking result "+managed.getKey())
				.containsExactlyInAnyOrderElementsOf(
					In.stream(expectedCsv.get(managed.getKey()).stream())
					.withUTF8()
					.readLines()
				);
		}
	}

	private void importConcepts(StandaloneSupport support) throws JSONException, IOException, ConfigurationException {
		Dataset dataset = support.getDataset();

		List<Concept<?>> concepts = parseSubTreeList(
			support,
			rawConcepts,
			Concept.class,
			c -> c.setDataset(support.getDataset().getId())
		);

		for (Concept<?> concept : concepts) {
			support.getDatasetsProcessor().addConcept(dataset, concept);
		}
	}
	

	private Form parseForm(StandaloneSupport support) throws JSONException, IOException {
		return parseSubTree(support, rawForm, Form.class);
	}
}

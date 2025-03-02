package com.bakdata.conquery.integration.json;

import static com.bakdata.conquery.integration.common.LoadingUtil.*;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.query.Query;
import com.bakdata.conquery.integration.common.IntegrationUtils;
import com.bakdata.conquery.integration.common.RequiredData;
import com.bakdata.conquery.integration.common.ResourceFile;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.index.InternToExternMapper;
import com.bakdata.conquery.models.index.MapInternToExternMapper;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
@CPSType(id = "QUERY_TEST", base = ConqueryTestSpec.class)
public class QueryTest extends AbstractQueryEngineTest {

	private ResourceFile expectedCsv;

	@NotNull
	@JsonProperty("query")
	private JsonNode rawQuery;
	@Valid
	@NotNull
	private RequiredData content;
	@NotNull
	@JsonProperty("concepts")
	private ArrayNode rawConcepts;

	@NotNull
	private List<InternToExternMapper> internToExternMappings = List.of();

	@JsonIgnore
	private Query query;

	@Override
	public Query getQuery() {
		return query;
	}

	@Override
	public void importRequiredData(StandaloneSupport support) throws Exception {
		importSecondaryIds(support, content.getSecondaryIds());
		support.waitUntilWorkDone();

		importInternToExternMappers(support, internToExternMappings);
		support.waitUntilWorkDone();

		importTables(support, content.getTables());
		support.waitUntilWorkDone();

		importConcepts(support, rawConcepts);
		support.waitUntilWorkDone();

		importTableContents(support, content.getTables());
		support.waitUntilWorkDone();
		
		importIdMapping(support, content);
		support.waitUntilWorkDone();
		
		importPreviousQueries(support, content, support.getTestUser());
		support.waitUntilWorkDone();
		
		query = IntegrationUtils.parseQuery(support, rawQuery);
	}

}

package com.bakdata.conquery.resources.api;

import static com.bakdata.conquery.resources.ResourceConstants.DATASET;
import static com.bakdata.conquery.resources.ResourceConstants.QUERY;

import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import com.bakdata.conquery.apiv1.AdditionalMediaTypes;
import com.bakdata.conquery.io.result.excel.ResultExcelProcessor;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import io.dropwizard.auth.Auth;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Path("datasets/{" + DATASET + "}/result/")
@Api(tags = "api")
public class ResultExcelResource {
	@Inject
	private ResultExcelProcessor processor;
	
	@GET
	@Path("{" + QUERY + "}.xlsx")
	@Produces(AdditionalMediaTypes.ARROW_FILE)
	public Response get(
		@Auth User user,
		@PathParam(DATASET) DatasetId datasetId,
		@PathParam(QUERY) ManagedExecutionId executionId,
		@QueryParam("pretty") Optional<Boolean> pretty) {
		log.info("Result for {} download on dataset {} by user {} ({}).", executionId, datasetId, user.getId(), user.getName());
		return processor.getExcelResult(user, executionId, datasetId, pretty.orElse(false));
	}
}

package com.bakdata.conquery.resources.api;


import static com.bakdata.conquery.resources.ResourceConstants.DATASET;
import static com.bakdata.conquery.resources.ResourceConstants.QUERY;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.bakdata.conquery.apiv1.AdditionalMediaTypes;
import com.bakdata.conquery.apiv1.ExecutionStatus;
import com.bakdata.conquery.apiv1.FullExecutionStatus;
import com.bakdata.conquery.apiv1.MetaDataPatch;
import com.bakdata.conquery.apiv1.QueryProcessor;
import com.bakdata.conquery.apiv1.RequestAwareUriBuilder;
import com.bakdata.conquery.apiv1.query.ExternalUpload;
import com.bakdata.conquery.apiv1.query.ExternalUploadResult;
import com.bakdata.conquery.apiv1.query.QueryDescription;
import com.bakdata.conquery.models.auth.entities.UserLike;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.execution.ManagedExecution;
import io.dropwizard.auth.Auth;
import io.dropwizard.jersey.PATCH;
import lombok.extern.slf4j.Slf4j;

@Path("datasets/{" + DATASET + "}/queries")
@Consumes(AdditionalMediaTypes.JSON)
@Produces(AdditionalMediaTypes.JSON)
@Slf4j
public class QueryResource {

	@Inject
	private QueryProcessor processor;

	@Context
	protected HttpServletRequest servletRequest;

	@PathParam(DATASET)
	private Dataset dataset;

	@GET
	public List<ExecutionStatus> getAllQueries(@Auth UserLike user, @QueryParam("all-providers") Optional<Boolean> allProviders) {

		user.authorize(dataset, Ability.READ);

		return processor.getAllQueries(dataset, servletRequest, user, allProviders.orElse(false))
						.collect(Collectors.toList());
	}

	@POST
	public Response postQuery(@Auth UserLike user, @QueryParam("all-providers") Optional<Boolean> allProviders, @NotNull @Valid QueryDescription query) {

		user.authorize(dataset, Ability.READ);

		ManagedExecution<?> execution = processor.postQuery(dataset, query, user);

		return Response.ok(processor.getQueryFullStatus(execution, user, RequestAwareUriBuilder.fromRequest(servletRequest), allProviders.orElse(false)))
					   .status(Status.CREATED)
					   .build();
	}

	@GET
	@Path("{" + QUERY + "}")
	public FullExecutionStatus getStatus(@Auth UserLike user, @PathParam(QUERY) ManagedExecution<?> query, @QueryParam("all-providers") Optional<Boolean> allProviders)
			throws InterruptedException {

		user.authorize(dataset, Ability.READ);
		user.authorize(query, Ability.READ);

		query.awaitDone(1, TimeUnit.SECONDS);

		return processor.getQueryFullStatus(query, user, RequestAwareUriBuilder.fromRequest(servletRequest), allProviders.orElse(false));
	}

	@PATCH
	@Path("{" + QUERY + "}")
	public FullExecutionStatus patchQuery(@Auth UserLike user, @PathParam(QUERY) ManagedExecution<?> query, @QueryParam("all-providers") Optional<Boolean> allProviders, MetaDataPatch patch)
			throws JSONException {
		user.authorize(dataset, Ability.READ);
		user.authorize(query, Ability.READ);

		processor.patchQuery(user, query, patch);

		return processor.getQueryFullStatus(query, user, RequestAwareUriBuilder.fromRequest(servletRequest), allProviders.orElse(false));
	}

	@DELETE
	@Path("{" + QUERY + "}")
	public void deleteQuery(@Auth UserLike user, @PathParam(QUERY) ManagedExecution<?> query) {
		user.authorize(dataset, Ability.READ);
		user.authorize(query, Ability.DELETE);

		processor.deleteQuery(user, query);
	}

	@POST
	@Path("{" + QUERY + "}/reexecute")
	public FullExecutionStatus reexecute(@Auth UserLike user, @PathParam(QUERY) ManagedExecution<?> query, @QueryParam("all-providers") Optional<Boolean> allProviders) {
		user.authorize(dataset, Ability.READ);
		user.authorize(query, Ability.READ);

		processor.reexecute(user, query);
		return processor.getQueryFullStatus(query, user, RequestAwareUriBuilder.fromRequest(servletRequest), allProviders.orElse(false));
	}

	@POST
	@Path("{" + QUERY + "}/cancel")
	public void cancel(@Auth UserLike user, @PathParam(QUERY) ManagedExecution<?> query) {

		user.authorize(dataset, Ability.READ);
		user.authorize(query, Ability.CANCEL);

		processor.cancel(user, dataset, query);
	}

	@POST
	@Path("/upload")
	public ExternalUploadResult upload(@Auth UserLike user, @Valid ExternalUpload upload) {
		return processor.uploadEntities(user, dataset, upload);
	}
}

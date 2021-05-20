package com.bakdata.conquery.resources.admin.rest;

import static com.bakdata.conquery.resources.ResourceConstants.CONCEPT;
import static com.bakdata.conquery.resources.ResourceConstants.DATASET;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.resources.hierarchies.HAdmin;
import groovyjarjarpicocli.CommandLine;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiParam;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Produces({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})
@Consumes({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})

@Getter
@Setter(onParam_ = {@Schema(implementation = Id.class)})
@Path("datasets/{" + DATASET + "}/concepts/{" + CONCEPT + "}")
@ApiImplicitParams({@ApiImplicitParam(name = DATASET, dataTypeClass = Id.class)})
public class AdminConceptsResource extends HAdmin {

	@PathParam(DATASET)
	public Dataset dataset;

	@PathParam(CONCEPT)
	@Schema(implementation = Id.class)
	public Concept<?> concept;

	@DELETE
	public void removeConcept() {
		processor.deleteConcept(concept);
	}
}
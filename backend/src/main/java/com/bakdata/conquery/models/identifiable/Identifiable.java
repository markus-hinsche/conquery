package com.bakdata.conquery.models.identifiable;

import javax.validation.Valid;

import com.bakdata.conquery.models.identifiable.ids.Id;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.core.util.PrimitiveType;
import io.swagger.v3.oas.annotations.media.Schema;

public interface Identifiable<ID extends Id<? extends Identifiable<? extends ID>>> {

	@JsonIgnore @Valid
	ID getId();
}

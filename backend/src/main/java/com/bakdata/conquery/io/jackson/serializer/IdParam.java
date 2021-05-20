package com.bakdata.conquery.io.jackson.serializer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

import com.bakdata.conquery.models.identifiable.ids.Id;
import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

@Parameter(schema = @Schema(implementation = Id.class))
@JacksonAnnotationsInside
@Target({ElementType.PARAMETER, ElementType.FIELD})
public @interface IdParam {
}

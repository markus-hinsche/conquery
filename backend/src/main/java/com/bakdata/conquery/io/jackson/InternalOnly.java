package com.bakdata.conquery.io.jackson;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.commands.ShardNode;
import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.v3.oas.annotations.media.Schema;

/** Jackson view for fields only used in the {@link ManagerNode}-{@link ShardNode}-connection **/
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@Schema(hidden = true)
@JsonView(InternalOnly.class)
public @interface InternalOnly {}
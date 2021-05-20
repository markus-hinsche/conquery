package com.bakdata.conquery.io.jackson;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.commands.ShardNode;
import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;

/** Jackson view for fields only used in the {@link ManagerNode}-{@link ShardNode}-connection **/
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonView(InternalOnly.class)
@ApiImplicitParams(@ApiImplicitParam(access = "INTERNAL", type = "header"))
public @interface InternalOnly {}
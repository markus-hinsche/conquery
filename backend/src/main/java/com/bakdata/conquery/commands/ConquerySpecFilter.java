package com.bakdata.conquery.commands;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.swagger.v3.core.filter.AbstractSpecFilter;
import io.swagger.v3.oas.models.media.Schema;

public class ConquerySpecFilter extends AbstractSpecFilter {
	@Override
	public Optional<Schema> filterSchema(Schema schema, Map<String, List<String>> params, Map<String, String> cookies, Map<String, List<String>> headers) {
		if(schema.getName().equalsIgnoreCase("ColumnStore")){
			return Optional.empty();
		}

		if(schema.getName().equalsIgnoreCase("JsonNode")){
			return Optional.empty();
		}

		return super.filterSchema(schema, params, cookies, headers);
	}

	@Override
	public Optional<Schema> filterSchemaProperty(Schema property, Schema schema, String propName, Map<String, List<String>> params, Map<String, String> cookies, Map<String, List<String>> headers) {

		if (property.get$ref() == null) {
			return super.filterSchemaProperty(property, schema, propName, params, cookies, headers);
		}

		if(property.get$ref().endsWith("ColumnStore")){
			return Optional.empty();
		}

		if(property.get$ref().endsWith("JsonNode")){
			return Optional.empty();
		}

		return super.filterSchemaProperty(property, schema, propName, params, cookies, headers);
	}
}

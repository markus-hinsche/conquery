package com.bakdata.conquery.io.jackson.serializer;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

public class IntListDeserializer extends StdDeserializer<IntList> {
	protected IntListDeserializer() {
		super(IntList.class);
	}

	@Override
	public IntList deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		final int[] values = p.readValueAs(int[].class);
		return new IntArrayList(values);
	}


}

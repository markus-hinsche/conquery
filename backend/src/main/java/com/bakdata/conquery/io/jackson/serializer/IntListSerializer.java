package com.bakdata.conquery.io.jackson.serializer;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import it.unimi.dsi.fastutil.ints.IntList;

public class IntListSerializer extends StdSerializer<IntList> {
	protected IntListSerializer() {
		super(IntList.class);
	}

	@Override
	public void serialize(IntList value, JsonGenerator gen, SerializerProvider provider) throws IOException {
		gen.writeArray(value.toIntArray(), 0, value.size());
	}
}

package com.bakdata.conquery.models.query;

import java.util.Arrays;
import java.util.List;

import com.bakdata.conquery.io.jackson.serializer.IntListDeserializer;
import com.bakdata.conquery.io.jackson.serializer.IntListSerializer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Preconditions;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor(onConstructor_ = {@JsonCreator})
public class EntityData {
	@JsonDeserialize(using = IntListDeserializer.class)
	@JsonSerialize(using = IntListSerializer.class)
	private final IntList entities;

	private final List[] data;

	public EntityData(List[] data) {
		entities = new IntArrayList();
		this.data = data;
	}

	public void addLine(int entity, Object[] row) {
		Preconditions.checkArgument(row.length == data.length, "Row is not same width as data.");

		entities.add(entity);

		for (int col = 0; col < row.length; col++) {
			data[col].add(row[col]);
		}
	}

	public void addAll(EntityData other) {
		Preconditions.checkArgument(other.data.length == data.length, "Row is not same width as data.");

		entities.addAll(other.entities);

		for (int col = 0; col < data.length; col++) {
			data[col].addAll(other.data[col]);
		}
	}

	public int size() {
		return entities.size();
	}

	public int entities() {
		return (int) entities.intStream().distinct().count();
	}

	public void clear() {
		entities.clear();
		Arrays.stream(data).forEach(List::clear);
	}

	public int getEntity(int line){
		return entities.getInt(line);
	}

	public Object[] getLine(int line) {
		final Object[] values = new Object[data.length];
		for (int col = 0; col < data.length; col++) {
			values[col] = data[col].get(line);
		}

		return values;
	}

}

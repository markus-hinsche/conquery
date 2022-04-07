package com.bakdata.conquery.models.dictionary;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.identifiable.NamedImpl;
import com.bakdata.conquery.models.identifiable.ids.NamespacedIdentifiable;
import com.bakdata.conquery.models.identifiable.ids.specific.DictionaryId;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;

@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
@CPSBase
public abstract class Dictionary extends NamedImpl<DictionaryId> implements NamespacedIdentifiable<DictionaryId>, Iterable<DictionaryEntry> {
	private final Charset CHARSET = StandardCharsets.UTF_8;

	//TODO detangle Dictionary from Storages so that we can move Cache here

	@Getter
	@Setter
	@NsIdRef
	private Dataset dataset;

	public Dictionary(Dataset dataset, String name) {
		this.setName(name);
		this.dataset = dataset;
	}

	public abstract Dictionary copyEmpty();

	public void compress() {
	}

	@Override
	public DictionaryId createId() {
		return new DictionaryId(dataset.getId(), getName());
	}

	public abstract int add(String bytes);

	public abstract int put(String bytes);

	public abstract int getId(String bytes);

	public abstract String getElement(int id);

	public abstract int size();

	protected String decode(byte[] elements) {
		return new String(elements, CHARSET);
	}

	protected byte[] encode(String value) {
		return value.getBytes(CHARSET);
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "[size=" + size() + "]";
	}

	public abstract long estimateMemoryConsumption();

}

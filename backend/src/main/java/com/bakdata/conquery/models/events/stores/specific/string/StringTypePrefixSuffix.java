package com.bakdata.conquery.models.events.stores.specific.string;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import javax.annotation.Nonnull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.xodus.NamespacedStorage;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.events.stores.ColumnStore;
import com.bakdata.conquery.models.identifiable.ids.specific.DictionaryId;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

/**
 * Strings with common, but stripped prefix/suffix.
 */
@Getter
@Setter
@CPSType(base = ColumnStore.class, id = "STRING_PREFIX")
@ToString(of = {"prefix", "suffix", "subType"})
public class StringTypePrefixSuffix extends StringType {

	@Nonnull
	protected StringType store;

	@NonNull
	private String prefix;

	@NonNull
	private String suffix;

	@JsonCreator
	public StringTypePrefixSuffix(StringType store, String prefix, String suffix) {
		super();
		this.store = store;
		this.prefix = prefix;
		this.suffix = suffix;
	}

	@Override
	public String getElement(int value) {
		return prefix + store.getElement(value) + suffix;
	}

	@Override
	public String createScriptValue(Integer value) {
		return prefix + store.createScriptValue(value);
	}

	@Override
	public int getId(String value) {
		if (value.startsWith(prefix)) {
			return store.getId(value.substring(prefix.length()));
		}
		return -1;
	}

	@Override
	public void setIndexStore(ColumnStore<Long> indexStore) {
		store.setIndexStore(indexStore);
	}

	@Override
	public Iterator<String> values() {
		Iterator<String> subIt = store.values();
		return new Iterator<String>() {
			@Override
			public boolean hasNext() {
				return subIt.hasNext();
			}

			@Override
			public String next() {
				return prefix + subIt.next();
			}
		};
	}


	@Override
	public int getLines() {
		return store.getLines();
	}

	@Override
	public StringTypePrefixSuffix select(int[] starts, int[] length) {
		return new StringTypePrefixSuffix(store.select(starts, length), getPrefix(), getSuffix());
	}

	@Override
	public void loadDictionaries(NamespacedStorage storage) {
		store.loadDictionaries(storage);
	}

	@Override
	public int size() {
		return store.size();
	}

	@Override
	public long estimateEventBits() {
		return store.estimateEventBits();
	}

	@Override
	public long estimateMemoryConsumptionBytes() {
		return (long) prefix.getBytes(StandardCharsets.UTF_8).length * Byte.SIZE +
			   (long) suffix.getBytes(StandardCharsets.UTF_8).length * Byte.SIZE +
			   store.estimateMemoryConsumptionBytes();
	}

	@Override
	public Dictionary getUnderlyingDictionary() {
		return store.getUnderlyingDictionary();
	}

	@Override
	public void setUnderlyingDictionary(DictionaryId newDict) {
		store.setUnderlyingDictionary(newDict);
	}

	@Override
	public Integer get(int event) {
		return getString(event);
	}

	@Override
	public int getString(int event) {
		return store.getString(event);
	}

	@Override
	public void set(int event, Integer value) {
		store.set(event, value);
	}

	@Override
	public boolean has(int event) {
		return store.has(event);
	}
}

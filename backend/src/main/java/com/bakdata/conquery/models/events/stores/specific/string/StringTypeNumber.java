package com.bakdata.conquery.models.events.stores.specific.string;

import java.util.Iterator;
import java.util.Map;
import java.util.stream.IntStream;

import javax.annotation.Nonnull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.events.stores.ColumnStore;
import com.bakdata.conquery.models.identifiable.ids.specific.DictionaryId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * The strings are only numbers and can therefore be used directly.
 */
@Getter
@Setter
@CPSType(base = ColumnStore.class, id = "STRING_NUMBER")
@ToString(of = "delegate")
public class StringTypeNumber extends StringType {

	@Nonnull
	protected ColumnStore<Long> store;
	//used as a compact intset
	private Range<Integer> range;

	// Only used for setting values in Preprocessing.
	@JsonIgnore
	private transient Map<Integer, String> dictionary;

	@JsonCreator
	public StringTypeNumber(Range<Integer> range, ColumnStore<Long> numberType) {
		super();
		this.range = range;
		this.store = numberType;
	}

	public StringTypeNumber(Range<Integer> range, ColumnStore<Long> numberType, Map<Integer, String> dictionary) {
		this(range, numberType);
		this.dictionary = dictionary;
	}

	@Override
	public long estimateEventBits() {
		return store.estimateEventBits();
	}

	@Override
	public Iterator<String> values() {
		return IntStream
					   .rangeClosed(
							   range.getMin(),
							   range.getMax()
					   )
					   .mapToObj(Integer::toString)
					   .iterator();
	}

	@Override
	public Object createPrintValue(Integer value) {
		return value;
	}

	@Override
	public Object createScriptValue(Integer value) {
		return value.toString();
	}

	@Override
	public String getElement(int id) {
		return Integer.toString(id);
	}

	@Override
	public int size() {
		return range.getMax() - range.getMin() + 1;
	}

	@Override
	public int getId(String value) {
		try {
			return Integer.parseInt(value);
		}
		catch (NumberFormatException e) {
			return -1;
		}
	}

	@Override
	public Dictionary getUnderlyingDictionary() {
		return null;
	}

	@Override
	public void setUnderlyingDictionary(DictionaryId newDict) {

	}

	@Override
	public void setIndexStore(ColumnStore<Long> indexStore) {	}

	@Override
	public int getLines() {
		return store.getLines();
	}

	@Override
	public StringTypeNumber select(int[] starts, int[] length) {
		return new StringTypeNumber(range, store.select(starts, length));
	}

	@Override
	public Integer get(int event) {
		return getString(event);
	}

	@Override
	public int getString(int event) {
		return (int) getStore().getInteger(event);
	}

	@Override
	public void set(int event, Integer value) {
		if (value == null) {
			getStore().set(event, null);
		}
		else {
			getStore().set(event, Long.valueOf(dictionary.get(value)));
		}
	}

	@Override
	public boolean has(int event) {
		return getStore().has(event);
	}
}

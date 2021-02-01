package com.bakdata.conquery.models.events.stores.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.events.stores.ColumnStore;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.Setter;

@CPSType(base = ColumnStore.class, id = "MONEY_VARINT")
@Getter
@Setter
public class MoneyTypeInteger extends ColumnStore<Long> {

	protected ColumnStore<Long> store;

	@JsonCreator
	public MoneyTypeInteger(ColumnStore<Long> store) {
		this.store = store;
	}

	@Override
	public Object createPrintValue(Long value) {
		return createScriptValue(value);
	}

	@Override
	public Long createScriptValue(Long value) {
		return (long) store.createScriptValue(value);
	}

	@Override
	public MoneyTypeInteger doSelect(int[] starts, int[] length) {
		return new MoneyTypeInteger(store.select(starts, length));
	}

	@Override
	public long getMoney(int event) {
		return store.getInteger(event);
	}

	@Override
	public Long get(int event) {
		return getMoney(event);
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "[numberType=" + store + "]";
	}

	@Override
	public long estimateEventBits() {
		return store.estimateEventBits();
	}

	@Override
	public void set(int event, Long value) {
		if (value == null) {
			store.set(event, null);
		}
		else {
			store.set(event, value.longValue());
		}
	}

	@Override
	public final boolean has(int event) {
		return store.has(event);
	}

	@Override
	public int getLines() {
		return store.getLines();
	}
}

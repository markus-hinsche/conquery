package com.bakdata.conquery.models.events.stores.specific;

import java.math.BigDecimal;
import java.math.BigInteger;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.events.stores.ColumnStore;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

/**
 * Store Decimals as longs with a fixed scale.
 */
@CPSType(base = ColumnStore.class, id = "DECIMAL_SCALED")
@Getter
public class DecimalTypeScaled extends ColumnStore<BigDecimal> {

	private final int scale;
	private final ColumnStore<Long> store;

	@JsonCreator
	public DecimalTypeScaled(int scale, ColumnStore<Long> subType) {
		this.scale = scale;
		this.store = subType;
	}

		@Override
	public BigDecimal createScriptValue(BigDecimal value) {
		return null;
	}

	@Override
	public String toString() {
		return "DecimalTypeScaled[numberType=" + store + "]";
	}

	@Override
	public long estimateEventBits() {
		return store.estimateEventBits();
	}

	@Override
	public DecimalTypeScaled select(int[] starts, int[] length) {
		return new DecimalTypeScaled(scale, store.select(starts, length));
	}

	@Override
	public void set(int event, BigDecimal value) {
		if (value == null) {
			store.set(event, null);
		}
		else {
			store.set(event, unscale(scale, value).longValue());
		}
	}

	public static BigInteger unscale(int scale, BigDecimal value) {
		return value.movePointRight(scale).toBigIntegerExact();
	}

	@Override
	public BigDecimal get(int event) {
		return getDecimal(event);
	}

	@Override
	public BigDecimal getDecimal(int event) {
		return scale(scale, store.getInteger(event));
	}

	public static BigDecimal scale(int scale, long value) {
		return BigDecimal.valueOf(value, scale);
	}

	@Override
	public boolean has(int event) {
		return store.has(event);
	}

	@Override
	public int getLines() {
		return store.getLines();
	}
}
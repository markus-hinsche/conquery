package com.bakdata.conquery.models.types.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.types.CType;
import lombok.Getter;
import lombok.ToString;

@CPSType(base=CType.class, id="VAR_INT_INT16")
@Getter
@ToString
public class VarIntTypeShort extends VarIntType {

	private final int maxValue;
	private final int minValue;
	
	public VarIntTypeShort(int minValue, int maxValue) {
		super(short.class);
		this.minValue = minValue;
		this.maxValue = maxValue;
	}

	@Override
	public int toInt(Number value) {
		return (int) (value.intValue() + minValue - (int) Short.MIN_VALUE);
	}

	public short toShort(int value) {
		return (short) ((int) value - (int)minValue + (int)Short.MIN_VALUE);
	}

	@Override
	public long estimateMemoryBitWidth() {
		return Short.SIZE;
	}
}

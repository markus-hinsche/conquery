package com.bakdata.conquery.models.types.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.types.CType;
import lombok.Getter;

@CPSType(base=CType.class, id="VAR_INT_INT16")
@Getter
public class VarIntTypeShort extends VarIntType {

	private final long maxValue;
	private final long minValue;
	
	public VarIntTypeShort(long minValue, long maxValue) {
		super(short.class);
		this.minValue = minValue;
		this.maxValue = maxValue;
	}

	@Override
	public int toInt(Number value) {
		return (int) (value.intValue() + minValue);
	}
	
	@Override
	public long estimateMemoryBitWidth() {
		return Short.SIZE;
	}
}

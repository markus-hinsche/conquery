package com.bakdata.conquery.models.types.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.types.CType;
import lombok.Getter;
import lombok.ToString;

@CPSType(base=CType.class, id="VAR_INT_INT32")
@Getter
@ToString
public class VarIntTypeInt extends VarIntType {

	private final long maxValue;
	private final long minValue;
	
	public VarIntTypeInt(long minValue, long maxValue) {
		super(int.class);
		this.minValue = minValue;
		this.maxValue = maxValue;
	}

	@Override
	public int toInt(Number value) {
		return (int) (value.intValue() + minValue);
	}
	
	@Override
	public long estimateMemoryBitWidth() {
		return Integer.SIZE;
	}
}

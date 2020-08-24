package com.bakdata.conquery.models.types.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.types.CType;
import lombok.Getter;
import lombok.ToString;

@CPSType(base=CType.class, id="VAR_INT_INT32")
@Getter
@ToString
public class VarIntTypeInt extends VarIntType {

	private final int maxValue;
	private final int minValue;
	
	public VarIntTypeInt(int minValue, int maxValue) {
		super(int.class);
		this.minValue = minValue;
		this.maxValue = maxValue;
	}

	@Override
	public int toInt(Number value) {
		return value.intValue();
	}
	
	@Override
	public long estimateMemoryBitWidth() {
		return Integer.SIZE;
	}
}

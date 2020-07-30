package com.bakdata.conquery.models.types.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.types.CType;
import lombok.Getter;
import lombok.ToString;

@CPSType(base=CType.class, id="VAR_INT_BYTE")
@Getter
@ToString
public class VarIntTypeByte extends VarIntType {

	private final long maxValue;
	private final long minValue;
	
	public VarIntTypeByte(long minValue, long maxValue) {
		super(byte.class);
		this.minValue = minValue;
		this.maxValue = maxValue;
	}

	@Override
	public int toInt(Number value) {
		return (value.intValue() + (int) minValue);
	}

		@Override
	public long estimateMemoryBitWidth() {
		return Byte.SIZE;
	}
}

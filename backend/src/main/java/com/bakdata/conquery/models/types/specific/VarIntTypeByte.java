package com.bakdata.conquery.models.types.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.types.CType;
import lombok.Getter;
import lombok.ToString;

@CPSType(base=CType.class, id="VAR_INT_BYTE")
@Getter
@ToString
public class VarIntTypeByte extends VarIntType {

	private final int maxValue;
	private final int minValue;
	
	public VarIntTypeByte(int minValue, int maxValue) {
		super(byte.class);
		this.minValue = minValue;
		this.maxValue = maxValue;
	}

	@Override
	public int toInt(Number value) {
		return ((int) value.byteValue() + (int) minValue - (int) Byte.MIN_VALUE);
	}

	public byte toByte(int value) {
		final byte b = (byte) ((int) value - (int) minValue + (int) Byte.MIN_VALUE);
		return b;
	}

		@Override
	public long estimateMemoryBitWidth() {
		return Byte.SIZE;
	}
}

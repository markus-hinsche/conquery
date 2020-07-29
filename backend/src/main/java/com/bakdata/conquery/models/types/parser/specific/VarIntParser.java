package com.bakdata.conquery.models.types.parser.specific;

import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.types.parser.Decision;
import com.bakdata.conquery.models.types.parser.Parser;
import com.bakdata.conquery.models.types.specific.VarIntType;
import com.bakdata.conquery.models.types.specific.VarIntTypeByte;
import com.bakdata.conquery.models.types.specific.VarIntTypeInt;
import com.bakdata.conquery.models.types.specific.VarIntTypeShort;
import lombok.ToString;

@ToString(callSuper = true)
public class VarIntParser extends Parser<Integer> {

	private long maxValue = Integer.MIN_VALUE;
	private long minValue = Integer.MAX_VALUE;

	@Override
	public void registerValue(Integer v) {
		minValue = Math.min(minValue, v);
		maxValue = Math.max(maxValue, v);
	}
	
	@Override
	protected Integer parseValue(String value) throws ParsingException {
		throw new UnsupportedOperationException();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Decision<Integer, Number, VarIntType> findBestType() {
		return (Decision<Integer, Number, VarIntType>) super.findBestType();
	}
	
	@Override
	public Decision<Integer, Number, VarIntType> decideType() {
		// TODO: 28.07.2020 FK: VarIntBoolean for two fields
		// TODO: 28.07.2020 FK: Restructure this, so that this can be folded into IntParser where all unwrap to longs instead

		if (maxValue - minValue < ((int) Byte.MAX_VALUE - (int) Byte.MIN_VALUE)) {
			return new Decision<>(
					value -> (byte) (value - minValue),
					new VarIntTypeByte(minValue, maxValue)
			);
		}

		if (maxValue - minValue < ((long) Short.MAX_VALUE - (long) Short.MIN_VALUE)) {
			return new Decision<>(
					value -> (short) (value - minValue),
					new VarIntTypeShort(minValue, maxValue)
			);
		}



		return new Decision<>(value -> (int) (value - minValue), new VarIntTypeInt(minValue, maxValue));
	}
}

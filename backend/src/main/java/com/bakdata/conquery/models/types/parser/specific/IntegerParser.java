package com.bakdata.conquery.models.types.parser.specific;

import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.parser.Decision;
import com.bakdata.conquery.models.types.parser.NoopTransformer;
import com.bakdata.conquery.models.types.parser.Parser;
import com.bakdata.conquery.models.types.specific.IntegerTypeLong;
import com.bakdata.conquery.models.types.specific.IntegerTypeVarInt;
import com.bakdata.conquery.models.types.specific.VarIntType;
import com.bakdata.conquery.util.NumberParsing;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@ToString(callSuper = true) @Slf4j
public class IntegerParser extends Parser<Long> {

	private long maxValue = Long.MIN_VALUE;
	private long minValue = Long.MAX_VALUE;

	@Override
	protected Long parseValue(String value) throws ParsingException {
		return NumberParsing.parseLong(value);
	}
	
	@Override
	protected void registerValue(Long v) {
		if(v > maxValue) {
			maxValue = v;
		}
		if(v < minValue) {
			minValue = v;
		}
	}
	
	@Override
	public Decision<Long, Number, ? extends CType<Long, ? extends Number>> findBestType() {
		return (Decision<Long, Number, ? extends CType<Long, ? extends Number>>) super.findBestType();
	}

	@Override
	protected Decision<Long, ?, ? extends CType<Long, ?>> decideType() {

		try {
			if (Math.subtractExact(maxValue + 1, minValue) < Math.subtractExact((long) Integer.MAX_VALUE, (long) Integer.MIN_VALUE)) {
				VarIntParser subParser = new VarIntParser();

				subParser.setMaxValue((int) maxValue);
				subParser.setMinValue((int) minValue);

				subParser.setLines(getLines());
				subParser.setNullLines(getNullLines());

				Decision<Integer, Number, VarIntType> subDecision = subParser.findBestType();

				return new Decision<>(
						value -> subDecision.getTransformer().transform(value.intValue()),
						new IntegerTypeVarInt(subDecision.getType())
				);
			}
		}
		catch (ArithmeticException exc) {
			// This means the numbers were out of integer range.
			log.trace("min = {}, max = {}", minValue, maxValue, exc);
		}

		return new Decision<>(
				new NoopTransformer<>(),
				new IntegerTypeLong(minValue, maxValue)
		);
	}

}

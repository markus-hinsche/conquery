package com.bakdata.conquery.models.types.parser.specific;

import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.parser.Decision;
import com.bakdata.conquery.models.types.parser.NoopTransformer;
import com.bakdata.conquery.models.types.parser.Parser;
import com.bakdata.conquery.models.types.specific.RealTypeDouble;
import com.bakdata.conquery.models.types.specific.RealTypeFloat;
import com.bakdata.conquery.util.NumberParsing;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString(callSuper = true)
public class RealParser extends Parser<Double> {

	private float floatULP = Float.NEGATIVE_INFINITY;
	private double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;


	@Override
	protected Double parseValue(String value) throws ParsingException {
		return NumberParsing.parseDouble(value);
	}

	@Override
	protected void registerValue(Double v) {
		if(v.isInfinite() || v.isNaN()) {
			return;
		}

		floatULP = Math.max(floatULP, Math.ulp(v.floatValue()));
		min = Math.min(v, min);
		max = Math.max(v, max);
	}

	@Override
	protected Decision<Double, ?, ? extends CType<Double, ?>> decideType() {
		// TODO: 27.07.2020 FK: Make this configurable
		log.debug("{}", this);

		if(floatULP < 1e-2){
			return new Decision<>(
					Double::floatValue,
					new RealTypeFloat()
			);
		}

		return new Decision<>(new NoopTransformer<Double>(), new RealTypeDouble());
	}
}

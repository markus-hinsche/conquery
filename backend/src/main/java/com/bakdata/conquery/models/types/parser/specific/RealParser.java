package com.bakdata.conquery.models.types.parser.specific;

import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.parser.Decision;
import com.bakdata.conquery.models.types.parser.NoopTransformer;
import com.bakdata.conquery.models.types.parser.Parser;
import com.bakdata.conquery.models.types.specific.RealTypeDouble;
import com.bakdata.conquery.models.types.specific.RealTypeFloat;
import com.bakdata.conquery.util.NumberParsing;
import com.google.common.math.StatsAccumulator;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class RealParser extends Parser<Double> {

	private double precision = 1e-3;

	private final StatsAccumulator ulpStats = new StatsAccumulator();
	private final StatsAccumulator valueStats = new StatsAccumulator();



	@Override
	protected Double parseValue(String value) throws ParsingException {
		return NumberParsing.parseDouble(value);
	}

	@Override
	protected void registerValue(Double v) {
		if(v.isInfinite() || v.isNaN()) {
			return;
		}

		ulpStats.add(Math.ulp(v.floatValue()));
		valueStats.add(v);
	}

	@Override
	protected Decision<Double, ?, ? extends CType<Double, ?>> decideType() {
		// TODO: 27.07.2020 FK: Make this configurable
		log.debug("Values = {} ULP = {}", valueStats, ulpStats);

		if(ulpStats.mean() < 1e-2){
			return new Decision<>(
					Double::floatValue,
					new RealTypeFloat()
			);
		}

		return new Decision<>(new NoopTransformer<Double>(), new RealTypeDouble());
	}
}

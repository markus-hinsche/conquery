package com.bakdata.conquery.io.result;

import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.forms.util.DateContext;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.MultilineEntityResult;
import com.bakdata.conquery.models.query.results.SinglelineEntityResult;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@UtilityClass
public class ResultTestUtil {


	@NotNull
	public static List<ResultType> getResultTypes() {
		return List.of(
				ResultType.BooleanT.INSTANCE,
				ResultType.IntegerT.INSTANCE,
				ResultType.NumericT.INSTANCE,
				ResultType.CategoricalT.INSTANCE,
				ResultType.ResolutionT.INSTANCE,
				ResultType.DateT.INSTANCE,
				ResultType.DateRangeT.INSTANCE,
				ResultType.StringT.INSTANCE,
				ResultType.MoneyT.INSTANCE,
				new ResultType.ListT(ResultType.BooleanT.INSTANCE)
		);
	}

	@NotNull
	public static List<EntityResult> getTestEntityResults() {
		List<EntityResult> results = List.of(
				new SinglelineEntityResult(1, new Object[]{Boolean.TRUE, 2345634, 123423.34, "CAT1", DateContext.Resolution.DAYS.toString(), 5646, List.of(345, 534), "test_string", 4521, List.of(true, false)}),
				new SinglelineEntityResult(2, new Object[]{Boolean.FALSE, null, null, null, null, null, null, null, null, List.of()}),
				new SinglelineEntityResult(2, new Object[]{Boolean.TRUE, null, null, null, null, null, null, null, null, List.of(false, false)}),
				new MultilineEntityResult(3, List.of(
						new Object[]{Boolean.FALSE, null, null, null, null, null, null, null, null, List.of(false)},
						new Object[]{Boolean.TRUE, null, null, null, null, null, null, null, null, null},
						new Object[]{Boolean.TRUE, null, null, null, null, null, null, null, 4, List.of(true, false, true, false)}
				)));
		return results;
	}
}

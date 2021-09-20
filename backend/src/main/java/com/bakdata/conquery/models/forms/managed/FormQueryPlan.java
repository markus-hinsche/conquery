package com.bakdata.conquery.models.forms.managed;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.forms.util.DateContext;
import com.bakdata.conquery.models.forms.util.ResultModifier;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.ArrayConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.results.MultilineEntityResult;
import com.bakdata.conquery.models.query.results.SinglelineEntityResult;
import com.bakdata.conquery.util.QueryUtils;
import lombok.Getter;

@Getter
public class FormQueryPlan implements QueryPlan<MultilineEntityResult> {

	private final List<DateContext> dateContexts;
	private final ArrayConceptQueryPlan features;

	private final int constantCount;

	public FormQueryPlan(List<DateContext> dateContexts, ArrayConceptQueryPlan features) {
		this.dateContexts = dateContexts;
		this.features = features;

		if (dateContexts.size() <= 0) {
			// There is nothing to do for this FormQueryPlan but we will return an empty result when its executed
			constantCount = 3;
			return;
		}

		// Either all date contexts have an relative event date or none has one
		boolean withRelativeEventDate = dateContexts.get(0).getEventDate() != null;

		for (DateContext dateContext : dateContexts) {
			if ((dateContext.getEventDate() == null) == withRelativeEventDate) {
				throw new IllegalStateException("QueryPlan has absolute AND relative date contexts. Only one kind is allowed.");
			}
		}
		constantCount = withRelativeEventDate ? 4 : 3; // resolution indicator, index value, (event date,) date range
	}

	@Override
	public Optional<MultilineEntityResult> execute(QueryExecutionContext ctx, Entity entity) {

		if (!isOfInterest(entity)) {
			// If the entity is not covered by the query generate a basic result line with constants but without features
			return Optional.of(createResultForNotContained(entity, null));
		}

		List<Object[]> resultValues = new ArrayList<>(dateContexts.size());

		for (DateContext dateContext : dateContexts) {
			CDateSet dateRestriction = CDateSet.create(ctx.getDateRestriction());
			dateRestriction.retainAll(dateContext.getDateRange());

			// Reference the dates per sub-query, don't accumulate dates of all sub-queries
			QueryExecutionContext innerContext = QueryUtils.determineDateAggregatorForContext(ctx, features::getValidityDateAggregator)
														   .withDateRestriction(dateRestriction);

			features.init(ctx, entity);

			Optional<SinglelineEntityResult> subResult = features.execute(innerContext, entity);

			if (subResult.isEmpty()) {
				resultValues.addAll(createResultForNotContained(entity, dateContext).listResultLines());
				continue;
			}

			resultValues.addAll(
					ResultModifier.modify(
							subResult.get(),
							ResultModifier.existAggValuesSetterFor(features.getAggregators(), OptionalInt.of(0))
										  .unaryAndThen(v -> addConstants(v, dateContext))
					)
								  .listResultLines()
			);
		}

		return Optional.of(new MultilineEntityResult(entity.getId(), resultValues));
	}

	@Override
	public void init(QueryExecutionContext ctx, Entity entity) {
		features.init(ctx, entity);
	}

	private MultilineEntityResult createResultForNotContained(Entity entity, DateContext dateContext) {

		List<Object[]> result = new ArrayList<>();
		result.add(new Object[features.getAggregatorSize()]);

		return ResultModifier.modify(
				new MultilineEntityResult(entity.getId(), result),
				ResultModifier.existAggValuesSetterFor(getAggregators(), OptionalInt.of(0))
							  .unaryAndThen(v -> addConstants(v, dateContext))
		);
	}

	public List<Aggregator<?>> getAggregators() {
		return features.getAggregators();
	}

	private Object[] addConstants(Object[] values, DateContext dateContext) {
		Object[] result = new Object[values.length + constantCount];
		System.arraycopy(values, 0, result, constantCount, values.length);

		if (dateContext == null) {
			return result;
		}

		//add resolution indicator
		result[0] = dateContext.getSubdivisionMode().toString();
		//add index value
		result[1] = dateContext.getIndex();
		// add event date
		if (dateContext.getEventDate() != null) {
			result[2] = dateContext.getEventDate().toEpochDay();
		}
		//add date range at [2] or [3]
		result[getDateRangeResultPosition()] = dateContext.getDateRange();

		return result;
	}

	private int getDateRangeResultPosition() {
		return constantCount - 1;
	}

	@Override
	public boolean isOfInterest(Entity entity) {
		return features.isOfInterest(entity);
	}

	@Override
	public Optional<Aggregator<CDateSet>> getValidityDateAggregator() {
		return Optional.empty();
	}

	public int columnCount() {
		return constantCount + features.getAggregatorSize();
	}
}

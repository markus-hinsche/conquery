package com.bakdata.conquery.models.concepts.temporal;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.concept.CQElement;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.temporal.BeforeOrSameTemporalQueryNode;

/**
 * Creates a query that will contain all entities where {@code preceding} contains events that happened on the same day or before the events of {@code index}. And the time where this has happened.
 */
@CPSType(id = "BEFORE_OR_SAME", base = CQElement.class)
public class BeforeOrSameTemporalQuery extends AbstractTemporalQuery {

	/**
	 * Constructor for class.
	 */
	public BeforeOrSameTemporalQuery(CQElement index, CQElement preceding, TemporalSampler sampler) {
		super(index, preceding, sampler);
	}

	@Override
	public QPNode createQueryPlan(QueryPlanContext registry, QueryPlan plan) {
		QueryPlan indexPlan = QueryPlan.create();
		indexPlan.setRoot(index.createQueryPlan(registry, plan));

		QueryPlan precedingPlan = QueryPlan.create();
		precedingPlan.setRoot(preceding.createQueryPlan(registry, plan));

		return new BeforeOrSameTemporalQueryNode(indexPlan, precedingPlan, getSampler(), plan.getIncluded());
	}
}

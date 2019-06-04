package com.bakdata.conquery.models.query.concept.specific;

import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Set;

import javax.validation.Valid;

import org.hibernate.validator.constraints.NotEmpty;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.concept.CQElement;
import com.bakdata.conquery.models.query.concept.SelectDescriptor;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.queryplan.specific.AndNode;

import lombok.Getter;
import lombok.Setter;

@CPSType(id="AND", base=CQElement.class)
public class CQAnd implements CQElement {
	@Getter @Setter @NotEmpty @Valid
	private List<CQElement> children;

	@Override
	public QPNode createQueryPlan(QueryPlanContext context, QueryPlan plan) {
		QPNode[] aggs = new QPNode[children.size()];
		for(int i=0;i<aggs.length;i++) {
			aggs[i] = children.get(i).createQueryPlan(context, plan);
		}
		return new AndNode(Arrays.asList(aggs));
	}
	
	@Override
	public void collectRequiredQueries(Set<ManagedExecutionId> requiredQueries) {
		for(CQElement c:children) {
			c.collectRequiredQueries(requiredQueries);
		}
	}

	@Override
	public CQElement resolve(QueryResolveContext context) {
		children.replaceAll(c->c.resolve(context));
		return this;
	}
	
	@Override
	public void collectSelects(Deque<SelectDescriptor> select) {
		for(CQElement c:children) {
			c.collectSelects(select);
		}
	}

	@Override
	public void collectNamespacedIds(Set<NamespacedId> namespacedIds) {
		for(CQElement c:children) {
			c.collectNamespacedIds(namespacedIds);
		}
	}
}

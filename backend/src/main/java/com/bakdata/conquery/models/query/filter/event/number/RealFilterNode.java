package com.bakdata.conquery.models.query.filter.event.number;

import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;

public class RealFilterNode extends NumberFilterNode<Range.DoubleRange> {

	public RealFilterNode(Column column, Range.DoubleRange filterValue) {
		super(column, filterValue);
	}

	@Override
	public RealFilterNode clone(QueryPlan plan, QueryPlan clone) {
		return new RealFilterNode(getColumn(), filterValue);
	}

	@Override
	public boolean contains(Block block, int event) {
		return getFilterValue().contains(block.getReal(event, getColumn()));
	}
}

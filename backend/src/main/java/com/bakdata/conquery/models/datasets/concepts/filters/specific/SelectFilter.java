package com.bakdata.conquery.models.datasets.concepts.filters.specific;

import java.util.EnumSet;

import com.bakdata.conquery.apiv1.query.concept.filter.FilterValue;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.datasets.concepts.filters.SingleColumnFilter;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.query.filter.event.SelectFilterNode;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import lombok.Getter;
import lombok.Setter;

/**
 * This filter represents a select in the front end. This means that the user can select one or more values from a list of values.",
 * 
 * @jsonExample {"label":"gender","column":"reference_data.gender","type":"SELECT"}
 */
@Getter @Setter
@CPSType(id = "SINGLE_SELECT", base = Filter.class)
public class SelectFilter extends SingleColumnFilter<String> {

	//TODO this class is not used
	@Override
	public Class<? extends FilterValue<?>> getFilterType() {
		return FilterValue.CQSelectFilter.class;
	}

	@Override
	public EnumSet<MajorTypeId> getAcceptedColumnTypes() {
		return EnumSet.of(MajorTypeId.STRING);
	}



	@Override
	public FilterNode<?> createFilterNode(String value) {
		return new SelectFilterNode(getColumn(), value);
	}
}

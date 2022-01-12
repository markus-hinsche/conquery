package com.bakdata.conquery.models.datasets.concepts.filters.specific;

import com.bakdata.conquery.apiv1.query.concept.filter.FilterValue;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.query.filter.event.MultiSelectFilterNode;
import lombok.Getter;
import lombok.Setter;

/**
 * This filter represents a select in the front end. This means that the user can select one or more values from a list of values.
 */
@Getter
@Setter
@CPSType(id = "BIG_MULTI_SELECT", base = Filter.class)
public class BigMultiSelectFilter extends AbstractSelectFilter {


	@Override
	public Class<? extends FilterValue<?>> getFilterType() {
		return FilterValue.CQBigMultiSelectFilter.class;
	}

	@Override
	public MultiSelectFilterNode createFilterNode(String[] value) {
		return new MultiSelectFilterNode(getColumn(), value);
	}
}

package com.bakdata.conquery.models.concepts.filters.specific;

import java.util.EnumSet;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.api.description.FEFilter;
import com.bakdata.conquery.models.api.description.FEFilterType;
import com.bakdata.conquery.models.concepts.filters.Filter;
import com.bakdata.conquery.models.concepts.filters.GroupSingleColumnFilter;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.models.query.aggregators.filter.DurationLengthFilterNode;
import com.bakdata.conquery.models.query.concept.filter.FilterValue.CQIntegerRangeFilter;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import com.bakdata.conquery.models.types.MajorTypeId;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@Slf4j
@CPSType(id = "DURATION_SUM", base = Filter.class)
public class DurationSumFilter extends GroupSingleColumnFilter<CQIntegerRangeFilter> {

	private static final long serialVersionUID = 1L;

	@Override
	public EnumSet<MajorTypeId> getAcceptedColumnTypes() {
		return EnumSet.of(MajorTypeId.DATE_RANGE);
	}

	@Override
	public void configureFrontend(FEFilter f) throws ConceptConfigurationException {
		switch (getColumn().getType()) {
			case DATE_RANGE: {
				f.setType(FEFilterType.INTEGER_RANGE);
				f.setMin(0);
				return;
			}
			default:
				throw new ConceptConfigurationException(getConnector(), "DURATION_SUM filter is incompatible with columns of type " + getColumn().getType());
		}
	}

	@Override
	public FilterNode createAggregator(CQIntegerRangeFilter filterValue) {
		return new DurationLengthFilterNode(this, filterValue);
	}
}

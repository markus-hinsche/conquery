package com.bakdata.conquery.models.query.queryplan.filter;

import com.bakdata.conquery.models.query.queryplan.EventIterating;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
public abstract class FilterNode<FILTER_VALUE> implements EventIterating {

	@Setter @Getter
	protected FILTER_VALUE filterValue;

	public abstract boolean isContained();
	
}

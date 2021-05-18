package com.bakdata.conquery.models.concepts.conditions;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.concepts.tree.ConceptTreeNode;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.util.CalculatedValue;
import lombok.Getter;
import lombok.Setter;

/**
 * This condition connects multiple conditions with an and.
 */
@CPSType(id="AND", base= ConceptTreeCondition.class)
public class AndCondition implements ConceptTreeCondition {

	@Setter @Getter @Valid @NotEmpty
	private List<ConceptTreeCondition> conditions;

	@Override
	public boolean matches(String value, CalculatedValue<Map<String, Object>> rowMap) throws ConceptConfigurationException {
		for(ConceptTreeCondition cond:conditions) {
			if(!cond.matches(value, rowMap)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public Collection<String> getPrefixTree() {
		return conditions.stream().map(ConceptTreeCondition::getPrefixTree).flatMap(Collection::stream).collect(Collectors.toSet());
	}


	@Override
	public void init(ConceptTreeNode node) throws ConceptConfigurationException {
		for(ConceptTreeCondition cond:conditions) {
			cond.init(node);
		}
	}
}

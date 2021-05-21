package com.bakdata.conquery.models.concepts.conditions;

import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import javax.validation.constraints.NotEmpty;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.util.CalculatedValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.dropwizard.validation.ValidationMethod;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * This condition requires each value to start with a prefix between the two given values
 */
@CPSType(id="PREFIX_RANGE", base= ConceptTreeCondition.class)
@RequiredArgsConstructor(onConstructor_ = @JsonCreator)
public class PrefixRangeCondition implements ConceptTreeCondition {

	@Getter @NotEmpty
	private final String min;
	@Getter @NotEmpty
	private final String max;
	
	@ValidationMethod(message="Min and max need to be of the same length and min needs to be smaller than max.") @JsonIgnore
	public boolean isValidMinMax() {
		if(min.length()!=max.length()) {
			return false;
		}
		return min.compareTo(max)<0;
	}


	@Override
	public boolean matches(String value, CalculatedValue<Map<String, Object>> rowMap) {
		if (value.length() >= min.length()) {
			String pref = value.substring(0, min.length());
			return min.compareTo(pref) <= 0 && max.compareTo(pref) >= 0;
		}

		return false;
	}

}

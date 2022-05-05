package com.bakdata.conquery.models.datasets.concepts.filters.specific;

import java.util.EnumSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.apiv1.FilterTemplate;
import com.bakdata.conquery.apiv1.frontend.FEFilter;
import com.bakdata.conquery.apiv1.frontend.FEValue;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.config.CSVConfig;
import com.bakdata.conquery.models.datasets.concepts.Searchable;
import com.bakdata.conquery.models.datasets.concepts.filters.SingleColumnFilter;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import io.dropwizard.validation.ValidationMethod;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Setter
@Getter
@NoArgsConstructor
@Slf4j
@JsonIgnoreProperties({"searchType"})
public abstract class SelectFilter<FE_TYPE> extends SingleColumnFilter<FE_TYPE> implements Searchable {

	/**
	 * user given mapping from the values in the CSVs to shown labels
	 */
	protected BiMap<String, String> labels = ImmutableBiMap.of();


	private FilterTemplate template;

	@JsonIgnore
	public abstract String getFilterType();

	@Override
	public EnumSet<MajorTypeId> getAcceptedColumnTypes() {
		return EnumSet.of(MajorTypeId.STRING);
	}

	@Override
	public void configureFrontend(FEFilter f) throws ConceptConfigurationException {
		f.setTemplate(getTemplate());
		f.setType(getFilterType());

		f.setOptions(labels.entrySet().stream()
						   .map(entry -> new FEValue(entry.getKey(), entry.getValue()))
						   .collect(Collectors.toList()));
	}

	@JsonIgnore
	@ValidationMethod(message = "Cannot use both labels and template.")
	public boolean isNotUsingTemplateAndLabels() {
		// Technically it's possible it just doesn't make much sense and would lead to Single-Point-of-Truth confusion.
		if (getTemplate() == null && labels.isEmpty()) {
			return true;
		}

		return (getTemplate() == null) != labels.isEmpty();
	}

	private int searchMinSuffixLength = 3;
	private boolean generateSearchSuffixes = true;

	@Override
	@JsonIgnore
	public boolean isGenerateSuffixes() {
		return generateSearchSuffixes;
	}

	@Override
	@JsonIgnore
	public int getMinSuffixLength() {
		return searchMinSuffixLength;
	}

	@Override
	public Stream<FEValue> getSearchValues(CSVConfig config, NamespaceStorage storage) {
		return labels.entrySet().stream()
					 .map(entry -> new FEValue(entry.getKey(), entry.getValue()));
	}
}

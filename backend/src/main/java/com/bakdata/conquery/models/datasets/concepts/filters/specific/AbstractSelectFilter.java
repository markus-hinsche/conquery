package com.bakdata.conquery.models.datasets.concepts.filters.specific;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.bakdata.conquery.apiv1.FilterTemplate;
import com.bakdata.conquery.apiv1.frontend.FEFilter;
import com.bakdata.conquery.apiv1.frontend.FEFilterType;
import com.bakdata.conquery.apiv1.frontend.FEValue;
import com.bakdata.conquery.io.storage.NamespacedStorage;
import com.bakdata.conquery.models.config.CSVConfig;
import com.bakdata.conquery.models.datasets.concepts.filters.SingleColumnFilter;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.events.stores.root.StringStore;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.util.search.TrieSearch;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.univocity.parsers.common.IterableResult;
import com.univocity.parsers.common.ParsingContext;
import com.univocity.parsers.common.record.Record;
import com.univocity.parsers.csv.CsvParser;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringSubstitutor;
import org.apache.commons.text.lookup.StringLookup;

@Getter
@Setter
@RequiredArgsConstructor
@Slf4j
@JsonIgnoreProperties(ignoreUnknown = true, value = {"values"}) //TODO this is a hotfix because we cannot reimport this late
public abstract class AbstractSelectFilter<FE_TYPE> extends SingleColumnFilter<FE_TYPE> {

	/**
	 * user given mapping from the values in the CSVs to shown labels
	 */
	protected BiMap<String, String> labels = ImmutableBiMap.of();

	@JsonIgnore
	private final int maximumSize;
	@JsonIgnore
	private final FEFilterType filterType;

	private FilterTemplate template;


	@Override
	public EnumSet<MajorTypeId> getAcceptedColumnTypes() {
		return EnumSet.of(MajorTypeId.STRING);
	}

	@Override
	public void configureFrontend(FEFilter f) throws ConceptConfigurationException {
		f.setTemplate(getTemplate());
		f.setType(filterType);


		f.setOptions(
				labels.entrySet().stream()
					  .map(entry -> new com.bakdata.conquery.apiv1.frontend.FEValue(entry.getValue(), entry.getKey()))
					  .collect(Collectors.toList())
		);
	}


	@JsonIgnore
	public List<String> getSearchReferences() {
		final List<String> references = new ArrayList<>(3);

		if (getTemplate() != null) {
			references.add(getTemplate().getFilePath());
		}

		references.add(getId().toString());
		references.add(getColumn().getId().toString());

		return references;
	}


	public void collectSourceSearchTasks(CSVConfig parserConfig, NamespacedStorage storage, Map<String, List<Consumer<TrieSearch<FEValue>>>> suppliers) {

		// Collect data from csv template
		if (getTemplate() != null) {
			String id = getTemplate().getFilePath();
			suppliers.put(id, List.of((search) -> collectTemplateSearchItems(parserConfig, search)));
		}


		// Collect data from labels
		if (!getLabels().isEmpty()) {
			String id = getId().toString();
			suppliers.put(id, List.of(this::collectLabeledSearchItems));
		}

		// Collect data from raw underlying data, try to unify among columns if at all possible
		//TODO this is not right: Search has to be reused here since columns are not necessarily filled with identical data!
		{
			String id = getColumn().getId().toString();

			if (getColumn().getSharedDictionary() != null) {
				id = getColumn().getSharedDictionary();
			}
			else if (getColumn().getSecondaryId() != null) {
				id = getColumn().getSecondaryId().toString();
			}

			suppliers.computeIfAbsent(id, (ignored) -> new ArrayList<>())
					 .add((search) -> collectRawSearchItems(storage, search));
		}
	}

	/**
	 * Generate search from provided labels.
	 *
	 * @param search
	 */
	private TrieSearch<FEValue> collectLabeledSearchItems(TrieSearch<FEValue> search) {

		if (labels.isEmpty()) {
			return search;
		}

		log.info("BEGIN processing {} labels for {}", labels.size(), getId());

		for (Map.Entry<String, String> entry : labels.entrySet()) {
			String value = entry.getKey();
			String label = entry.getValue();

			final FEValue item = new FEValue(label, value, null);

			search.addItem(item, item.extractKeywords());
		}

		log.debug("DONE processing {} labels for {}", labels.size(), getId());

		return search;
	}

	/**
	 * Helper class to be used with {@link StringSubstitutor} and {@link CsvParser}.
	 */
	@Data
	private static class MutableRecordBackedLookup implements StringLookup {
		private Record record;

		@Override
		public String lookup(String key) {
			return record.getString(key);
		}
	}

	/**
	 * Collect search results based on provided CSV with prepopulated values.
	 */
	private TrieSearch<FEValue> collectTemplateSearchItems(CSVConfig parserConfig, TrieSearch<FEValue> search) {

		final Set<String> ids = new HashSet<>();


		log.info("BEGIN Processing template {}", getTemplate());
		final long time = System.currentTimeMillis();

		final CsvParser parser = parserConfig.createParser();

		final MutableRecordBackedLookup lookup = new MutableRecordBackedLookup();
		final StringSubstitutor substitutor = new StringSubstitutor(lookup, "{{", "}}", StringSubstitutor.DEFAULT_ESCAPE);

		try {
			File file = new File(template.getFilePath());

			final IterableResult<Record, ParsingContext> records = parser.iterateRecords(file);


			for (Record row : records) {

				lookup.setRecord(row);

				final String rowId = row.getString(template.getColumnValue());

				if (!ids.add(rowId)) {
					log.error("Source file ({}) contains multiple entries for `{}`", file, rowId);
					continue;
				}

				final String label = substitutor.replace(template.getValue());
				final String optionValue = substitutor.replace(template.getOptionValue());

				FEValue item = new FEValue(label, rowId, optionValue);

				search.addItem(item, item.extractKeywords());
			}

			final long duration = System.currentTimeMillis() - time;

			log.info("DONE Processing reference for {} in {} ms ({} Items in {} Lines)",
					 getTemplate(), duration, search.calculateSize(), records.getContext().currentLine()
			);

			return search;
		}
		finally {
			parser.stopParsing();
		}

	}


	/**
	 * Collect search Items from raw data in.
	 */
	private TrieSearch<FEValue> collectRawSearchItems(NamespacedStorage storage, TrieSearch<FEValue> search) {
		//TODO explain this really bananas thing
		log.info("BEGIN processing values for {}", getColumn().getId());


		getConnector().getTable().findImports(storage)
					  .forEach(
							  imp -> {
								  Set<String> items = search.listItems().stream()
															.map(FEValue::getValue)
															.collect(Collectors.toSet());

								  for (String value : ((StringStore) getColumn().getTypeFor(imp))) {
									  if (items.contains(value)) {
										  continue;
									  }

									  final FEValue item = new FEValue(value, value);
									  search.addItem(item, item.extractKeywords());
								  }
							  }
					  );

		log.debug("DONE processing values for {} with {} Items", getColumn().getId(), search.calculateSize());

		return search;
	}
}

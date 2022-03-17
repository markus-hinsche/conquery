package com.bakdata.conquery.apiv1;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Stream;

import com.bakdata.conquery.apiv1.frontend.FEValue;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.config.CSVConfig;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.AbstractSelectFilter;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.jobs.SimpleJob;
import com.bakdata.conquery.util.search.TrieSearch;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@NoArgsConstructor
public class FilterSearch {

	private final Map<String, TrieSearch<FEValue>> searchCache = new HashMap<>();

	public TrieSearch<FEValue> getSearchFor(String reference) {
		return searchCache.getOrDefault(reference, new TrieSearch<>());
	}

	/**
	 * Scan all SelectFilters and submit {@link SimpleJob}s to create interactive searches for them.
	 */
	public void updateSearch(NamespaceStorage storage, JobManager jobManager, CSVConfig parser) {

		jobManager.addSlowJob(new SimpleJob("Initialize Source Search", () -> {

			final Map<String, List<Stream<FEValue>>> suppliers = new HashMap<>();

			storage.getAllConcepts().stream()
				   .flatMap(c -> c.getConnectors().stream())
				   .flatMap(co -> co.collectAllFilters().stream())
				   .filter(f -> f instanceof AbstractSelectFilter)
				   .map(f -> ((AbstractSelectFilter<?>) f))
				   .forEach(f -> f.collectSourceSearchTasks(parser, storage, suppliers));

			final ExecutorService service = Executors.newCachedThreadPool();


			for (Map.Entry<String, List<Stream<FEValue>>> entry : suppliers.entrySet()) {

				service.submit(() -> {
					final String id = entry.getKey();

					try {
						final List<Stream<FEValue>> fillers = entry.getValue();

						final TrieSearch<FEValue> search = new TrieSearch<>();


						fillers.stream()
							   .flatMap(Function.identity())
							   .distinct()
							   .forEach(item -> search.addItem(item, item.extractKeywords()));

						searchCache.put(id, search);

						log.info("Stats for `{}`", id);
						search.logStats();
						search.shrinkToFit();
					}
					catch (Exception e) {
						log.error("Failed to create search for {}", id, e);
					}
				});
			}

			service.shutdown();

			service.awaitTermination(10, TimeUnit.HOURS);
		}));


	}
}

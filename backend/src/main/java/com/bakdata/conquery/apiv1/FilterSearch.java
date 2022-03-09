package com.bakdata.conquery.apiv1;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

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
		final Map<String, List<Consumer<TrieSearch<FEValue>>>> suppliers = new HashMap<>();

		storage.getAllConcepts().stream()
			   .flatMap(c -> c.getConnectors().stream())
			   .flatMap(co -> co.collectAllFilters().stream())
			   .filter(f -> f instanceof AbstractSelectFilter)
			   .map(f -> ((AbstractSelectFilter<?>) f))
			   .forEach(f -> f.collectSourceSearchTasks(parser, storage, suppliers));


		jobManager.addSlowJob(new SimpleJob("Initialize Source Search", () -> {
			ExecutorService service = Executors.newCachedThreadPool();

			suppliers.forEach((id, fillers) -> {
				service.submit(() -> {

					final TrieSearch<FEValue> search = new TrieSearch<>();

					for (Consumer<TrieSearch<FEValue>> filler : fillers) {
						filler.accept(search);
					}

					searchCache.put(id, search);

					log.info("Stats for `{}`", id);
					search.logStats();
				});
			});

			service.shutdown();

			service.awaitTermination(10, TimeUnit.HOURS);
		}));


	}
}

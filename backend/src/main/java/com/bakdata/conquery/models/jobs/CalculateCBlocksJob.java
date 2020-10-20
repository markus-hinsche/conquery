package com.bakdata.conquery.models.jobs;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.bakdata.conquery.io.xodus.WorkerStorage;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.concepts.Connector;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.BucketEntry;
import com.bakdata.conquery.models.events.BucketManager;
import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.identifiable.ids.specific.CBlockId;
import com.bakdata.conquery.models.worker.Worker;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Calculate CBlocks, ie the Connection between a Concept and a Bucket.
 * <p>
 * If a Bucket x Connector has a CBlock, the ConceptNode will rely on that to iterate events. If not, it will fall back onto equality checks.
 */
@RequiredArgsConstructor
@Slf4j
public class CalculateCBlocksJob extends Job {

	private final List<CalculationInformation> infos = new ArrayList<>();
	private final WorkerStorage storage;
	private final Worker worker;
	private final BucketManager bucketManager;
	private final Connector connector;
	private final Table table;

	@Override
	public String getLabel() {
		return "Calculate " + infos.size() + " CBlocks for " + connector.getId();
	}

	public void addCBlock(Import imp, Bucket bucket, CBlockId cBlockId) {
		infos.add(new CalculationInformation(bucket, cBlockId));
	}

	@Override
	public void execute() throws Exception {
		getProgressReporter().setMax(infos.size());

		final ListeningExecutorService service = MoreExecutors.listeningDecorator(worker.getExecutorService());

		log.info("Starting to calculate {} CBlocks.", infos.size());

		final List<ListenableFuture<CBlock>> cBlockFutures =
				infos.stream()
					 .filter(info -> !bucketManager.hasCBlock(info.getCBlockId()))
					 .map(info -> service.submit(() -> doCalculateCBlock(info)))
					 .collect(Collectors.toList());

		final List<CBlock> cBlocks = Futures.allAsList(cBlockFutures).get();

		log.info("Finished calculating CBlocks.");

		for (CBlock cBlock : cBlocks) {

			// Might have failed.
			if (cBlock == null) {
				continue;
			}

			bucketManager.addCalculatedCBlock(cBlock);
			storage.addCBlock(cBlock);
		}

		getProgressReporter().done();
	}

	public CBlock doCalculateCBlock(CalculationInformation info) {
		try {
			CBlock cBlock = new CBlock(info.getBucket().getId(), connector.getId(), info.getBucket().getBucketSize());

			connector.calculateCBlock(cBlock, info.getBucket());

			calculateEntityDateIndices(cBlock, info.getBucket(), storage.getDataset().getTables().get(info.getBucket().getImp().getTable()));

			return cBlock;
		}
		catch (Exception e) {
			log.warn("Exception in CalculateCBlocksJob for {}", info, e);
		}
		finally {
			getProgressReporter().report(1);
		}

		return null;
	}

	/**
	 * For every included entity, calculate min and max and store them as statistics in the CBlock.
	 */
	private static void calculateEntityDateIndices(CBlock cBlock, Bucket bucket, Table table) {
		for (Column column : table.getColumns()) {
			if (!column.getType().isDateCompatible()) {
				continue;
			}

			for (BucketEntry entry : bucket.entries()) {
				if (!bucket.has(entry.getEvent(), column)) {
					continue;
				}

				CDateRange range = bucket.getAsDateRange(entry.getEvent(), column);

				cBlock.getMinDate()[entry.getLocalEntity()] = Math.min(cBlock.getMinDate()[entry.getLocalEntity()], range.getMinValue());

				cBlock.getMaxDate()[entry.getLocalEntity()] = Math.max(cBlock.getMaxDate()[entry.getLocalEntity()], range.getMaxValue());
			}
		}
	}

	public boolean isEmpty() {
		return infos.isEmpty();
	}

	@RequiredArgsConstructor
	@Data
	private static class CalculationInformation {
		private final Bucket bucket;
		private final CBlockId cBlockId;
	}
}

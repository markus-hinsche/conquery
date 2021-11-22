package com.bakdata.conquery.models.datasets.concepts.filters.postalcode;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import com.univocity.parsers.common.ParsingContext;
import com.univocity.parsers.common.processor.AbstractRowProcessor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.internal.guava.HashBasedTable;
import org.glassfish.jersey.internal.guava.Table;

@Slf4j
public class PostalCodeProcessor extends AbstractRowProcessor {


	final Table<Integer, Integer, Double> csvEntries = HashBasedTable.create();
	private int plz1Index, plz2Index, distanceIndex;

	@Getter
	final private List<PostalCodeRecord> data = new ArrayList<>();

	@Override
	public void processStarted(ParsingContext context) {
		super.processStarted(context);
		final String[] headers = context.headers();
		plz1Index = IntStream.range(0, headers.length).filter(i -> headers[i].equals("plz1")).findFirst()..orElseThrow(() -> new IllegalStateException("Required Column[plz1] is missing in Headers."));
		

		plz2Index = IntStream.range(0, headers.length).filter(i -> headers[i].equals("plz2")).findFirst().orElse(-1);
		if (plz2Index == -1) {
			throw new IllegalStateException("Column plz2 not found in the csv file");
		}

		distanceIndex = IntStream.range(0, headers.length).filter(i -> headers[i].equals("Distanz_convert_in_km")).findFirst().orElse(-1);
		if (distanceIndex == -1) {
			throw new IllegalStateException("Column Distanz_in_km not found in the csv file");
		}


		context.skipLines(1); //skip headers when reading rows
	}


	@Override
	public void rowProcessed(String[] row, ParsingContext context) {

		if (row.length < 3) {
			throw new IllegalStateException(String.format("Line %d of given csv does not contain enough column-values", context.currentLine()));
		}
		int plz1 = Integer.parseInt(row[plz1Index].trim());
		int plz2 = Integer.parseInt(row[plz2Index].trim());
		double distance = Double.parseDouble(row[distanceIndex].trim());

		//if the element has not been already added then we add the new record
		if (csvEntries.put(plz1, plz2, distance) == null) {
			data.add(new PostalCodeRecord(plz1, plz2, distance));
		}


	}
}

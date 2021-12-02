package com.bakdata.conquery.models.datasets.concepts.filters.postalcode;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.GZIPInputStream;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;

import com.github.powerlibraries.io.In;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)

public class PostalCodesManager {
	private final List<PostalCodeDistance> data;

	/**
	 * This method loads the postcodes and their distances between each other. The loaded postal codes will be passed to the created {@link PostalCodesManager}
	 *
	 * @param csvFilePath Path of file containing the postal codes data as csv
	 * @return Preloaded  {@link PostalCodesManager}
	 */
	static public PostalCodesManager loadFrom(@NonNull @NotEmpty String csvFilePath, boolean zipped) throws IOException {

		final PostalCodeProcessor rowProcessor = new PostalCodeProcessor();
		final CsvParserSettings csvParserSettings = new CsvParserSettings();
		csvParserSettings.setDelimiterDetectionEnabled(true);
		csvParserSettings.setHeaderExtractionEnabled(true);
		csvParserSettings.setProcessor(rowProcessor);

		final CsvParser parser = new CsvParser(csvParserSettings);
		if (zipped) {
			parser.parse(new InputStreamReader(new GZIPInputStream(In.resource(csvFilePath).asStream()), StandardCharsets.US_ASCII));
		}
		else {
			parser.parse(new InputStreamReader(In.resource(csvFilePath).asStream(), StandardCharsets.US_ASCII));
		}
		return new PostalCodesManager(rowProcessor.getData());

	}


	/**
	 * This method filters out all postcodes that are within the specified distance radius from the specified reference postcode (reference-postcode included).
	 */
	public String[] filterAllNeighbours(@Min(1) int plz, @Min(0) double radius) {

		if (radius == 0) {
			return new String[]{String.format("%05d", plz)};
		}

		return data.stream()
				   .takeWhile(postalCodeDistance -> postalCodeDistance.getDistanceInKm() <= radius)
				   .filter(postalCodeDistance -> postalCodeDistance.getLeft() == plz || postalCodeDistance.getRight() == plz)
				   .map(postalCodeDistance -> {
					   if (postalCodeDistance.getLeft() == plz) {
						   return String.format("%05d", postalCodeDistance.getRight());
					   }
					   else {
						   return String.format("%05d", postalCodeDistance.getLeft());
					   }
				   })
				   .toArray(String[]::new);


	}
}


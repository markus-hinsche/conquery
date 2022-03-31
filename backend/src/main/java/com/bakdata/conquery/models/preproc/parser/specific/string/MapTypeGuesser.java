package com.bakdata.conquery.models.preproc.parser.specific.string;

import java.util.UUID;

import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.dictionary.MapDictionary;
import com.bakdata.conquery.models.events.stores.root.IntegerStore;
import com.bakdata.conquery.models.events.stores.root.StringStore;
import com.bakdata.conquery.models.events.stores.specific.string.StringTypeDictionary;
import com.bakdata.conquery.models.preproc.parser.specific.StringParser;
import lombok.RequiredArgsConstructor;

/**
 * Map implementation using {@link MapDictionary} implementation.
 */
@RequiredArgsConstructor
public class MapTypeGuesser extends StringTypeGuesser {

	private final StringParser parser;

	@Override
	public Guess createGuess() {
		IntegerStore indexType = parser.decideIndexType();

		StringTypeDictionary type = new StringTypeDictionary(indexType, null);
		long mapSize = MapDictionary.estimateMemoryConsumption(
				parser.getStrings().size(),
				parser.getStrings().keySet().stream().mapToLong(String::length).sum()
		);



		return new Guess(
				type,
				indexType.estimateMemoryConsumptionBytes(),
				mapSize
		) {
			@Override
			public StringStore getType() {
				MapDictionary map = new MapDictionary(Dataset.PLACEHOLDER, UUID.randomUUID().toString(), parser.getEncoding());

				parser.valuesOrdered().forEach(map::add);

				type.setDictionary(map);
				return super.getType();
			}
		};
	}

}

package com.bakdata.conquery.models.types;

import static org.assertj.core.api.Assertions.assertThat;

import com.bakdata.conquery.models.config.ParserConfig;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class MajorTypesTest {

	public static MajorTypeId[] reflection() {
		return MajorTypeId.values();
	}

	@ParameterizedTest
	@MethodSource
	public void reflection(MajorTypeId typeId) {
		CType<?> type = typeId.createParser(new ParserConfig()).findBestType();
		assertThat(type.getTypeId())
				.isNotNull()
				.isEqualTo(typeId);
	}
}

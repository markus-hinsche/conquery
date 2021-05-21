//package com.bakdata.conquery.models.concepts.conditions;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.junit.jupiter.api.Assertions.*;
//
//import java.util.Set;
//
//import org.junit.jupiter.api.Test;
//
//class PrefixRangeConditionTest {
//	@Test
//	public void test() {
//		final Set<String> strings = PrefixRangeCondition.buildStringRange("C99", "D01");
//		assertThat(strings).isEqualTo(Set.of("c00", "c01"));
//	}
//
//	@Test
//	public void test2() {
//		final PrefixRangeCondition condition = new PrefixRangeCondition();
//		condition.setMin("C00");
//		condition.setMax("D48");
//
//
//		assertThat(condition.matches("C~~", null)).isTrue();
//	}
//
//
//}
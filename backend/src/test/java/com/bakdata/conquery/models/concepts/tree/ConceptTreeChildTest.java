package com.bakdata.conquery.models.concepts.tree;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.bakdata.conquery.models.concepts.conditions.ColumnEqualCondition;
import com.bakdata.conquery.models.concepts.conditions.ConceptTreeCondition;
import com.bakdata.conquery.models.concepts.conditions.PrefixCondition;
import com.bakdata.conquery.models.concepts.conditions.PrefixRangeCondition;
import com.bakdata.conquery.models.concepts.tree.validation.Prefix;
import com.bakdata.conquery.models.datasets.Dataset;
import com.google.common.collect.ImmutableRangeSet;
import com.google.common.collect.Range;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConceptTreeChildTest {
	private final Dataset dataset = new Dataset("dataset");
	private final TreeConcept concept = new TreeConcept();

	@BeforeEach
	public void beforeAll() {
		concept.setDataset(dataset);
		concept.setName("concept");
	}

	@Test
	public void columnSpan() {
		final ConceptTreeChild root = new ConceptTreeChild("root");
		root.setParent(concept);
		root.setCondition(new PrefixRangeCondition("A00", "A10"));

		final ConceptTreeChild child1 = new ConceptTreeChild();
		child1.setCondition(new PrefixRangeCondition("A01", "A02.2"));
		child1.setParent(concept);

		final ConceptTreeChild child2 = new ConceptTreeChild();
		child2.setCondition(new ColumnEqualCondition(Set.of("A01"), "my_column"));
		child2.setParent(concept);

		root.setChildren(List.of(child1, child2));

		final Map<String, ImmutableRangeSet<Prefix>> should_span = Map.of(
				ConceptTreeCondition.COLUMN_PLACEHOLDER, ImmutableRangeSet.of(Range.closed(Prefix.prefix("A00"), Prefix.prefix("A10"))),
				"my_column", ImmutableRangeSet.of(Range.singleton(Prefix.equal("A01")))
		);

		assertThat(root.getColumnSpan()).isEqualTo(should_span);
	}

	@Test
	public void enclosesSubRange() {
		final ConceptTreeChild root = new ConceptTreeChild("root");
		root.setCondition(new PrefixRangeCondition("A00", "A10"));
		root.setParent(concept);

		final ConceptTreeChild child = new ConceptTreeChild("child");
		child.setCondition(new PrefixRangeCondition("A01", "A02.2"));
		child.setParent(root);

		root.setChildren(List.of(child));

		assertThat(root.isEnclosingChildren()).isTrue();
	}

	@Test
	public void enclosesSelfRange() {
		final ConceptTreeChild root = new ConceptTreeChild("root");
		root.setCondition(new PrefixRangeCondition("A00", "A10"));
		root.setParent(concept);

		final ConceptTreeChild child = new ConceptTreeChild("child");
		child.setCondition(root.getCondition());
		child.setParent(root);

		root.setChildren(List.of(child));
		assertThat(root.isEnclosingChildren()).isTrue();
	}

	@Test
	public void enclosesOnOtherDimension() {
		final ConceptTreeChild root = new ConceptTreeChild("root");
		root.setCondition(new PrefixRangeCondition("A00", "A10"));
		root.setParent(concept);

		final ConceptTreeChild child = new ConceptTreeChild("child");
		child.setCondition(new ColumnEqualCondition(Set.of("G"), "column"));
		child.setParent(root);

		root.setChildren(List.of(child));
		assertThat(root.isEnclosingChildren()).isTrue();
	}

	@Test
	public void doesNotEncloseExcessSingleton() {
		final ConceptTreeChild root = new ConceptTreeChild("root");
		root.setCondition(new PrefixRangeCondition("A00", "A10"));
		root.setParent(concept);

		final ConceptTreeChild child = new ConceptTreeChild("child");
		child.setCondition(new PrefixCondition(new String[]{"A11"}));
		child.setParent(root);

		root.setChildren(List.of(child));

		assertThat(root.isEnclosingChildren()).isFalse();
	}

	@Test
	public void overlaps() {
		final ConceptTreeChild root = new ConceptTreeChild("root");
		root.setCondition(new PrefixRangeCondition("A00", "A10"));
		root.setParent(concept);

		final ConceptTreeChild child1 = new ConceptTreeChild("child1");
		child1.setCondition(new PrefixRangeCondition("A01", "A04.2"));
		child1.setParent(root);

		root.setChildren(List.of(child1));

		assertThat(root.isChildrenAreNonOverlapping()).isTrue();


		final ConceptTreeChild child2 = new ConceptTreeChild("child2");
		child2.setCondition(new PrefixCondition(new String[]{"A11"}));
		child2.setParent(root);

		root.setChildren(List.of(child1, child2));

		assertThat(root.isChildrenAreNonOverlapping()).isTrue();

		final ConceptTreeChild child3 = new ConceptTreeChild("child3");
		child3.setCondition(new PrefixCondition(new String[]{"A03.5"}));
		root.setChildren(List.of(child1, child3));
		child3.setParent(root);

		assertThat(root.isChildrenAreNonOverlapping()).isFalse();

		final ConceptTreeChild child4 = new ConceptTreeChild("child4");
		child4.setCondition(new PrefixRangeCondition("A02", "A07"));
		child4.setParent(root);
		root.setChildren(List.of(child1, child4));

		assertThat(root.isChildrenAreNonOverlapping()).isFalse();
	}

}
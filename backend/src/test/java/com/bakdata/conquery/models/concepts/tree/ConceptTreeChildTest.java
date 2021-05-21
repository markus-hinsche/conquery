package com.bakdata.conquery.models.concepts.tree;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import com.bakdata.conquery.models.concepts.conditions.PrefixCondition;
import com.bakdata.conquery.models.concepts.conditions.PrefixRangeCondition;
import com.bakdata.conquery.models.datasets.Dataset;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.junit.jupiter.api.Test;

@Slf4j
class ConceptTreeChildTest {
	@Test
	public void canBuildTrie() {

		final PatriciaTrie<ConceptTreeNode<?>> trie = new PatriciaTrie<>();

		final TreeConcept concept = new TreeConcept();
		concept.setName("concept");
		concept.setDataset(Dataset.PLACEHOLDER);

		final ConceptTreeChild parent = new ConceptTreeChild();

		parent.setName("parent");
		parent.setParent(concept);

		parent.setCondition(new PrefixRangeCondition("A0", "A1"));

		final ConceptTreeChild child1 = new ConceptTreeChild();
		child1.setCondition(new PrefixRangeCondition("A01", "A03"));

		child1.setName("child1");
		child1.setParent(parent);


		final ConceptTreeChild child2 = new ConceptTreeChild();
		child2.setCondition(new PrefixRangeCondition("A1", "A3"));

		child2.setName("child2");
		child2.setParent(parent);

		parent.setChildren(List.of(child1, child2));

		parent.getPrefixTree(trie);


		log.info("{}", trie);
	}

}
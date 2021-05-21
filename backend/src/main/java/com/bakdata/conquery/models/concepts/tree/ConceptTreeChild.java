package com.bakdata.conquery.models.concepts.tree;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.validation.Valid;
import javax.validation.ValidationException;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.models.concepts.ConceptElement;
import com.bakdata.conquery.models.concepts.conditions.ConceptTreeCondition;
import com.bakdata.conquery.models.concepts.conditions.PrefixCondition;
import com.bakdata.conquery.models.concepts.conditions.PrefixRangeCondition;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptTreeChildId;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.Trie;
import org.apache.commons.collections4.trie.PatriciaTrie;

@Getter
@Setter
@Slf4j
public class ConceptTreeChild extends ConceptElement<ConceptTreeChildId> implements ConceptTreeNode<ConceptTreeChildId> {

	@JsonIgnore
	private transient int[] prefix;

	@JsonManagedReference
	@Valid
	private List<ConceptTreeChild> children = Collections.emptyList();

	@JsonIgnore
	private int localId;

	@JsonBackReference
	private ConceptTreeNode<?> parent;

	@JsonIgnore
	private int depth = 0;

	@NotNull
	private ConceptTreeCondition condition = null;

	@JsonIgnore
	private TreeChildPrefixIndex childIndex;

	@Override
	@JsonIgnore
	public int[] getPrefix() {
		if (prefix == null) {
			int[] pPrefix = getParent().getPrefix();
			prefix = Arrays.copyOf(pPrefix, pPrefix.length + 1);
			prefix[prefix.length - 1] = this.getLocalId();
		}
		return prefix;
	}

	public void init() throws ConceptConfigurationException {
		if (condition != null) {
			condition.init(this);
		}
	}

	@Override
	public ConceptTreeChildId createId() {
		return new ConceptTreeChildId(parent.getId(), getName());
	}

	@Override
	@JsonIgnore
	public TreeConcept getConcept() {
		ConceptTreeNode<?> n = this;
		while (n != null) {
			if (n instanceof TreeConcept) {
				return (TreeConcept) n;
			}
			n = n.getParent();
		}
		throw new IllegalStateException("The node " + this + " seems to have no root");
	}

	@Override
	public boolean matchesPrefix(int[] conceptPrefix) {
		return conceptPrefix.length > depth && conceptPrefix[depth] == localId;
	}

	@Override
	public long calculateBitMask() {
		if (getLocalId() < 64) {
			return 1L << getLocalId();
		}
		return getParent().calculateBitMask();
	}

	@JsonIgnore
	@Override
	public Dataset getDataset() {
		return getConcept().getDataset();
	}

	public boolean isDescendant(ConceptTreeNode<?> other) {
		if (children == null) {
			return false;
		}

		if (children.contains(other)) {
			return true;
		}

		return children.stream().anyMatch(child -> isDescendant(other));
	}

	@Data
	public static class TreeValidationException extends ValidationException {
		private final ConceptTreeNode<?> parent;
		private final ConceptTreeNode<?> illegalChild;

	}

	@JsonIgnore
	public Trie<String, ConceptTreeNode<?>> getPrefixTree(PatriciaTrie<ConceptTreeNode<?>> trie) {

		if (children != null) {
			children.forEach(child -> child.getPrefixTree(trie));
		}


		if (condition instanceof PrefixRangeCondition) {

			validateAreDescendants(trie.headMap(((PrefixRangeCondition) condition).getMin()).values());
			validateAreDescendants(trie.tailMap(((PrefixRangeCondition) condition).getMax()).values());

			trie.put(((PrefixRangeCondition) condition).getMin(), this);
			trie.put(((PrefixRangeCondition) condition).getMax(), this);
		}

		if (condition instanceof PrefixCondition) {
			for (String prefix : ((PrefixCondition) condition).getPrefixes()) {
				validateAreDescendants(trie.prefixMap(prefix).values());

				trie.put(prefix, this);
			}
		}


		return trie;
	}

	private void validateAreDescendants(Collection<ConceptTreeNode<?>> values) {
		for (ConceptTreeNode<?> node : values) {
			if (!isDescendant(node)) {
				continue;
			}

			throw new TreeValidationException(this, node);
		}
	}
}

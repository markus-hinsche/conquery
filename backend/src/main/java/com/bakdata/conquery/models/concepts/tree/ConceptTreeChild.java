package com.bakdata.conquery.models.concepts.tree;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.models.concepts.ConceptElement;
import com.bakdata.conquery.models.concepts.conditions.ConceptTreeCondition;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptTreeChildId;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.Trie;
import org.apache.commons.collections4.TrieUtils;
import org.apache.commons.collections4.trie.PatriciaTrie;

@Getter
@Setter
public class ConceptTreeChild extends ConceptElement<ConceptTreeChildId> implements ConceptTreeNode<ConceptTreeChildId> {

	@JsonIgnore
	private transient int[] prefix;

	@JsonManagedReference @Valid
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

	@JsonIgnore
	public Trie<String, String> getPrefixTree() {
		if(condition == null){
			return new PatriciaTrie<>();
		}

		//TODO implement this
		return null;
	}
}

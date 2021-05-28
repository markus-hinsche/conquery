package com.bakdata.conquery.models.datasets;

import java.io.ObjectInputStream;
import java.util.Objects;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.jackson.InternalOnly;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.events.stores.root.ColumnStore;
import com.bakdata.conquery.models.identifiable.Labeled;
import com.bakdata.conquery.models.identifiable.ids.NamespacedIdentifiable;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.dropwizard.validation.ValidationMethod;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class Column extends Labeled<ColumnId> implements NamespacedIdentifiable<ColumnId> {

	public static final int UNKNOWN_POSITION = -1;

	@JsonBackReference
	@NotNull
	@ToString.Exclude
	private Table table;
	@NotNull
	private MajorTypeId type;

	@InternalOnly
	private int position = UNKNOWN_POSITION;
	/**
	 * if set this column should use the given dictionary
	 * if it is of type string, instead of its own dictionary
	 */
	private String sharedDictionary;
	/**
	 * if this is set this column counts as the secondary id of the given name for this
	 * table
	 */
	@NsIdRef
	private SecondaryIdDescription secondaryId;

	@Override
	public ColumnId createId() {
		return new ColumnId(table.getId(), getName());
	}

	//TODO try to remove this method methods, they are quite leaky
	public ColumnStore getTypeFor(Import imp) {
		if (!imp.getTable().equals(getTable())) {
			throw new IllegalArgumentException(String.format("Import %s is not for same table as %s", imp.getTable().getId(), getTable().getId()));
		}

		return Objects.requireNonNull(imp.getColumns()[getPosition()].getTypeDescription(), () -> "No description for Column/Import " + getId() + "/" + imp.getId());
	}

	@Override
	public String toString() {
		return String.format("Column[%s](type = %s)", getId(), getType());
	}

	@ValidationMethod(message = "Only STRING columns can be part of shared Dictionaries.")
	@JsonIgnore
	public boolean isSharedString() {
		return sharedDictionary == null || type.equals(MajorTypeId.STRING);
	}

	@JsonIgnore
	@Override
	public Dataset getDataset() {
		return table.getDataset();
	}
}

package com.bakdata.conquery.models.datasets;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.io.storage.NamespacedStorage;
import com.bakdata.conquery.models.identifiable.Labeled;
import com.bakdata.conquery.models.identifiable.ids.NamespacedIdentifiable;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import io.dropwizard.validation.ValidationMethod;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@Slf4j
public class Table extends Labeled<TableId>  implements NamespacedIdentifiable<TableId> {

	// TODO: 10.01.2020 fk: register imports here?

	@NsIdRef
	@NonNull @Nullable// may be null at load time, but cannot be set to null
	private Dataset dataset;
	@NotNull @Valid @JsonManagedReference
	private Column[] columns = new Column[0];

	@ValidationMethod(message = "More than one column map to the same secondaryId")
	@JsonIgnore
	public boolean isDistinctSecondaryIds() {
		Set<SecondaryIdDescription> secondaryIds = new HashSet<>();
		for (Column column : columns) {
			SecondaryIdDescription secondaryId = column.getSecondaryId();
			if (secondaryId != null && !secondaryIds.add(secondaryId)) {
				log.error("{} is duplicated", secondaryId);
				return false;
			}
		}
		return true;
	}

	@Override
	public TableId createId() {
		return new TableId(dataset.getId(), getName());
	}

	public Stream<Import> findImports(NamespacedStorage storage) {
		return storage
					   .getAllImports()
					   .stream()
					   .filter(imp -> imp.getTable().equals(this));
	}
}

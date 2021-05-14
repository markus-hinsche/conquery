package com.bakdata.conquery.models.identifiable.ids.specific;

import java.util.List;
import java.util.UUID;

import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.ids.AId;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.IdIterator;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@AllArgsConstructor @Getter @EqualsAndHashCode(callSuper=false)
@Schema(implementation = String.class, name = "ManagedExecutionId")
public class ManagedExecutionId extends AId<ManagedExecution<?>> {

	private final DatasetId dataset;
	private final UUID execution;
	
	@Override
	public void collectComponents(List<Object> components) {
		dataset.collectComponents(components);
		components.add(execution);
	}
	
	public static enum Parser implements Id.Parser<ManagedExecutionId> {
		INSTANCE;
		
		@Override
		public ManagedExecutionId parseInternally(IdIterator parts) {
			UUID query = UUID.fromString(parts.next());
			return new ManagedExecutionId(DatasetId.Parser.INSTANCE.parse(parts), query);
		}
	}
}

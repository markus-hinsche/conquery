package com.bakdata.conquery.models.execution;

import java.net.URL;
import java.time.ZonedDateTime;

import javax.annotation.Nullable;

import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.SecondaryIdDescriptionId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;

@NoArgsConstructor
@ToString
@Data
@FieldNameConstants
public abstract class ExecutionStatus {

	@Schema(required = false)
	private String[] tags;
	private String label;
	@JsonProperty("isPristineLabel")
	private boolean isPristineLabel;
	private ZonedDateTime createdAt;
	@Nullable
	private ZonedDateTime lastUsed;
	private UserId owner;
	private String ownerName;
	private boolean shared;
	private boolean own;
	private boolean system;

	private ManagedExecutionId id;
	private ExecutionState status;
	@Nullable
	private Long numberOfResults;

	@Nullable
	private Long requiredTime;

	private String queryType;

	@Nullable
	private SecondaryIdDescriptionId secondaryId;


	/**
	 * The url under from which the result of the execution can be downloaded as soon as it finished successfully.
	 */
	private URL resultUrl;


}

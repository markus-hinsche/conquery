package com.bakdata.conquery.models.datasets.concepts.filters;

import com.bakdata.conquery.apiv1.frontend.FEFilterConfiguration;
import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.models.identifiable.Labeled;
import com.bakdata.conquery.models.identifiable.ids.NamespacedIdentifiable;
import com.bakdata.conquery.models.identifiable.ids.specific.FilterId;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.dropwizard.validation.ValidationMethod;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * This class is the abstract superclass for all filters.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
@CPSBase
@Slf4j
public abstract class Filter<FILTER_VALUE> extends Labeled<FilterId> implements NamespacedIdentifiable<FilterId> {

	private String unit;
	@JsonAlias("description")
	private String tooltip;
	@JsonBackReference
	private Connector connector;
	private String pattern;
	private Boolean allowDropFile;

	private FILTER_VALUE defaultValue;

	@JsonIgnore
	@Override
	public Dataset getDataset() {
		return getConnector().getDataset();
	}

	public FEFilterConfiguration.Top createFrontendConfig() throws ConceptConfigurationException {
		FEFilterConfiguration.Top f = FEFilterConfiguration.Top.builder()
															   .id(getId())
															   .label(getLabel())
															   .tooltip(getTooltip())
															   .unit(getUnit())
															   .allowDropFile(getAllowDropFile())
															   .pattern(getPattern())
															   .defaultValue(getDefaultValue())
															   .build();
		configureFrontend(f);
		return f;
	}

	protected abstract void configureFrontend(FEFilterConfiguration.Top f) throws ConceptConfigurationException;

	@JsonIgnore
	public abstract Column[] getRequiredColumns();

	public abstract FilterNode<?> createFilterNode(FILTER_VALUE filterValue);

	@Override
	public FilterId createId() {
		return new FilterId(connector.getId(), getName());
	}

	@JsonIgnore
	@ValidationMethod(message = "Not all Filters are for Connector's table.")
	public boolean isForConnectorsTable() {
		boolean valid = true;

		for (Column column : getRequiredColumns()) {
			if (column == null || column.getTable() == connector.getTable()) {
				continue;
			}

			log.error("Filter[{}] of Table[{}] is not of Connector[{}]#Table[{}]", getId(), column.getTable().getId(), connector.getId(), connector.getTable().getId());

			valid = false;
		}

		return valid;
	}
}

package com.bakdata.conquery.models.query.concept;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import c10n.C10N;
import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.apiv1.QueryDescription;
import com.bakdata.conquery.apiv1.forms.Form;
import com.bakdata.conquery.internationalization.TableExportFormC10n;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.i18n.I18n;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.query.IQuery;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.concept.filter.CQTable;
import com.bakdata.conquery.models.query.concept.filter.CQUnfilteredTable;
import com.bakdata.conquery.models.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.worker.DatasetRegistry;

@CPSType(base = QueryDescription.class, id = "TABLE_EXPORT_FORM")
public class TableExportForm implements Form {

	@Valid
	@NotNull
	protected IQuery queryGroup;

	@NotNull
	private Range<LocalDate> dateRange = Range.all();

	@NotEmpty
	@Valid
	private List<CQConcept> concepts;

	@Override
	public Set<ManagedExecutionId> collectRequiredQueries() {
		return queryGroup.collectRequiredQueries();
	}

	@Override
	public void resolve(QueryResolveContext context) {
		queryGroup.resolve(context);
	}

	@Override
	public Map<String, List<ManagedQuery>> createSubQueries(DatasetRegistry datasets, UserId userId, DatasetId submittedDataset) {

		if(dateRange.isAll()){
			throw new IllegalArgumentException("We don't allow open exports.");
		}

		final List<CQUnfilteredTable> tables = new ArrayList<>();

		// Extract necessary info from query.
		for (CQConcept concept : this.concepts) {
			for (CQTable table : concept.getTables()) {

				tables.add(new CQUnfilteredTable(
						table.getId(),
						table.getDateColumn()
				));
			}
		}


		final TableExportQuery exportQuery = new TableExportQuery(queryGroup);
		exportQuery.setDateRange(dateRange);
		exportQuery.setTables(tables);

		return Map.of(ConqueryConstants.SINGLE_RESULT_TABLE_NAME, List.of(exportQuery.toManagedExecution(datasets, userId, submittedDataset)));
	}

	@Override
	public String getLocalizedTypeLabel() {
		return C10N.get(TableExportFormC10n.class, I18n.LOCALE.get()).getType();
	}

	@Override
	public void visit(Consumer<Visitable> visitor) {
		visitor.accept(this);
		queryGroup.visit(visitor);
	}
}

package com.bakdata.conquery.apiv1.forms;

import java.util.List;
import java.util.Map;

import com.bakdata.conquery.apiv1.SubmittedQuery;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.worker.Namespaces;
import lombok.Getter;
import lombok.Setter;

/**
 * API representation of a form query.
 */
@Getter
@Setter
public abstract class Form implements SubmittedQuery {
	
	public abstract void init(Namespaces namespaces, User user);
	

	public abstract Map<String, List<ManagedQuery>> createSubQueries(Namespaces namespaces, UserId userId, DatasetId submittedDataset);
	
}

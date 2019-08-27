package com.bakdata.eva;

import java.util.Set;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.MetaIdRef;
import com.bakdata.conquery.io.jackson.serializer.MetaIdRefCollection;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.io.jackson.serializer.NsIdRefCollection;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.concepts.conditions.CTCondition;
import com.bakdata.conquery.models.concepts.filters.Filter;
import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.preproc.outputs.Output;
import com.bakdata.conquery.models.query.IQuery;
import com.bakdata.conquery.models.query.concept.CQElement;
import com.bakdata.conquery.models.query.concept.filter.FilterValue;
import com.bakdata.eva.model.Base;
import com.bakdata.eva.model.Group;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class Constants {
	public static final Group[] GROUPS = {
		Group.builder().name("Concept JSONs")
			.base(new Base(Concept.class, ""))
			.base(new Base(CTCondition.class, "a condition in a Concept"))
			.base(new Base(Filter.class, ""))
			.base(new Base(Select.class, "used to define selects that can be used to create additional CSV columns"))
			.build(),
		Group.builder().name("Import JSONs")
			.base(new Base(Output.class, ""))
			.build(),
		Group.builder().name("API JSONs")
			.base(new Base(IQuery.class, ""))
			.base(new Base(CQElement.class, ""))
			.base(new Base(FilterValue.class, ""))
			.build()
	};
	
	public static final String JSON_CREATOR = JsonCreator.class.getName();
	public static final String CPS_TYPE = CPSType.class.getName();
	public static final Set<String> ID_REF = Set.of(NsIdRef.class.getName(), MetaIdRef.class.getName());
	public static final Set<String> ID_REF_COL = Set.of(NsIdRefCollection.class.getName(), MetaIdRefCollection.class.getName());
	public static final String JSON_IGNORE = JsonIgnore.class.getName();
	public static final String JSON_BACK_REFERENCE = JsonBackReference.class.getName();
}

package com.bakdata.conquery.apiv1;

import org.glassfish.hk2.utilities.binding.AbstractBinder;

import com.bakdata.conquery.commands.MasterCommand;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jersey.IdParamConverter;
import com.bakdata.conquery.io.jetty.CORSResponseFilter;
import com.bakdata.conquery.models.worker.Namespaces;
import com.bakdata.conquery.resources.ResourcesProvider;
import com.bakdata.conquery.resources.api.APIResource;
import com.bakdata.conquery.resources.api.ConceptElementResource;
import com.bakdata.conquery.resources.api.ConceptResource;
import com.bakdata.conquery.resources.api.ConceptsProcessor;
import com.bakdata.conquery.resources.api.DatasetResource;
import com.bakdata.conquery.resources.api.FilterResource;

import io.dropwizard.jersey.setup.JerseyEnvironment;

@CPSType(base = ResourcesProvider.class, id = "ApiV1")
public class ApiV1 implements ResourcesProvider {

	@Override
	public void registerResources(MasterCommand master) {
		Namespaces namespaces = master.getNamespaces();
		JerseyEnvironment environment = master.getEnvironment().jersey();

		environment.register(master.getAuthDynamicFeature());
		environment.register(new QueryResource(namespaces, master.getStorage()));
		environment.register(new ResultCSVResource(namespaces, master.getConfig()));
		environment.register(new StoredQueriesResource(namespaces));
		environment.register(IdParamConverter.Provider.INSTANCE);
		environment.register(CORSResponseFilter.class);
		environment.register(new ConfigResource(master.getConfig()));
		
		//inject required services
		environment.register(new AbstractBinder() {
			@Override
			protected void configure() {
				bind(new ConceptsProcessor(master.getNamespaces())).to(ConceptsProcessor.class);
			}
		});
		environment.register(APIResource.class);
		environment.register(ConceptElementResource.class);
		environment.register(ConceptResource.class);
		environment.register(DatasetResource.class);
		environment.register(FilterResource.class);
	}
}

package com.bakdata.conquery.models.config;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import io.dropwizard.Configuration;
import io.dropwizard.server.DefaultServerFactory;
import io.dropwizard.server.ServerFactory;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ConqueryConfig extends Configuration {
	
	@Getter
	private static ConqueryConfig instance;
	
	@Valid @NotNull
	private ClusterConfig cluster = new ClusterConfig();
	@Valid @NotNull //TODO remove
	private ShardConfig shardConfig = new ShardConfig();
	@Valid @NotNull
	private PreprocessingConfig preprocessor = new PreprocessingConfig();
	@Valid @NotNull
	private CSVConfig csv = new CSVConfig();
	@Valid @NotNull
	private LocaleConfig locale = new LocaleConfig();
	@Valid @NotNull
	private StandaloneConfig standalone = new StandaloneConfig();
	@Valid @NotNull
	private StorageConfig storage = new StorageConfig();
	@Valid @NotNull
	private QueryConfig queries = new QueryConfig();
	/**
	 * null means here that we try to deduce from an attached agent
	 */
	private Boolean debugMode = null;

	//this is needed to force start the REST backend on /api/
	public ConqueryConfig() {
		((DefaultServerFactory)this.getServerFactory()).setJerseyRootPath("/api/");
		ConqueryConfig.instance = this;
	}
	
	@Override
	public void setServerFactory(ServerFactory factory) {
		super.setServerFactory(factory);
		((DefaultServerFactory)this.getServerFactory()).setJerseyRootPath("/api/");
	}
}

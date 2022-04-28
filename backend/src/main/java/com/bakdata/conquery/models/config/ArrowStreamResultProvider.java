package com.bakdata.conquery.models.config;

import static com.bakdata.conquery.io.result.arrow.ResultArrowProcessor.getArrowResult;
import static com.bakdata.conquery.resources.ResourceConstants.FILE_EXTENTION_ARROW_STREAM;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.result.ResultRender.ResultRendererProvider;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.query.SingleTableResult;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.resources.api.ResultArrowStreamResource;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import lombok.Data;
import lombok.SneakyThrows;
import org.apache.arrow.vector.dictionary.DictionaryProvider;
import org.apache.arrow.vector.ipc.ArrowStreamWriter;

@Data
@CPSType(base = ResultRendererProvider.class, id = "ARROW_STREAM")
public class ArrowStreamResultProvider implements ResultRendererProvider {

	public static final MediaType MEDIA_TYPE = new MediaType("application", "vnd.apache.arrow.stream");
	@JsonIgnore
	private DatasetRegistry datasetRegistry;
	@JsonIgnore
	private ConqueryConfig config;

	private boolean hidden = true;

	@Override
	@SneakyThrows(MalformedURLException.class)
	public Collection<URL> generateResultURLs(ManagedExecution<?> exec, UriBuilder uriBuilder, boolean allProviders) {
		if (!(exec instanceof SingleTableResult)) {
			return Collections.emptyList();
		}

		if (hidden && !allProviders) {
			return Collections.emptyList();
		}

		return List.of(ResultArrowStreamResource.getDownloadURL(uriBuilder, exec));
	}

	@Override
	public Response createResult(Subject subject, ManagedExecution<?> exec, Dataset dataset, boolean pretty, Charset charset, Runnable onClose) {
		return getArrowResult(
				(output) -> (root) -> new ArrowStreamWriter(root, new DictionaryProvider.MapDictionaryProvider(), output),
				subject,
				((ManagedExecution<?> & SingleTableResult) exec),
				dataset, // TODO pull dataset up
				datasetRegistry,
				pretty,
				FILE_EXTENTION_ARROW_STREAM,
				MEDIA_TYPE,
				config,
				onClose
		);
	}

	@Override
	public void registerResultResource(JerseyEnvironment environment, ManagerNode manager) {
		setConfig(manager.getConfig());
		setDatasetRegistry(manager.getDatasetRegistry());

		environment.register(this);

		environment.register(ResultArrowStreamResource.class);
	}
}

package com.bakdata.conquery.io.result;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.query.SingleTableResult;
import com.bakdata.conquery.util.io.FileUtil;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Slf4j
public class ResultUtil {

	public static enum ContentDispositionOption {
		// Try to display payload in the browser
		INLINE,
		// Force download of the payload by the browser
		ATTACHMENT;

		String getHeaderValue() {
			return this.name().toLowerCase(Locale.ROOT);
		}
	}


	public static Response makeResponseWithFileName(StreamingOutput out, String label, String fileExtension, MediaType mediaType, ContentDispositionOption disposition) {
		Response.ResponseBuilder response = Response.ok(out);
		response.header(HttpHeaders.CONTENT_TYPE, mediaType);
		if (!(Strings.isNullOrEmpty(label) || label.isBlank())) {
			// Set filename from label if the label was set, otherwise the browser will name the file according to the request path
			response.header("Content-Disposition", String.format(
					"%s; filename=\"%s\"", disposition.getHeaderValue(), FileUtil.makeSafeFileName(label, fileExtension)));
		}
		return response.build();
	}

	/**
	 * Tries to determine the charset for the result encoding from different request properties.
	 * Defaults to StandardCharsets.UTF_8.
	 */
	public static Charset determineCharset(String userAgent, String queryCharset) {
		if (queryCharset != null) {
			try {
				return Charset.forName(queryCharset);
			}
			catch (Exception e) {
				log.warn("Unable to map '{}' to a charset. Defaulting to UTF-8", queryCharset);
			}
		}
		if (userAgent != null && userAgent.toLowerCase().contains("windows")) {
			return StandardCharsets.ISO_8859_1;
		}
		return StandardCharsets.UTF_8;
	}


	/**
	 * Throws a "Bad Request" response if the execution result is not a single table.
	 *
	 * @param exec the execution to test
	 */
	public static void checkSingleTableResult(ManagedExecution<?> exec) {
		if (!(exec instanceof SingleTableResult)) {
			throw new BadRequestException("Execution cannot be rendered as the requested format");
		}
	}

}

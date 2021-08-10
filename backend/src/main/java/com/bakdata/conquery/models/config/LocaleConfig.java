package com.bakdata.conquery.models.config;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class LocaleConfig {
	@NotNull
	private Locale frontend = Locale.ROOT;

	@NotNull
	@JsonDeserialize(contentUsing = DateTimeFormatterDeserializer.class)
	private Map<Locale, DateTimeFormatter> dateFormatMapping = Collections.emptyMap();

	public static class DateTimeFormatterDeserializer extends JsonDeserializer<DateTimeFormatter> {

		@Override
		public DateTimeFormatter deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
			return DateTimeFormatter.ofPattern(p.getText());
		}
	}

	/**
	 * Finds the best formatter according to the locale and mapped date formatters.
	 * If there is no perfect match, the locale is abstracted, see findClosestMatch.
	 * @param locale
	 * @return
	 */
	public DateTimeFormatter findDateTimeFormater(Locale locale) {
		final Locale closestMatch = findClosestMatch(locale);
		return closestMatch != null ? dateFormatMapping.get(closestMatch) : DateTimeFormatter.ISO_DATE;
	}

	/**
	 * Adapted from {@link c10n.share.DefaultLocaleMapping}
	 */
	public Locale findClosestMatch(Locale forLocale) {
		Set<Locale> fromSet = dateFormatMapping.keySet();
		String variant = forLocale.getDisplayVariant();
		String country = forLocale.getCountry();
		String language = forLocale.getLanguage();
		List<Locale> c = new ArrayList<>(4);
		if (null != variant && !variant.isEmpty()) {
			c.add(forLocale);
		}
		if (null != country && !country.isEmpty()) {
			c.add(new Locale(language, country));
		}
		if (null != language && !language.isEmpty()) {
			c.add(new Locale(language));
		}
		c.add(Locale.ROOT);
		for (Locale candidateLocale : c) {
			if (fromSet.contains(candidateLocale)) {
				return candidateLocale;
			}
		}
		return null;
	}
}

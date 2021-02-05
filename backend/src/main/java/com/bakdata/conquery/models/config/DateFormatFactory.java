package com.bakdata.conquery.models.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.*;

@Data
public class DateFormatFactory {


    private static final LocalDate ERROR_DATE = LocalDate.MIN;

    @NotNull
    private List<String> dateFormats = List.of(
            "yyyy-MM-dd", "yyyyMMdd", "dd.MM.yyyy"
    );

    /**
     * All available formats for parsing.
     */
    @JsonIgnore
    private Set<DateTimeFormatter> formats;



    /**
     * Last successfully parsed dateformat.
     */
    @JsonIgnore
    private static ThreadLocal<DateTimeFormatter> lastFormat = new ThreadLocal<>();

    /**
     * Parsed values cache.
     */
    @JsonIgnore
    private static LoadingCache<String, LocalDate> DATE_CACHE;

    /**
     * Lazy-initialize all formatters. Load additional formatters via ConqueryConfig.
     */
    public void initializeFormatters() {
        final Set<DateTimeFormatter> formatters = new HashSet<>();

        for (String p : dateFormats) {
            formatters.add(createFormatter(p));
        }

        this.formats = Collections.unmodifiableSet(formatters);

        DATE_CACHE = CacheBuilder.newBuilder()
                .softValues()
                .concurrencyLevel(10)
                .initialCapacity(64000)
                .build(CacheLoader.from(this::tryParse));
    }


    private static DateTimeFormatter createFormatter(String pattern) {
        return new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(pattern).toFormatter(Locale.US);
    }

    /**
     * Try and parse with the last successful parser. If not successful try and parse with other parsers and update the last successful parser.
     *
     * Method is private as it is only directly accessed via the Cache.
     */
    private LocalDate tryParse(String value) {

        final DateTimeFormatter formatter = lastFormat.get();

        if (formatter != null) {
            try {
                return LocalDate.parse(value, formatter);
            } catch (DateTimeParseException e) {
                //intentionally left blank
            }
        }

        for (DateTimeFormatter format : formats) {
            if (formatter != format) {
                try {
                    LocalDate res = LocalDate.parse(value, format);
                    lastFormat.set(format);
                    return res;
                } catch (DateTimeParseException e) {
                    //intentionally left blank
                }
            }
        }
        return ERROR_DATE;
    }
}

package com.portfolio.framework.utils;

import com.opencsv.bean.CsvToBeanBuilder;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Generic CSV-to-POJO loader for test data files under src/test/resources/testdata.
 *
 * <p>Centralising this in one generic method (rather than one bespoke reader
 * per data file) means adding a new data-driven suite is just: define a POJO
 * with {@code @CsvBindByName} fields, drop a CSV next to the existing ones,
 * and call {@code load("new_file.csv", NewPojo.class)}.</p>
 */
public final class CsvTestDataReader {

    private CsvTestDataReader() {
    }

    public static <T> List<T> load(String classpathResourceName, Class<T> type) {
        String resourcePath = "testdata/" + classpathResourceName;
        try (Reader reader = new InputStreamReader(
                requireResourceStream(resourcePath), StandardCharsets.UTF_8)) {
            return new CsvToBeanBuilder<T>(reader)
                    .withType(type)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build()
                    .parse();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read test data file: " + resourcePath, e);
        }
    }

    private static java.io.InputStream requireResourceStream(String resourcePath) {
        java.io.InputStream stream = CsvTestDataReader.class.getClassLoader().getResourceAsStream(resourcePath);
        if (stream == null) {
            throw new IllegalStateException("Test data file not found on classpath: " + resourcePath);
        }
        return stream;
    }
}

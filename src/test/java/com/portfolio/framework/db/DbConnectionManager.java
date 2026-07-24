package com.portfolio.framework.db;

import com.portfolio.framework.config.ConfigReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Owns the JDBC connection lifecycle and one-time schema setup for the local
 * H2 audit database.
 *
 * <p>H2 in file mode ({@code jdbc:h2:file:...}) was chosen over in-memory
 * mode deliberately: an in-memory DB is wiped the instant the JVM exits,
 * which would make it impossible to inspect the audit trail after a CI run.
 * File mode persists {@code target/testdb/*.mv.db} as a build artifact,
 * so "prove the record was written" survives past the test process —
 * exactly what a real DB validation step needs to demonstrate.</p>
 */
public final class DbConnectionManager {

    private static final Logger log = LoggerFactory.getLogger(DbConnectionManager.class);
    private static volatile boolean schemaInitialised = false;

    private DbConnectionManager() {
    }

    public static Connection getConnection() {
        try {
            Class.forName(ConfigReader.get("db.driver"));
            Connection connection = DriverManager.getConnection(
                    ConfigReader.get("db.url"),
                    ConfigReader.get("db.username"),
                    ConfigReader.get("db.password"));
            ensureSchema(connection);
            return connection;
        } catch (ClassNotFoundException | SQLException e) {
            throw new IllegalStateException("Unable to obtain H2 connection at " + ConfigReader.get("db.url"), e);
        }
    }

    /**
     * Applies schema.sql exactly once per JVM (guarded by a volatile flag —
     * this class is only ever driven by single-threaded DB tests, so a full
     * lock isn't warranted, but the flag still avoids redundant DDL on every
     * connection acquisition within one run).
     */
    private static void ensureSchema(Connection connection) {
        if (schemaInitialised) {
            return;
        }
        synchronized (DbConnectionManager.class) {
            if (schemaInitialised) {
                return;
            }
            try (Statement statement = connection.createStatement()) {
                statement.execute(readSchemaSql());
                schemaInitialised = true;
                log.info("H2 audit schema verified/created");
            } catch (SQLException e) {
                throw new IllegalStateException("Failed to apply schema.sql to H2 audit database", e);
            }
        }
    }

    private static String readSchemaSql() {
        try (InputStream input = DbConnectionManager.class.getClassLoader()
                .getResourceAsStream("db/schema.sql")) {
            if (input == null) {
                throw new IllegalStateException("db/schema.sql not found on test classpath");
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}

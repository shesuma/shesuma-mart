
package com.shesuma.shesumamart.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.h2.tools.RunScript;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;

public final class Database {

    private static HikariDataSource dataSource;

    private Database() {
    }

    public static synchronized void initialize() {
        if (dataSource != null && !dataSource.isClosed()) {
            return;
        }

        try {
            Class.forName("org.h2.Driver");

            String projectPath = System.getProperty("shesumamart.data.dir");

            Path dataDirectory = (projectPath == null || projectPath.isBlank())
                    ? Paths.get(System.getProperty("user.home"),
                                "ShesumaMartData")
                    : Paths.get(projectPath);

            java.nio.file.Files.createDirectories(dataDirectory);

            Path databasePath = dataDirectory.resolve("shesumamart")
                    .toAbsolutePath()
                    .normalize();

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:h2:" + databasePath);
            config.setUsername("sa");
            config.setPassword("");
            config.setMaximumPoolSize(10);

            dataSource = new HikariDataSource(config);

            try (
                Connection connection = dataSource.getConnection();
                InputStream inputStream =
                    Database.class.getClassLoader()
                        .getResourceAsStream("schema.sql")
            ) {
                if (inputStream == null) {
                    throw new IllegalStateException(
                            "schema.sql not found");
                }

                RunScript.execute(
                    connection,
                    new InputStreamReader(
                        inputStream,
                        StandardCharsets.UTF_8
                    )
                );
            }

        } catch (Exception e) {
            shutdown();
            throw new IllegalStateException(
                    "Database initialization failed", e);
        }
    }

    public static synchronized HikariDataSource getDataSource() {
        if (dataSource == null || dataSource.isClosed()) {
            throw new IllegalStateException(
                    "Database is not initialized");
        }
        return dataSource;
    }

    public static synchronized void shutdown() {
        if (dataSource != null) {
            dataSource.close();
            dataSource = null;
        }
    }
}
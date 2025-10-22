package dev.zeann3th.stresspilot.config;

import dev.zeann3th.stresspilot.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Statement;

@Slf4j
@Configuration
@SuppressWarnings("all")
public class DataSourceConfig {
    @Bean
    public DataSource dataSource() {
        try {
            String appHome = System.getenv("PILOT_HOME");
            if (appHome == null || appHome.isEmpty()) {
                log.warn("[Database Migrator] PILOT_HOME not set, defaulting to user home directory");
                appHome = System.getProperty("user.home") + "/" + Constants.APP_DIR;
            }

            Path appDir = Paths.get(appHome);
            Path dbPath = appDir.resolve(Constants.DB_FILE_NAME);
            Files.createDirectories(appDir);
            boolean dbExists = Files.exists(dbPath);

            if (!dbExists) {
                log.info("[Database Migrator] Database file not found, creating new database at {}", dbPath);
                Files.createFile(dbPath);
            } else {
                log.info("[Database Migrator] Database file exists, skipping initiation");
            }

            String jdbcUrl = "jdbc:sqlite:" + dbPath.toAbsolutePath();
            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setDriverClassName("org.sqlite.JDBC");
            dataSource.setUrl(jdbcUrl);

            if (!dbExists) {
                try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
                    // do smt
                }
            }

            log.info("[Database Migrator] Data source configured with URL: {}", jdbcUrl);
            return dataSource;
        } catch (Exception e) {
            throw new RuntimeException("[Database Migrator] Failed to configure data source", e);
        }
    }
}

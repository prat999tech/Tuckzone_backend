package com.school.canteen.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.net.URI;

/**
 * Production Database Configuration.
 * Automatically parses any URL format (including Render, Railway, Neon, Supabase URLs
 * with embedded username:password@) and converts it to valid PostgreSQL JDBC format.
 */
@Configuration
@Profile("prod")
public class DatabaseConfig {

    @Value("${spring.datasource.url:${JDBC_DATABASE_URL:${DATABASE_URL:jdbc:postgresql://localhost:5432/canteen}}}")
    private String rawUrl;

    @Value("${spring.datasource.username:${SPRING_DATASOURCE_USERNAME:${PGUSER:}}}")
    private String defaultUser;

    @Value("${spring.datasource.password:${SPRING_DATASOURCE_PASSWORD:${PGPASSWORD:}}}")
    private String defaultPassword;

    @Bean
    @Primary
    public DataSource dataSource() {
        String url = rawUrl;
        String user = defaultUser;
        String password = defaultPassword;

        if (url != null && !url.isBlank()) {
            String uriString = url.startsWith("jdbc:") ? url.substring(5) : url;
            if (uriString.startsWith("postgresql://") || uriString.startsWith("postgres://")) {
                try {
                    URI uri = new URI(uriString);
                    if (uri.getUserInfo() != null) {
                        String[] userInfo = uri.getUserInfo().split(":", 2);
                        user = userInfo[0];
                        if (userInfo.length > 1) {
                            password = userInfo[1];
                        }
                    }
                    int port = uri.getPort() == -1 ? 5432 : uri.getPort();
                    String path = uri.getPath() != null ? uri.getPath() : "/canteen";
                    String query = uri.getQuery() != null ? "?" + uri.getQuery() : "";
                    url = "jdbc:postgresql://" + uri.getHost() + ":" + port + path + query;
                } catch (Exception e) {
                    // Fallback to raw URL if parsing fails
                }
            }
        }

        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(url);
        if (user != null && !user.isBlank()) {
            dataSource.setUsername(user);
        }
        if (password != null && !password.isBlank()) {
            dataSource.setPassword(password);
        }
        dataSource.setDriverClassName("org.postgresql.Driver");
        return dataSource;
    }
}

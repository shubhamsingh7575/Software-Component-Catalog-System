package edu.software.project.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth")
public class AppProperties {
    private long sessionTtlHours = 24;

    public long getSessionTtlHours() {
        return sessionTtlHours;
    }

    public void setSessionTtlHours(long sessionTtlHours) {
        this.sessionTtlHours = sessionTtlHours;
    }
}

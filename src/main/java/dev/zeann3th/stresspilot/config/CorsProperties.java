package dev.zeann3th.stresspilot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "application.cors")
public class CorsProperties {
    private List<String> allowedOrigins;
}
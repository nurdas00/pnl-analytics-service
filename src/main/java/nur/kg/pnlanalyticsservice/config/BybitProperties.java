package nur.kg.pnlanalyticsservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bybit")
public record BybitProperties(
        String domain,
        String apiKey,
        String apiSecret
) {
}
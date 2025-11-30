package nur.kg.pnlanalyticsservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan(basePackages = "nur.kg.pnlanalyticsservice.config")
public class PnlAnalyticsServiceApplication {

    static void main(String[] args) {
        SpringApplication.run(PnlAnalyticsServiceApplication.class, args);
    }

}

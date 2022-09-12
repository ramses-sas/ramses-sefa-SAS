package it.polimi.saefa.analyze;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class AnalyzeApplication {
    public static void main(String[] args) {
        SpringApplication.run(AnalyzeApplication.class, args);
    }
}

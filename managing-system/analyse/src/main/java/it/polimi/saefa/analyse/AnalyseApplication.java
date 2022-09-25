package it.polimi.saefa.analyse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class AnalyseApplication {

    public static void main(String[] args) {
        SpringApplication.run(AnalyseApplication.class, args);
    }
}

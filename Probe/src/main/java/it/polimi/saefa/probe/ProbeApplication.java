package it.polimi.saefa.probe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class ProbeApplication {

        public static void main(String[] args) {
            SpringApplication.run(ProbeApplication.class, args);
        }

}

package it.polimi.saefa.knowledge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;


@SpringBootApplication
@EnableDiscoveryClient
public class KnowledgeApplication {

    public static void main(String[] args) throws Exception {
        SpringApplication.run(KnowledgeApplication.class, args);
    }

}

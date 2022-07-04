package polimi.saefa.eurekaservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@EnableEurekaServer
@SpringBootApplication
public class ServiceRegisrtyApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServiceRegisrtyApplication.class, args);
    }
}
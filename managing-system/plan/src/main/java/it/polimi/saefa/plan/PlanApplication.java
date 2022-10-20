package it.polimi.saefa.plan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class PlanApplication {

    public static void main(String[] args) throws Exception {
        SpringApplication.run(PlanApplication.class, args);
    }

}

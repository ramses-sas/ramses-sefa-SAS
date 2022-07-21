package polimi.saefa.orderingservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class OrderingServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrderingServiceApplication.class, args);
	}

}


package polimi.saefa.paymentproxyservice2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class PaymentProxyServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(PaymentProxyServiceApplication.class, args);
	}

}


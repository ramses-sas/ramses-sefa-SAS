package it.polimi.saefa.paymentproxy2service;

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


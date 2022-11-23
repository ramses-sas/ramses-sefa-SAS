package it.polimi.dummymanagedsystem.randintproducerservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class RandintProducerServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(RandintProducerServiceApplication.class, args);
	}

}


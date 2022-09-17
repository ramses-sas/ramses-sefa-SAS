package it.polimi.saefa.restclient;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class RestClientApplication implements CommandLineRunner {
	@Autowired
	RequestsGenerator requestsGenerator;

	public static void main(String[] args) {
		SpringApplication.run(RestClientApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		log.info("Simulating new order");
		requestsGenerator.simulateOrder();
		log.info("Exiting...");
	}
}


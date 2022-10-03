package it.polimi.saefa.restclient;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class RestClientApplication implements CommandLineRunner {
	@Autowired
	RequestsGenerator requestsGenerator;

	@Value("${NUMBER_OF_REQUESTS}")
	private int numberOfRequests;

	public static void main(String[] args) {
		SpringApplication.run(RestClientApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		log.info("Simulating new order");
		for (int i = 0; i < numberOfRequests; i++) {
			new Thread(() -> {
				requestsGenerator.simulateOrder();
			}).start();
			Thread.sleep(1000);
		}
		log.info("Exiting...");
	}
}


package it.polimi.sofa.instancesmanager;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class InstancesManagerApplication {

	public static void main(String[] args) {
		SpringApplication.run(InstancesManagerApplication.class, args);
	}

}


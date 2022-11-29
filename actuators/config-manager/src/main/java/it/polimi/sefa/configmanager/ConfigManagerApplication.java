package it.polimi.sefa.configmanager;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@Slf4j
@SpringBootApplication
public class ConfigManagerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ConfigManagerApplication.class, args);
	}

}


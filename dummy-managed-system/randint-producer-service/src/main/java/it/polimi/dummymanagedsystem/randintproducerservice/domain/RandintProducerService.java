package it.polimi.dummymanagedsystem.randintproducerservice.domain;

import org.springframework.stereotype.Service;

import java.util.*; 

@Service
public class RandintProducerService {
	public int generateRandomInt() {
		Random rand = new Random();
		return rand.nextInt(100);
	}
}


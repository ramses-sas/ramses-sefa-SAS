package it.polimi.dummymanagedsystem.bservice.domain;

import org.springframework.stereotype.Service;

import java.util.*; 

@Service
public class RandGeneratorService {
	public int generateRandomInt() {
		Random rand = new Random();
		return rand.nextInt(100);
	}
}


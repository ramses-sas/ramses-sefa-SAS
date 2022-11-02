package it.polimi.dummymanagedsystem.aservice.domain;

import it.polimi.dummymanagedsystem.aservice.externalinterfaces.BServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Slf4j
@Service
public class RandService {
	@Autowired
	private BServiceClient bServiceClient;

	public int getRandomNumber() {
		return bServiceClient.generateRandomInteger();
	}

}


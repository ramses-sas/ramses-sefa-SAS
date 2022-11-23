package it.polimi.dummymanagedsystem.randintvendorservice.domain;

import it.polimi.dummymanagedsystem.randintvendorservice.externalinterfaces.RandintProducerServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Slf4j
@Service
public class RandintVendorService {
	@Autowired
	private RandintProducerServiceClient randintProducerServiceClient;

	public int getRandomNumber() {
		return randintProducerServiceClient.generateRandomInteger();
	}

}


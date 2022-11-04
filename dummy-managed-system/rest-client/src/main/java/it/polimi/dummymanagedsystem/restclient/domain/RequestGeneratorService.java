package it.polimi.dummymanagedsystem.restclient.domain;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class RequestGeneratorService {
	@Value("${API_GATEWAY_IP_PORT}")
	private String apiGatewayUri;

	private String getApiGatewayUrl() {
		return "http://"+apiGatewayUri;
	}

	public Integer getRandomNumber() {
		String url = getApiGatewayUrl()+"/randomNumber";
		RestTemplate restTemplate = new RestTemplate();
		return restTemplate.getForObject(url, Integer.class);
	}

}


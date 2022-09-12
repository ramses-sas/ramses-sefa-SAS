package it.polimi.saefa.knowledge;

import it.polimi.saefa.knowledge.parser.AdaptationParametersParser;
import it.polimi.saefa.knowledge.parser.ConfigurationParser;
import it.polimi.saefa.knowledge.parser.SystemArchitectureParser;
import it.polimi.saefa.knowledge.persistence.KnowledgeService;
import it.polimi.saefa.knowledge.persistence.domain.adaptation.AdaptationParameter;
import it.polimi.saefa.knowledge.persistence.domain.architecture.Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;
import java.io.FileReader;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class KnowledgeInit implements CommandLineRunner {
    @Autowired
    private KnowledgeService knowledgeService;

    @Autowired
    private ConfigurationParser configurationParser;

    @Override
    public void run(String... args) throws Exception {
        FileReader architectureReader = new FileReader(ResourceUtils.getFile("classpath:system_architecture.json"));
        List<Service> serviceList = SystemArchitectureParser.parse(architectureReader);
        serviceList.forEach(knowledgeService::addService);
        serviceList.forEach(service -> {
            service.setConfiguration(configurationParser.parseProperties(service.getServiceId()));
            knowledgeService.addService(service);
        });
        configurationParser.parseGlobalProperties(knowledgeService.getServicesMap());

        FileReader adaptationParametersReader = new FileReader(ResourceUtils.getFile("classpath:adaptation_parameters_specification.json"));
        Map<String, List<AdaptationParameter>> servicesAdaptationParameters = AdaptationParametersParser.parse(adaptationParametersReader);
        for (String serviceName : servicesAdaptationParameters.keySet()) {
            knowledgeService.getService(serviceName).setAdaptationParameters(servicesAdaptationParameters.get(serviceName).toArray(AdaptationParameter[]::new));
        }
        for (Service service : serviceList) {
            log.debug(service.toString());
        }
        log.info("Knowledge initialized");
    }
}

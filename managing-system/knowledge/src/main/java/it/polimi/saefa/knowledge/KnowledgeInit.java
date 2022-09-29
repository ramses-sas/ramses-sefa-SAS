package it.polimi.saefa.knowledge;

import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Application;
import it.polimi.saefa.knowledge.parser.AdaptationParamParser;
import it.polimi.saefa.knowledge.parser.ConfigurationParser;
import it.polimi.saefa.knowledge.parser.SystemArchitectureParser;
import it.polimi.saefa.knowledge.parser.SystemBenchmarkParser;
import it.polimi.saefa.knowledge.persistence.KnowledgeService;
import it.polimi.saefa.knowledge.persistence.domain.adaptation.specifications.AdaptationParamSpecification;
import it.polimi.saefa.knowledge.persistence.domain.architecture.Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;
import java.io.FileReader;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class KnowledgeInit implements InitializingBean {
    @Autowired
    private KnowledgeService knowledgeService;
    @Autowired
    private ConfigurationParser configurationParser;
    @Autowired
    private EurekaClient discoveryClient;

    @Override
    public void afterPropertiesSet() throws Exception {
        FileReader architectureReader = new FileReader(ResourceUtils.getFile("classpath:system_architecture.json"));
        List<Service> serviceList = SystemArchitectureParser.parse(architectureReader);
        FileReader adaptationParametersReader = new FileReader(ResourceUtils.getFile("classpath:adaptation_parameters_specification.json"));
        Map<String, List<AdaptationParamSpecification>> servicesAdaptationParameters = AdaptationParamParser.parse(adaptationParametersReader);
        FileReader benchmarkReader = new FileReader(ResourceUtils.getFile("classpath:system_benchmarks.json"));
        Map<String, List<SystemBenchmarkParser.ServiceImplementationBenchmarks>> servicesBenchmarks = SystemBenchmarkParser.parse(benchmarkReader);

        serviceList.forEach(service -> {
            service.setConfiguration(configurationParser.parseProperties(service.getServiceId()));
            service.setAdaptationParameters(servicesAdaptationParameters.get(service.getServiceId()));
            knowledgeService.addService(service);
            servicesBenchmarks.get(service.getServiceId()).forEach(serviceImplementationBenchmarks -> {
                serviceImplementationBenchmarks.getAdaptationParametersBenchmarks().forEach((adaptationClass, value) ->
                        service.getPossibleImplementations()
                                .get(serviceImplementationBenchmarks.getServiceImplementationId())
                                .getAdaptationParamCollection()
                                .setBootBenchmark(adaptationClass, value));
            });
        });
        configurationParser.parseGlobalProperties(knowledgeService.getServicesMap());


        for (Service service : knowledgeService.getServicesMap().values()) {
            log.debug("Service: " + service.getServiceId());
            Application serviceApplication = discoveryClient.getApplication(service.getServiceId());
            if(serviceApplication!=null) {
                service.setCurrentImplementation(serviceApplication.getInstances().get(0).getInstanceId().split("@")[0]);
                log.debug(discoveryClient.getApplication(service.getServiceId()).getName());
            }
        }

        for (Service service : serviceList) {
            log.debug(service.toString());
        }
        log.info("Knowledge initialized");
    }
}

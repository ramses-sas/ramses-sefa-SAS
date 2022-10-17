package it.polimi.saefa.knowledge;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Application;
import it.polimi.saefa.knowledge.domain.architecture.Instance;
import it.polimi.saefa.knowledge.domain.architecture.InstanceStatus;
import it.polimi.saefa.knowledge.domain.architecture.ServiceConfiguration;
import it.polimi.saefa.knowledge.domain.persistence.ConfigurationRepository;
import it.polimi.saefa.knowledge.parser.QoSParser;
import it.polimi.saefa.knowledge.parser.ConfigurationParser;
import it.polimi.saefa.knowledge.parser.SystemArchitectureParser;
import it.polimi.saefa.knowledge.parser.SystemBenchmarkParser;
import it.polimi.saefa.knowledge.domain.KnowledgeService;
import it.polimi.saefa.knowledge.domain.adaptation.specifications.QoSSpecification;
import it.polimi.saefa.knowledge.domain.architecture.Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
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
    private ConfigurationRepository configurationRepository;
    @Autowired
    private EurekaClient discoveryClient;

    @Override
    public void afterPropertiesSet() throws Exception {
        FileReader architectureReader = new FileReader(ResourceUtils.getFile("classpath:system_architecture.json"));
        List<Service> serviceList = SystemArchitectureParser.parse(architectureReader);
        FileReader qoSReader = new FileReader(ResourceUtils.getFile("classpath:qos_specification.json"));
        Map<String, List<QoSSpecification>> servicesQoS = QoSParser.parse(qoSReader);
        FileReader benchmarkReader = new FileReader(ResourceUtils.getFile("classpath:system_benchmarks.json"));
        Map<String, List<SystemBenchmarkParser.ServiceImplementationBenchmarks>> servicesBenchmarks = SystemBenchmarkParser.parse(benchmarkReader);


        serviceList.forEach(service -> {
            Application serviceApplication = discoveryClient.getApplication(service.getServiceId());
            if (serviceApplication == null)
                throw new RuntimeException("Service " + service.getServiceId() + " not found in Eureka");
            List<InstanceInfo> instances = serviceApplication.getInstances();
            if (instances == null || instances.isEmpty())
                throw new RuntimeException("No instances found for service " + service.getServiceId());
            service.setCurrentImplementationId(instances.get(0).getInstanceId().split("@")[0]);
            service.setAllQoS(servicesQoS.get(service.getServiceId()));
            servicesBenchmarks.get(service.getServiceId()).forEach(serviceImplementationBenchmarks -> {
                serviceImplementationBenchmarks.getQoSBenchmarks().forEach((adaptationClass, value) ->
                        service.getPossibleImplementations()
                                .get(serviceImplementationBenchmarks.getServiceImplementationId())
                                .setBenchmark(adaptationClass, value));
            });
            instances.forEach(instanceInfo -> {
                if (!instanceInfo.getInstanceId().split("@")[0].equals(service.getCurrentImplementationId()))
                    throw new RuntimeException("Service " + service.getServiceId() + " has more than one running implementation");
                service.createInstance(instanceInfo.getInstanceId().split("@")[1]).setCurrentStatus(InstanceStatus.ACTIVE);
            });
            service.setConfiguration(configurationParser.parsePropertiesAndCreateConfiguration(service.getServiceId()));
            configurationRepository.save(service.getConfiguration());
            knowledgeService.addService(service);


        });
        configurationParser.parseGlobalProperties(knowledgeService.getServicesMap());

        for (Service service : serviceList) {
            ServiceConfiguration configuration = service.getConfiguration();
            if (configuration.getLoadBalancerType() != null && configuration.getLoadBalancerType().equals(ServiceConfiguration.LoadBalancerType.WEIGHTED_RANDOM)) {
                if (configuration.getLoadBalancerWeights() == null) {
                    for (Instance instance : service.getInstances())
                        configuration.addLoadBalancerWeight(instance.getInstanceId(), 1.0/service.getInstances().size());
                } else if (!configuration.getLoadBalancerWeights().keySet().equals(service.getCurrentImplementation().getInstances().keySet())) {
                    throw new RuntimeException("Service " + service.getServiceId() + " has a load balancer weights map with different keys than the current implementation instances");
                }
            }
        }



        for (Service service : serviceList) {
            log.debug(service.toString());
        }
        log.info("Knowledge initialized");
    }
}

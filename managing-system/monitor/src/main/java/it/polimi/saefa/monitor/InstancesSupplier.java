/*
package it.polimi.saefa.monitor;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Application;
import it.polimi.saefa.knowledge.KnowledgeInit;
import it.polimi.saefa.knowledge.domain.KnowledgeService;
import it.polimi.saefa.monitor.externalinterfaces.KnowledgeClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class InstancesSupplier {
    @Autowired
    private EurekaClient discoveryClient;

    @Autowired
    private KnowledgeClient knowledgeClient;

    public Map<String, List<InstanceInfo>> getServicesInstances() {
        // Get the managed services from the knowledge
        Set<String> serviceIdSet = knowledgeClient.getServicesMap().keySet();
        List<Application> applications = discoveryClient.getApplications().getRegisteredApplications();
        Map<String, List<InstanceInfo>> servicesInstances = new HashMap<>();
        applications.forEach(application -> {
            // If the service is managed get its instances from the discovery client
            if (serviceIdSet.contains(application.getName())) {
                List<InstanceInfo> applicationsInstances = application.getInstances();
                servicesInstances.put(application.getName(), applicationsInstances);
            }
        });
        return servicesInstances;
    }


}


*/

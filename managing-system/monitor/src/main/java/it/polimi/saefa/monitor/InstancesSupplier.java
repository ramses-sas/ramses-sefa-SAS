package it.polimi.saefa.monitor;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Application;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class InstancesSupplier {
    @Autowired
    private EurekaClient discoveryClient;

    public Map<String, List<InstanceInfo>> getServicesInstances() {
        List<Application> applications = discoveryClient.getApplications().getRegisteredApplications();
        Map<String, List<InstanceInfo>> servicesInstances = new HashMap<>();
        applications.forEach(application -> {
            if (application.getName().endsWith("-SERVICE")) {
                List<InstanceInfo> applicationsInstances = application.getInstances();
                servicesInstances.put(application.getName(), applicationsInstances);
            }
        });
        return servicesInstances;
    }

    public InstanceInfo getConfigServerInstance() {
        return discoveryClient.getApplication("CONFIG-SERVER").getInstances().get(0);
    }

}

package it.polimi.saefa.monitor;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Application;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class InstancesDiscoveryService {

    @Autowired
    private EurekaClient discoveryClient;


    public Map<String, List<InstanceInfo>> getServicesInstances(){
        List<Application> applications = discoveryClient.getApplications().getRegisteredApplications();
        Map<String, List<InstanceInfo>> servicesInstances = new HashMap<>();
        applications.forEach(application -> {
            List<InstanceInfo> applicationsInstances = application.getInstances();
            servicesInstances.put(application.getName(), applicationsInstances);
        });
        return servicesInstances;
    }

    public Map<String, List<String>> getServicesInstancesOLD(){
        List<Application> applications = discoveryClient.getApplications().getRegisteredApplications();
        Map<String, List<String>> servicesInstances = new HashMap<>();
        applications.forEach(application -> {
            List<InstanceInfo> applicationsInstances = application.getInstances();
            servicesInstances.put(application.getName(), applicationsInstances.stream().map(InstanceInfo::getHomePageUrl)
                /*.map(elem -> {
                    try {
                        return new URL(elem);
                    } catch (MalformedURLException e) {
                        throw new RuntimeException(e);
                    }
                })*/
                .toList());
        });
        return servicesInstances;
    }

    public String printInstances(){
        StringBuilder toReturn = new StringBuilder();

        Map<String, List<String>> servicesInstances = getServicesInstancesOLD();
        servicesInstances.forEach((serviceName, serviceInstances) -> {
            toReturn.append(serviceName).append(": ");
            serviceInstances.forEach(serviceInstance -> {
                toReturn.append(serviceInstance).append(" ");
            });
            toReturn.append("\n");
        } );
        return toReturn.toString();
    }

}

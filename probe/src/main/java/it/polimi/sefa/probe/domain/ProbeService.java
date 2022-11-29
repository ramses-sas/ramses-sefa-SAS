package it.polimi.sefa.probe.domain;

import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Application;
import it.polimi.sefa.probe.configuration.ConfigurationParser;
import it.polimi.sefa.probe.configuration.ServiceConfiguration;
import it.polimi.sefa.probe.domain.metrics.InstanceMetricsSnapshot;
import it.polimi.sefa.probe.prometheus.PrometheusParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@org.springframework.stereotype.Service
@Slf4j
public class ProbeService {
    @Autowired
    private EurekaClient discoveryClient;
    @Autowired
    private PrometheusParser prometheusParser;
    @Autowired
    private ConfigurationParser configurationParser;

    @Value("${INTERNET_CONNECTION_CHECK_HOST}")
    private String internetConnectionCheckHost;
    @Value("${INTERNET_CONNECTION_CHECK_PORT}")
    private int internetConnectionCheckPort;

    public List<InstanceMetricsSnapshot> createServiceSnapshot(String serviceId) {
        AtomicBoolean invalidIteration = new AtomicBoolean(false);
        final List<InstanceMetricsSnapshot> instanceMetricsSnapshots = new LinkedList<>();
        Application application = discoveryClient.getApplication(serviceId);
        if (application == null) {
            log.error("Service {} not found in Eureka", serviceId);
            return instanceMetricsSnapshots;
        }
        application.getInstances().forEach(instance -> {
            if (invalidIteration.get()) return;
            InstanceMetricsSnapshot instanceMetricsSnapshot;
            try {
                instanceMetricsSnapshot = prometheusParser.parse(instance);
                instanceMetricsSnapshot.applyTimestamp();
                log.debug("Adding metric for instance {}", instanceMetricsSnapshot.getInstanceId());
                instanceMetricsSnapshots.add(instanceMetricsSnapshot);
            } catch (Exception e) {
                log.warn("Error adding metrics for {}. Note that it might have been shutdown by the executor. Creating a snapshot with status UNREACHABLE", instance.getInstanceId());
                log.warn("The exception is: " + e.getMessage());
                instanceMetricsSnapshot = new InstanceMetricsSnapshot(instance.getAppName(), instance.getInstanceId());
                instanceMetricsSnapshot.setStatus(InstanceStatus.UNREACHABLE);
                instanceMetricsSnapshot.applyTimestamp();
                instanceMetricsSnapshots.add(instanceMetricsSnapshot);
                try {
                    if (!pingHost(internetConnectionCheckHost, internetConnectionCheckPort, 5000))
                        invalidIteration.set(true); //iteration is invalid if monitor cannot reach a known host
                } catch (Exception e1) {
                    log.error("Error checking internet connection");
                    log.error(e1.getMessage());
                    invalidIteration.set(true);
                }
            }
        });
        if (invalidIteration.get()) {
            log.error("Invalid iteration. Skipping service {}", serviceId);
            return null;
        }
        return instanceMetricsSnapshots;
    }

    public Map<String, ServiceInfo> getServices() {
        Map<String, ServiceInfo> serviceInfoList = new HashMap<>();
        discoveryClient.getApplications().getRegisteredApplications().forEach(application -> {
            ServiceInfo serviceInfo = getService(application);
            serviceInfoList.put(serviceInfo.getServiceId(), serviceInfo);
        });
        return serviceInfoList;
    }

    public ServiceConfiguration getServiceConfiguration(String serviceId, String currentImplementationId) {
        return configurationParser.parsePropertiesAndCreateConfiguration(serviceId, currentImplementationId);
    }

    private ServiceInfo getService(Application application) {
        ServiceInfo serviceInfo = new ServiceInfo(application.getName());
        application.getInstances().forEach(instance -> serviceInfo.addInstance(instance.getInstanceId()));
        serviceInfo.setCurrentImplementationId(application.getInstances().get(0).getInstanceId().split("@")[0]);
        return serviceInfo;
    }

    private ServiceInfo getService(String serviceId){
        Application application = discoveryClient.getApplication(serviceId);
        return getService(application);
    }


    private boolean pingHost(String host, int port, int timeout) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeout);
            return true;
        } catch (IOException e) {
            return false; // Either timeout or unreachable or failed DNS lookup.
        }
    }
}

package it.polimi.saefa.monitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@EnableDiscoveryClient
@SpringBootApplication
public class MonitorApplication {

    public static void main(String[] args) {
        SpringApplication.run(MonitorApplication.class, args); }
}





    /*
    //TODO Se non si presenta il caso in cui la macchina è raggiungibile ma lo status non è UP, integrare questa logica nell'altro scheduler
    @Scheduled(fixedDelay = 50_000) //delay in milliseconds
    public void periodicCheckDownStatus() {
        Map<String, List<InstanceInfo>> services = getServicesInstances();
        services.forEach((serviceName, serviceInstances) -> {
            serviceInstances.forEach(instance -> {

                    String url = instance.getHomePageUrl() + "actuator/health";
                    try {
                        ResponseEntity<Object> response = new RestTemplate().getForEntity(url, Object.class);
                        String status = response.getBody().toString();
                        if (!status.contains("status=UP"))
                            throw new RuntimeException("Instance is down");
                        else
                            log.debug("Instance {} of service {} is up", instance.getHostName() + ":" + instance.getPort(), instance.getAppName());
                    } catch (Exception e) {
                        log.warn("Instance {} is down", url);
                        if(getServicesInstances().get(serviceName).contains(instance)) {
                            //this check is done to avoid the case in which the instance is gracefully
                            // disconnected from eureka since the original services list is not updated.
                            // Still, it can happen between the check and the actual call to the service, so how to solve?
                            InstanceMetrics instanceMetrics = new InstanceMetrics(instance.getAppName(), instance.getInstance());
                            instanceMetrics.setStatus(InstanceStatus.FAILED);
                            instanceMetrics.applyTimestamp();
                            knowledgeClient.addMetrics(instanceMetrics);

                        }
                    }

            });
        });
    }

    */

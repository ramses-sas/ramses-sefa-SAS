package it.polimi.saefa.knowledge.persistence.domain.architecture;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Data
@NoArgsConstructor
public class Instance {
    private String instanceId; //service implementation name @ ip : port
    private String serviceId;
    private InstanceStatus currentStatus = InstanceStatus.ACTIVE;

    //private List<InstanceMetrics> metrics = new LinkedList<>();

    public Instance(String instanceId, String serviceId) {
        this.instanceId = instanceId;
        this.serviceId = serviceId;
    }

    public Instance(String instanceId, String serviceId, InstanceStatus currentStatus) {
        this.instanceId = instanceId;
        this.serviceId = serviceId;
        this.currentStatus = currentStatus;
    }



    public String getAddress(){
        return instanceId.split("@")[1];
    }

    /*public void addMetric(InstanceMetrics metric){
        metrics.add(metric);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Instance instance = (Instance) o;
        return Objects.equals(instanceId, instance.instanceId) && Objects.equals(serviceId, instance.serviceId);
    }

     */
}

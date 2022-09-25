package it.polimi.saefa.instancesmanager.restinterface;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
public class AddInstancesResponse {

    private final List<SingleInstanceResponse> dockerizedInstances = new ArrayList<>();

    public void addContainerInfo(String imageName, String containerId, String containerName, String address, int port, List<String> envVars) {
        dockerizedInstances.add(new SingleInstanceResponse(imageName, containerId, containerName, address, port, envVars));
    }

    @AllArgsConstructor
    @Getter
    static class SingleInstanceResponse {
        private String imageName;
        private String containerId;
        private String containerName;
        private String address;
        private int port;
        private List<String> envVars;
    }
}

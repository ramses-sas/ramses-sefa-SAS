package it.polimi.saefa.instancesmanager.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceContainerInfo {
    private String imageName;
    private String containerId;
    private String containerName;
    private String address;
    private int port;
    private List<String> envVars;
}

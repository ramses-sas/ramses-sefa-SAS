package it.polimi.saefa.instancesmanager.domain;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.github.dockerjava.api.model.HostConfig.newHostConfig;


@Slf4j
@Service
public class InstancesManagerService {
    @Value("${EUREKA_IP_PORT}") String EUREKA_IP_PORT;
    @Value("${API_GATEWAY_IP_PORT}") String API_GATEWAY_IP_PORT;
    @Value("${MYSQL_SERVER}") String MYSQL_SERVER;

    @Autowired
    Environment env;

    DockerClient dockerClient;

    public InstancesManagerService(@Value("${DOCKER_HOST}") String dockerHost) {
        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(dockerHost)
                .build();
        log.warn("Docker host: {}", dockerHost);
        dockerClient = DockerClientBuilder.getInstance(config).build();
        List<Container> containers = dockerClient.listContainersCmd().withShowAll(true).exec();
        for (Container container : containers) {
            log.warn("Container: {}", container);
            log.warn("Container name: {}", Arrays.stream(container.getNames()).findFirst().orElse("N/A"));
            log.warn("Container: {}", Arrays.toString(container.getPorts()));
        }

    }

    // TODO deve ritornare info sul container creato, tipo id, ip, porta, etc
    public List<ServiceContainerInfo> addInstances(String serviceImplementationName, int numberOfInstances) {
        String imageName = serviceImplementationName;
        List<ServiceContainerInfo> serviceContainerInfos = new ArrayList<>(numberOfInstances);
        for (int i = 0; i < numberOfInstances; i++) {
            int randomPort = getRandomPort();
            ExposedPort serverPort = ExposedPort.tcp(randomPort);
            Ports portBindings = new Ports();
            portBindings.bind(serverPort, Ports.Binding.bindIpAndPort("0.0.0.0", randomPort));
            log.error("Port: {}", randomPort);

            //containers.forEach(container -> log.info("Container: {}", Arrays.stream(container.getNames()).findFirst().orElse("N/A")));
            String newContainerId = dockerClient.createContainerCmd(imageName)
                    .withName(imageName + "@" + randomPort)
                    .withEnv("EUREKA_IP_PORT=" + EUREKA_IP_PORT, "MYSQL_SERVER=" + MYSQL_SERVER, "SERVER_PORT=" + randomPort) // TODO: add host
                    .withExposedPorts(serverPort)
                    .withHostConfig(newHostConfig().withPortBindings(portBindings))
                    .exec().getId();
            dockerClient.startContainerCmd(newContainerId).exec();
            serviceContainerInfos.add(new ServiceContainerInfo(imageName, newContainerId, imageName + "@" + randomPort, randomPort));
        }
        return serviceContainerInfos;
    }

    private String buildContainerEnvVariables() {
        StringBuilder sb = new StringBuilder();
        String eurekaIpPort = env.getProperty("EUREKA_IP_PORT");
        if (eurekaIpPort != null)
            sb.append("EUREKA_IP_PORT=").append(eurekaIpPort);
        String apiGatewayIpPort = env.getProperty("API_GATEWAY_IP_PORT");
        if (apiGatewayIpPort != null)
            sb.append(", API_GATEWAY_IP_PORT=").append(apiGatewayIpPort);

        return "EUREKA_IP_PORT=" + EUREKA_IP_PORT + ",API_GATEWAY_IP_PORT=" + API_GATEWAY_IP_PORT + ",MYSQL_SERVER=" + MYSQL_SERVER;
    }

    private int getRandomPort() {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            if (serverSocket.getLocalPort() == 0)
                throw new IOException();
            return serverSocket.getLocalPort();
        } catch (IOException e) {
            throw new IllegalStateException("Could not find a free TCP/IP port to start the server", e);
        }
    }

	
}


package it.polimi.sefa.instancesmanager.domain;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.*;
import java.util.*;

import static com.github.dockerjava.api.model.HostConfig.newHostConfig;


@Slf4j
@Service
public class InstancesManagerService {
    private final Object lock = new Object();
    private final Environment env;
    private String currentProfile;
    private final String localIp;
    private final String dockerIp;
    private final String arch;
    private final DockerClient dockerClient;

    // <Profile, List of SimulationInstanceParams>
    private final Map<String, List<SimulationInstanceParams>> simulationInstanceParamsMap;


    public InstancesManagerService(Environment env, @Value("${CURRENT_PROFILE}") String currentProfile){
        this.env = env;
        localIp = getMachineLocalIp();
        dockerIp = env.getProperty("DOCKER_IP") != null ? env.getProperty("DOCKER_IP") : localIp;
        arch = env.getProperty("ARCH") != null ? env.getProperty("ARCH") : "arm64";
        String dockerPort = env.getProperty("DOCKER_PORT");
        if (dockerIp == null || dockerIp.isEmpty() || dockerPort == null || dockerPort.isEmpty())
            throw new RuntimeException("Docker IP and port must be set");
        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost("tcp://"+dockerIp+":"+dockerPort)
                .build();
        log.warn("Docker host: {}", config.getDockerHost());
        this.currentProfile = currentProfile;
        simulationInstanceParamsMap = new HashMap<>();
        dockerClient = DockerClientBuilder.getInstance(config).build();
        List<Container> containers = dockerClient.listContainersCmd().exec();
        for (Container container : containers) {
            log.warn("\nContainer name: {} \n\tports: {}", Arrays.stream(container.getNames()).findFirst().orElse("N/A"), Arrays.toString(container.getPorts()));
        }
        switch (currentProfile) {
            case "PerfectInstance" -> simulationInstanceParamsMap.put(currentProfile, List.of(
                    // (failureRate, sleepDuration, sleepVariance)
                    new SimulationInstanceParams(0.0, 0.01, 0.01)
            ));
            case "SlowInstance100ms" -> simulationInstanceParamsMap.put(currentProfile, List.of(
                    // (failureRate, sleepDuration, sleepVariance)
                    new SimulationInstanceParams(0.0, 0.1, 0.02)
            ));
            case "aBitFaultyInstance" -> simulationInstanceParamsMap.put(currentProfile, List.of(
                    // (failureRate, sleepDuration, sleepVariance)
                    new SimulationInstanceParams(0.02, 0.01, 0.001)
            ));
            case "AverageFaultyInstance" -> simulationInstanceParamsMap.put(currentProfile, List.of(
                    // (failureRate, sleepDuration, sleepVariance)
                    new SimulationInstanceParams(0.04, 0.02, 0.001)
            ));
            case "FaultyInstance" -> simulationInstanceParamsMap.put(currentProfile, List.of(
                    // (failureRate, sleepDuration, sleepVariance)
                    new SimulationInstanceParams(0.85, 0.015, 0.001)
            ));
            default -> {
            }
        }
    }


    public List<ServiceContainerInfo> addInstances(String serviceImplementationName, int numberOfInstances) {
        String imageName = "sbi98/sefa-"+serviceImplementationName+":"+arch;
        List<ServiceContainerInfo> serviceContainerInfos = new ArrayList<>(numberOfInstances);
        List<SimulationInstanceParams> simulationInstanceParamsList;
        synchronized (lock) {
            if (serviceImplementationName.equalsIgnoreCase("restaurant-service") || serviceImplementationName.startsWith("payment-proxy"))
                simulationInstanceParamsList = simulationInstanceParamsMap.get(currentProfile);
            else
                simulationInstanceParamsList = List.of(new SimulationInstanceParams(0.0, 0.0, 0.0));
        }
        for (int i = 0; i < numberOfInstances; i++) {
            int randomPort = getRandomPort();
            ExposedPort exposedRandomPort = ExposedPort.tcp(randomPort);
            Ports portBindings = new Ports();
            portBindings.bind(exposedRandomPort, Ports.Binding.bindIpAndPort("0.0.0.0", randomPort));
            String containerName = "sefa-" + serviceImplementationName + "-" + randomPort;
            HostConfig hostConfig = new HostConfig();
            hostConfig.withPortBindings(portBindings);
            hostConfig.withNetworkMode("ramses-sas-net");
            List<String> envVars = buildContainerEnvVariables(containerName, randomPort, simulationInstanceParamsList.get(i % simulationInstanceParamsList.size()));
            String newContainerId = dockerClient.createContainerCmd(imageName)
                    .withImage(imageName)
                    .withName(containerName)
                    .withEnv(envVars)
                    .withExposedPorts(exposedRandomPort)
                    .withHostConfig(hostConfig)
                    .exec().getId();
            dockerClient.startContainerCmd(newContainerId).exec();
            serviceContainerInfos.add(new ServiceContainerInfo(imageName, newContainerId, containerName, containerName, randomPort, envVars));
        }
        return serviceContainerInfos;
    }

    public void startInstance(String address, int port) {
        List<Container> containers = dockerClient
            .listContainersCmd()
            .withShowAll(true)
            .exec()
            .stream()
            .filter(container ->
                Arrays.stream(container.getNames()).anyMatch(name -> name.contains(address))
            )
            .toList();

        if (containers.size() == 1) {
            Container container = containers.get(0);
            try {
                dockerClient.startContainerCmd(container.getId()).exec();
            } catch (NotFoundException|NotModifiedException e){
                log.warn("Cannot start container {}", container.getId());
            }
            return;
        } else if (containers.size() == 0) {
            log.warn("Container {} at port {} not found. Considering it as crashed.", address, port);
            return;
        }
        throw new RuntimeException("Too many containers found: " + containers);
    }

    public void stopInstance(String address, int port) {
        List<Container> containers = dockerClient
            .listContainersCmd()
            .exec()
            .stream()
            .filter(container ->
                Arrays.stream(container.getNames()).anyMatch(name -> name.contains(address))
            )
            .toList();

        if (containers.size() == 1) {
            Container container = containers.get(0);
            try {
                dockerClient.stopContainerCmd(container.getId()).exec();
            } catch (NotFoundException|NotModifiedException e){
                log.warn("Container {} already removed", container.getId());
            }
            return;
        } else if (containers.size() == 0) {
            log.warn("Container {} at port {} not found. Considering it as crashed.", address, port);
            return;
        }
        throw new RuntimeException("Too many containers found: " + containers);
    }

    private List<String> buildContainerEnvVariables(String containerName, int serverPort, SimulationInstanceParams simulationInstanceParams) {
        List<String> envVars = new LinkedList<>();

        // Get Eureka, Gateway and MySQL addresses from Environment. When null, use the local IP address and the default ports
        /*String eurekaIpPort = env.getProperty("EUREKA_IP_PORT");
        envVars.add("EUREKA_IP_PORT=" + (eurekaIpPort == null ? localIp+":58082" : eurekaIpPort));
        String apiGatewayIpPort = env.getProperty("API_GATEWAY_IP_PORT");
        envVars.add("API_GATEWAY_IP_PORT="+(apiGatewayIpPort == null ? localIp+":58081" : apiGatewayIpPort));
        String mySqlIpPort = env.getProperty("MYSQL_IP_PORT");
        envVars.add("MYSQL_IP_PORT="+(mySqlIpPort == null ? localIp+":3306" : mySqlIpPort));*/
        envVars.add("SERVER_PORT="+serverPort);
        envVars.add("HOST="+containerName);
        envVars.add("SLEEP_MEAN="+simulationInstanceParams.getSleepDuration()*1000);
        envVars.add("SLEEP_VARIANCE="+simulationInstanceParams.getSleepVariance());
        envVars.add("EXCEPTION_PROBABILITY="+simulationInstanceParams.getExceptionProbability());
        return envVars;
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

    public void changeProfile(String newProfile) {
        synchronized (lock) {
            currentProfile = newProfile;
        }
    }

    private String getMachineLocalIp() {
        try (Socket socket = new Socket("1.1.1.1", 80)) {
            InetSocketAddress addr = (InetSocketAddress) socket.getLocalSocketAddress();
            log.info("Local address: {}", addr.getAddress().getHostAddress());
            return addr.getAddress().getHostAddress();
        } catch (IOException e) {
            throw new RuntimeException("Impossible to get local IP address", e);
        }
    }
}


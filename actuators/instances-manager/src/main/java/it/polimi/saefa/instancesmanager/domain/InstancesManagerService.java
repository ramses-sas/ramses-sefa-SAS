package it.polimi.saefa.instancesmanager.domain;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.*;

import static com.github.dockerjava.api.model.HostConfig.newHostConfig;


@Slf4j
@Service
public class InstancesManagerService {
    Environment env;
    String localIp;
    String dockerIp;
    DockerClient dockerClient;

    // <Profile, List of SimulationInstanceParams>
    Map<String, List<SimulationInstanceParams>> simulationInstanceParamsMap;


    public InstancesManagerService(Environment env) throws UnknownHostException {
        localIp = InetAddress.getLocalHost().getHostAddress();
        this.env = env;
        dockerIp = env.getProperty("DOCKER_IP");
        String dockerPort = env.getProperty("DOCKER_PORT");
        if (dockerIp == null || dockerIp.isEmpty())
            dockerIp = localIp;
        if (dockerIp == null || dockerIp.isEmpty() || dockerPort == null || dockerPort.isEmpty())
            throw new RuntimeException("Docker IP and port must be set");
        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost("tcp://"+dockerIp+":"+dockerPort)
                .build();
        log.warn("Docker host: {}", config.getDockerHost());
        dockerClient = DockerClientBuilder.getInstance(config).build();
        List<Container> containers = dockerClient.listContainersCmd().exec();
        for (Container container : containers) {
            //log.warn("Container: {}", container);
            log.warn("\nContainer name: {} \n\tports: {}", Arrays.stream(container.getNames()).findFirst().orElse("N/A"), Arrays.toString(container.getPorts()));
        }
        String profile = env.getProperty("PROFILE");
        profile = "AAA";
        simulationInstanceParamsMap = new HashMap<>();
        switch (profile) {
            case "AAA":
                simulationInstanceParamsMap.put(profile, List.of(
                    new SimulationInstanceParams(0.1, 1.7, 0.2),
                    new SimulationInstanceParams(0.2, 2.0, 0.2),
                    new SimulationInstanceParams(0.5, 4.0, 0.5)
                ));
                break;
            default:
                break;
        }
    }


    public List<ServiceContainerInfo> addInstances(String serviceImplementationName, int numberOfInstances) {
        String imageName = serviceImplementationName;
        List<ServiceContainerInfo> serviceContainerInfos = new ArrayList<>(numberOfInstances);
        String profile = env.getProperty("PROFILE");
        profile = "AAA";
        List<SimulationInstanceParams> simulationInstanceParamsList = simulationInstanceParamsMap.get(profile);
        for (int i = 0; i < numberOfInstances; i++) {
            int randomPort = getRandomPort();
            ExposedPort serverPort = ExposedPort.tcp(randomPort);
            Ports portBindings = new Ports();
            portBindings.bind(serverPort, Ports.Binding.bindIpAndPort("0.0.0.0", randomPort));
            List<String> envVars = buildContainerEnvVariables(randomPort, simulationInstanceParamsList.get(i % simulationInstanceParamsList.size()));
            String newContainerId = dockerClient.createContainerCmd(imageName)
                    .withName(imageName + "_" + randomPort)
                    .withEnv(envVars)
                    .withExposedPorts(serverPort)
                    .withHostConfig(newHostConfig().withPortBindings(portBindings))
                    .exec().getId();
            dockerClient.startContainerCmd(newContainerId).exec();
            serviceContainerInfos.add(new ServiceContainerInfo(imageName, newContainerId, imageName + "_" + randomPort, dockerIp, randomPort, envVars));
        }
        return serviceContainerInfos;
    }

    public void removeInstance(String serviceImplementationName, String address, int port) {
        List<Container> containers = dockerClient.listContainersCmd().withNameFilter(Collections.singleton(serviceImplementationName+"_"+port)).exec();
        if (containers.size() == 1) {
            Container container = containers.get(0);
            dockerClient.stopContainerCmd(container.getId()).exec();
            dockerClient.removeContainerCmd(container.getId()).exec();
            return;
        }
        throw new RuntimeException("Container not found");
    }

    private List<String> buildContainerEnvVariables(int serverPort, SimulationInstanceParams simulationInstanceParams) {
        List<String> envVars = new LinkedList<>();

        // Get Eureka, Gateway and MySQL addresses from Environment. When null, use the local IP address and the default ports
        String eurekaIpPort = env.getProperty("EUREKA_IP_PORT");
        envVars.add("EUREKA_IP_PORT=" + (eurekaIpPort == null ? localIp+":58082" : eurekaIpPort));
        String apiGatewayIpPort = env.getProperty("API_GATEWAY_IP_PORT");
        envVars.add("API_GATEWAY_IP_PORT="+(apiGatewayIpPort == null ? localIp+":58081" : apiGatewayIpPort));
        String mySqlIpPort = env.getProperty("MYSQL_IP_PORT");
        envVars.add("MYSQL_IP_PORT="+(mySqlIpPort == null ? localIp+":3306" : mySqlIpPort));
        envVars.add("SERVER_PORT="+serverPort);
        envVars.add("HOST="+dockerIp);

        /*
        // TO SIMULATE SOME SCENARIOS
        double[] sleepMeanSecondsPool = new double[]{1, 2, 3, 4, 5};
        double sleepMean = sleepMeanSecondsPool[new Random().nextInt(sleepMeanSecondsPool.length)]*1000;
        double sleepVariance = 1500;
        double[] exceptionProbabilitiesPool = new double[]{0, 0.2, 0.65, 0.9};
        double exceptionProbability = exceptionProbabilitiesPool[new Random().nextInt(exceptionProbabilitiesPool.length)];
        */
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


    public String getDockerIp() {
        return dockerIp;
    }

    public DockerClient getDockerClient() {
        return dockerClient;
    }
}


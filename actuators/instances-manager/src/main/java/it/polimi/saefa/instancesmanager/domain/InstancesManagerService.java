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
    String dockerIp;
    DockerClient dockerClient;

    public InstancesManagerService(Environment env) throws UnknownHostException {
        this.env = env;
        dockerIp = env.getProperty("DOCKER_IP");
        String dockerPort = env.getProperty("DOCKER_PORT");
        if (dockerIp == null || dockerIp.isEmpty())
            dockerIp = InetAddress.getLocalHost().getHostAddress();
        if (dockerIp == null || dockerIp.isEmpty() || dockerPort == null || dockerPort.isEmpty())
            throw new RuntimeException("Docker IP and port must be set");
        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost("tcp://"+dockerIp+":"+dockerPort)
                .build();
        log.warn("Docker host: {}", config.getDockerHost());
        dockerClient = DockerClientBuilder.getInstance(config).build();
        List<Container> containers = dockerClient.listContainersCmd().withShowAll(true).exec();
        for (Container container : containers) {
            log.warn("Container: {}", container);
            log.warn("Container name: {}", Arrays.stream(container.getNames()).findFirst().orElse("N/A"));
            log.warn("Container: {}", Arrays.toString(container.getPorts()));
        }
    }


    public List<ServiceContainerInfo> addInstances(String serviceImplementationName, int numberOfInstances) {
        String imageName = serviceImplementationName;
        List<ServiceContainerInfo> serviceContainerInfos = new ArrayList<>(numberOfInstances);
        for (int i = 0; i < numberOfInstances; i++) {
            int randomPort = getRandomPort();
            ExposedPort serverPort = ExposedPort.tcp(randomPort);
            Ports portBindings = new Ports();
            portBindings.bind(serverPort, Ports.Binding.bindIpAndPort("0.0.0.0", randomPort));
            List<String> envVars = buildContainerEnvVariables(randomPort);
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

    private List<String> buildContainerEnvVariables(int serverPort) {
        List<String> envVars = new LinkedList<>();
        String eurekaIpPort = env.getProperty("EUREKA_IP_PORT");
        if (eurekaIpPort != null)
            envVars.add("EUREKA_IP_PORT="+eurekaIpPort);
        String apiGatewayIpPort = env.getProperty("API_GATEWAY_IP_PORT");
        if (apiGatewayIpPort != null)
            envVars.add("API_GATEWAY_IP_PORT="+apiGatewayIpPort);
        String mySqlIpPort = env.getProperty("MYSQL_IP_PORT");
        if (mySqlIpPort != null)
            envVars.add("MYSQL_IP_PORT="+mySqlIpPort);
        envVars.add("SERVER_PORT="+serverPort);
        envVars.add("HOST="+dockerIp);

        // TO SIMULATE SOME SCENARIOS
        double[] sleepMeanSecondsPool = new double[]{1, 2, 3, 4, 5};
        double sleepMean = sleepMeanSecondsPool[new Random().nextInt(sleepMeanSecondsPool.length)]*1000;
        double sleepVariance = 1500;
        envVars.add("SLEEP_MEAN="+sleepMean);
        envVars.add("SLEEP_VARIANCE="+sleepVariance);
        double[] exceptionProbabilitiesPool = new double[]{0, 0.2, 0.65, 0.9};
        double exceptionProbability = exceptionProbabilitiesPool[new Random().nextInt(exceptionProbabilitiesPool.length)];
        envVars.add("EXCEPTION_PROBABILITY="+exceptionProbability);
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


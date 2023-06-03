package it.polimi.sefa.configmanager.domain;

import it.polimi.ramses.configparser.CustomPropertiesWriter;
import it.polimi.sefa.configmanager.restinterface.PropertyToChange;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Slf4j
@Service
public class ConfigManagerService {
    Git gitClient;
    CredentialsProvider credentialsProvider;

    public ConfigManagerService(@Value("${GITHUB_REPOSITORY_URL}") String gitRepository) throws Exception {
        String token = System.getenv("GITHUB_OAUTH");
        if (token == null)
            throw new Exception("GITHUB_OAUTH environment variable not set");
        credentialsProvider = new UsernamePasswordCredentialsProvider(token, "");
        gitClient = Git.cloneRepository()
                .setURI(gitRepository)
                .setDirectory(Files.createTempDirectory("config-server").toFile())
                .call();
        log.info("Cloned repository: {}", gitClient.getRepository().getDirectory().getParent());
    }

    public void commitAndPush(String message) throws Exception {
        gitClient.add().addFilepattern(".").call();
        gitClient.commit().setMessage(message).call();
        gitClient.push().setRemote("origin").setForce(true).setCredentialsProvider(credentialsProvider).call();
    }

    public void pull() throws Exception {
        gitClient.pull().setCredentialsProvider(credentialsProvider).setRebase(true).call();
    }

    public void rollback() throws Exception {
        gitClient.checkout().setName("HEAD").call();
    }

    /**
     * Contacts the Config Manager actuator to change the weights of the load balancer of the specified service.
     *
     * @param serviceId the id of the service to change the weights of
     * @param weights the new weights of the load balancer for the active instances
     * @param instancesToShutdownIds the instances that will be shut down
     */
    public void updateLoadbalancerWeights(String serviceId, Map<String, Double> weights, List<String> instancesToShutdownIds) throws Exception {
        if (serviceId == null) throw new Exception("Service id is null");
        List<PropertyToChange> propertyToChangeList = new LinkedList<>();
        if (weights != null) {
            weights.forEach((instanceId, weight) -> {
                String propertyKey = CustomPropertiesWriter.buildLoadBalancerInstanceWeightPropertyKey(serviceId, instanceId.split("@")[1]);
                propertyToChangeList.add(new PropertyToChange(null, propertyKey, weight == null ? null : weight.toString()));
            });
        }
        if (instancesToShutdownIds != null) {
            instancesToShutdownIds.forEach(instanceId -> {
                String propertyKey = CustomPropertiesWriter.buildLoadBalancerInstanceWeightPropertyKey(serviceId, instanceId.split("@")[1]);
                propertyToChangeList.add(new PropertyToChange(null, propertyKey));
            });
        }
        for (PropertyToChange propertyToChange : propertyToChangeList) {
            String filename = propertyToChange.getServiceName() == null ? "application.properties" : propertyToChange.getServiceName().toLowerCase() + ".properties";
            changePropertyInFile(propertyToChange.getPropertyName(), propertyToChange.getValue(), filename);
        }
    }

    /**
     * Changes a property in a file.
     *
     * @param property the key of the property to change. It cannot be null. If the property does not exist, it will be added.
     * @param value the new value of the property. If null, the property will be removed.
     * @param fileName the name of the file to change the property in
     */
    public void changePropertyInFile(String property, String value, String fileName) throws Exception {
        log.info("Changing property {} with value {}", property, value);
        Path propFile = Path.of(gitClient.getRepository().getDirectory().getParent(), fileName);
        List<String> fileContent = new ArrayList<>(Files.readAllLines(propFile, StandardCharsets.UTF_8));
        boolean propertyFound = false;
        for (int i = 0; i < fileContent.size(); i++) {
            if (fileContent.get(i).startsWith(property+"=")) {
                if (value == null)
                    fileContent.remove(i);
                else
                    fileContent.set(i, property+"="+value);
                propertyFound = true;
                break;
            }
        }
        if (!propertyFound) {
            if (value != null)
                fileContent.add(property+"="+value);
        }
        Files.write(propFile, fileContent, StandardCharsets.UTF_8);
    }
}


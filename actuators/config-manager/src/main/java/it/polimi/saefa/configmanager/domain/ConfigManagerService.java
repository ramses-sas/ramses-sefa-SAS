package it.polimi.saefa.configmanager.domain;

import it.polimi.saefa.configmanager.restinterface.PropertyToChange;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;


@Slf4j
@Service
public class ConfigManagerService {
    Git gitClient;
    CredentialsProvider credentialsProvider;

    List<PropertyToChange> propertiesToChange = Collections.synchronizedList(new LinkedList<>());

    // Remember to set the auth token as an environment variable
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

    public void changeProperty(String property, String value, String fileName) throws Exception {
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
            else
                throw new Exception("Impossible to remove property. Property "+property+" not found in file "+fileName);
        }
        Files.write(propFile, fileContent, StandardCharsets.UTF_8);
    }

    public void commitAndPush(String message) throws Exception {
        gitClient.add().addFilepattern(".").call();
        gitClient.commit().setMessage(message).call();
        gitClient.push().setRemote("origin").setForce(true).setCredentialsProvider(credentialsProvider).call(); //.setRefSpecs(new RefSpec("refs/heads/master:refs/heads/master"))
    }

    public void pull() throws Exception {
        gitClient.pull().setCredentialsProvider(credentialsProvider).setRebase(true).call();
    }

    public void rollback() throws Exception {
        gitClient.checkout().call();
    }
}


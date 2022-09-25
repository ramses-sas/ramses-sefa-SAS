package it.polimi.saefa.configmanager.domain;

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
import java.util.ArrayList;
import java.util.List;


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

    public void addOrUpdatePropertyAndPush(String property, String value, String fileName) throws Exception {
        pull();
        addOrUpdateProperty(property, value, fileName);
        commitAndPush("Added property "+property+" with value "+value);
    }

    public void removePropertyAndPush(String property, String fileName) throws Exception {
        pull();
        removeProperty(property, fileName);
        commitAndPush("Removed property "+property);
    }

    private void removeProperty(String property, String fileName) throws Exception {
        log.info("Removing property {}", property);
        Path propFile = Path.of(gitClient.getRepository().getDirectory().getParent(), fileName);
        List<String> fileContent = new ArrayList<>(Files.readAllLines(propFile, StandardCharsets.UTF_8));
        boolean propertyFound = false;
        for (int i = 0; i < fileContent.size(); i++) {
            if (fileContent.get(i).startsWith(property+"=")) {
                fileContent.remove(i);
                propertyFound = true;
                break;
            }
        }
        if (!propertyFound)
            throw new Exception("Property "+property+" not found in file "+fileName);
        Files.write(propFile, fileContent, StandardCharsets.UTF_8);
    }

    private void addOrUpdateProperty(String property, String value, String fileName) throws IOException {
        log.info("Adding property {} with value {}", property, value);
        Path propFile = Path.of(gitClient.getRepository().getDirectory().getParent(), fileName);
        List<String> fileContent = new ArrayList<>(Files.readAllLines(propFile, StandardCharsets.UTF_8));
        boolean propertyFound = false;
        for (int i = 0; i < fileContent.size(); i++) {
            if (fileContent.get(i).startsWith(property+"=")) {
                fileContent.set(i, property+"="+value);
                propertyFound = true;
                break;
            }
        }
        if (!propertyFound)
            fileContent.add(property+"="+value);
        Files.write(propFile, fileContent, StandardCharsets.UTF_8);
    }

    private void commitAndPush(String message) throws Exception {
        gitClient.add().addFilepattern(".").call();
        gitClient.commit().setMessage(message).call();
        gitClient.push().setRemote("origin").setForce(true).setCredentialsProvider(credentialsProvider).call(); //.setRefSpecs(new RefSpec("refs/heads/master:refs/heads/master"))
    }

    private void pull() throws Exception {
        gitClient.pull().setCredentialsProvider(credentialsProvider).setRebase(true).call();
    }
}


package it.polimi.saefa.monitor.externalinterfaces;

import it.polimi.saefa.knowledge.persistence.InstanceMetrics;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "KNOWLEDGE", url = "http://localhost:58005")
public interface KnowledgeClient {
    @GetMapping("/rest/getAll")
    List<InstanceMetrics> getMetrics();

    @PostMapping("/rest/")
    void addMetrics(@RequestBody InstanceMetrics metrics);
}

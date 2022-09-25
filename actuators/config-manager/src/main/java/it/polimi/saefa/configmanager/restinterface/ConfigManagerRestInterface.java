package it.polimi.saefa.configmanager.restinterface;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface ConfigManagerRestInterface {

    @PostMapping(path = "/addInstances")
    void addOrUpdateProperty(@RequestBody AddOrUpdatePropertyRequest request);

}

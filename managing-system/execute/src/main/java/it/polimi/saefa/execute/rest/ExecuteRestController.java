package it.polimi.saefa.execute.rest;

import it.polimi.saefa.execute.domain.ExecuteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping(path="/rest")
public class ExecuteRestController {

    @Autowired
    private ExecuteService executeService;

    @GetMapping("/start")
    public String start() {
        (new Thread(() -> executeService.execute())).start();
        return "OK";
    }

    // TODO remove after test
    @GetMapping("/")
    public String debug() {
        executeService.breakpoint();
        return "Hello from Execute Service";
    }
}

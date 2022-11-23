package it.polimi.sofa.restaurantservice.rest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping(path="/rest/test")
public class TestingRestController {

    @GetMapping("/")
    public String dummy() {
        return "echovar";
    }

    @GetMapping("/sleep")
    public String dummy2() throws InterruptedException {
        Thread.sleep(10000);
        return "echovar";
    }


}

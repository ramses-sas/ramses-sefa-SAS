package it.polimi.sofa.paymentproxy3service.rest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path="/rest/test")
public class TestingRestController {

    @GetMapping("/{echovar}")
    public String dummy(@PathVariable String echovar) {
        return echovar;
    }
}

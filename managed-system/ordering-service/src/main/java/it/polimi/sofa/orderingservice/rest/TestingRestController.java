package it.polimi.sofa.orderingservice.rest;

import it.polimi.sofa.orderingservice.domain.OrderingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path="/rest/test")
public class TestingRestController {
    @Autowired
    private OrderingService orderingService;

    @GetMapping("/{echovar}")
    public String dummy(@PathVariable String echovar) {
        return echovar;
    }

}

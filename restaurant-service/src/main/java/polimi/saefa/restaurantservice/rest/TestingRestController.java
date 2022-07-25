package polimi.saefa.restaurantservice.rest;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping(path="/rest/test")
public class TestingRestController {

    @GetMapping("/{echovar}")
    public String dummy(@PathVariable String echovar) {
        return echovar;
    }
}

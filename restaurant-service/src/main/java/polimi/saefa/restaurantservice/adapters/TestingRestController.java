package polimi.saefa.restaurantservice.adapters;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping(path="/rest/test")
public class TestingRestController {

    @GetMapping("/{echovar}")
    public String getRestaurant(@PathVariable String echovar) {
        return echovar;
    }
}

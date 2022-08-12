package polimi.saefa.orderingservice;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import polimi.saefa.orderingservice.CheckService;
import polimi.saefa.orderingservice.domain.OrderingService;

@RestController
public class CheckController {

    public CheckController(OrderingService orderingService) {
        this.orderingService = orderingService;
    }

    private final OrderingService orderingService;


    @GetMapping("/check")
    public Map<String, Number> check() {
        return orderingService.check();
    }

}

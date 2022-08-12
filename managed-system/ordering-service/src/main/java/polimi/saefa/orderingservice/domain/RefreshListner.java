package polimi.saefa.orderingservice.domain;


import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.ApplicationListener;
import polimi.saefa.orderingservice.rest.OrderingRestController;

import java.util.logging.Logger;

public class RefreshListner implements ApplicationListener<RefreshScopeRefreshedEvent> {

    private OrderingService orderingService;
    private final Logger logger = Logger.getLogger(RefreshListner.class.toString());

    public RefreshListner(OrderingService orderingService) {
        this.orderingService = orderingService;
    }

    @Override
    public void onApplicationEvent(RefreshScopeRefreshedEvent event) {
        //logger.warning("RefreshListner: resetting cricuitbreaker");
        //orderingService.refreshCircuitBreaker();
    }

}


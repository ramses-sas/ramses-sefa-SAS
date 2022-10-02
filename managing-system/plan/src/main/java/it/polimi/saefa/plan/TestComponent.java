package it.polimi.saefa.plan;

import it.polimi.saefa.plan.domain.PlanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TestComponent implements CommandLineRunner {

    @Autowired
    PlanService service;

    @Override
    public void run(String... args) throws Exception {

        /*
        log.warn("Starting iteration 0");
        for (int i = 0; i < 10_000 && service.handleChangeLoadBalancerWeightsTEST()!=null; i++) {
            log.warn("Starting iteration " + (i+1));
        }

         */
    }

}

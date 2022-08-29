package it.polimi.saefa.knowledge;

import it.polimi.saefa.knowledge.persistence.InstanceMetrics;
import it.polimi.saefa.knowledge.persistence.MetricsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/*
@Component
@Slf4j
@Transactional
public class CLIRunner implements CommandLineRunner {
    @Autowired
    private MetricsRepository repo;

    @Override
    public void run(String... args) throws Exception {
        InstanceMetrics i = new InstanceMetrics("RESTAURANT-SERVICE", "localhost:58085");
        i.applyTimestamp();
        repo.save(i);
        i = new InstanceMetrics("RESTAURANT-SERVICE", "localhost:58086");
        i.applyTimestamp();
        //repo.save(i);

        Instant now = Instant.now();
        Instant yesterday = now.minus(1, ChronoUnit.DAYS);
        Instant tomorrow = now.plus(1, ChronoUnit.DAYS);

        Collection<InstanceMetrics> all = repo.findAllByTimestampBetween(Date.from(yesterday), Date.from(tomorrow));
        all.forEach(elem -> log.info(elem.toString()));
    }

}

 */

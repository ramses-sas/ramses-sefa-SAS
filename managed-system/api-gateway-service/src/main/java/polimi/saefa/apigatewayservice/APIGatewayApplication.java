package polimi.saefa.apigatewayservice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.cloud.autoconfigure.RefreshEndpointAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.cloud.endpoint.RefreshEndpoint;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

@Slf4j
@EnableDiscoveryClient
@SpringBootApplication
public class APIGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(APIGatewayApplication.class, args);
    }

    @Autowired
    private Environment env;

    // cosi posso scrivere custom logic per il refresh. Ma devo sempre chiamare il refresh endpoint
    @EventListener(EnvironmentChangeEvent.class)
    public void onApplicationEvent(EnvironmentChangeEvent environmentChangeEvent) {
        log.info("Received an environment changed event for keys {}", environmentChangeEvent.getKeys());
        if (environmentChangeEvent.getKeys().contains("key.i.wanted.to.recalculate")) {
            String newValue = env.getProperty("key.i.wanted.to.recalculate");
            System.out.println("New Value: " + newValue);
        }
    }

    @ConditionalOnBean({RefreshEndpoint.class})
    @Configuration
    @AutoConfigureAfter({RefreshAutoConfiguration.class, RefreshEndpointAutoConfiguration.class})
    @EnableScheduling
    public class ConfigClientAutoRefreshConfiguration implements SchedulingConfigurer {
        @Value("${spring.cloud.config.refresh-interval:60}")
        private long refreshInterval;
        @Value("${spring.cloud.config.auto-refresh:false}")
        private boolean autoRefresh;
        private final RefreshEndpoint refreshEndpoint;
        public ConfigClientAutoRefreshConfiguration(RefreshEndpoint refreshEndpoint) {
            this.refreshEndpoint = refreshEndpoint;
        }
        @Override
        public void configureTasks(ScheduledTaskRegistrar scheduledTaskRegistrar) {
            if (autoRefresh) {
                // set minimal refresh interval to 5 seconds
                refreshInterval = Math.max(refreshInterval, 5);
                scheduledTaskRegistrar.addFixedRateTask(refreshEndpoint::refresh, refreshInterval * 1000);
            }
        }
    }
}

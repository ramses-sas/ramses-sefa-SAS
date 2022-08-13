package polimi.saefa.apigatewayservice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@EnableDiscoveryClient
@SpringBootApplication
public class APIGatewayApplication {

    public static void main(String[] args) {
        //Pattern lbPattern = Pattern.compile("loadbalancing\\.([\\w-\\.])+\\.weight", Pattern.CASE_INSENSITIVE);
        Pattern lbPattern = Pattern.compile("loadbalancing\\.([\\w\\d-.])+", Pattern.CASE_INSENSITIVE);
        Matcher lbMatcher = lbPattern.matcher("loadbalancing.restaurant-service");
        //Matcher lbMatcher = lbPattern.matcher("loadbalancing.restaurant-service.92.168.1.1-8888.weight");
        /*Pattern serviceNamePattern = Pattern.compile("([\\w-])\\.", Pattern.CASE_INSENSITIVE);
        for (MatchResult matchResult : matchResults) {
            System.out.println(matchResult.group(0));
        }*/
        //boolean matchFound = matcher.results().toList().size() > 0;
        while(lbMatcher.find()){
            log.info("matchFound: {}", lbMatcher.group());
        }
        //SpringApplication.run(APIGatewayApplication.class, args);
    }


    // cosi posso scrivere custom logic per il refresh.
    /*@EventListener(EnvironmentChangeEvent.class)
    public void onApplicationEvent(EnvironmentChangeEvent environmentChangeEvent) {
        // Received an environment changed event for keys [config.client.version, test.property]
        log.info("Received an environment changed event for keys {}", environmentChangeEvent.getKeys());
    }*/
}

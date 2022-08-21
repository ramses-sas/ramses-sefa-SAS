package polimi.saefa.apigatewayservice;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;



@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@EventListener(EnvironmentChangeEvent.class)
public @interface Monitor {
    String value() default ""; // the argument of the annotation

    
}

@Slf4j
@Aspect
@Component
class MonitorAspect {

    /*@EventListener(EnvironmentChangeEvent.class)
    public void onApplicationEvent(EnvironmentChangeEvent environmentChangeEvent) {
        // Received an environment changed event for keys [config.client.version, test.property]
        log.info("Received an environment changed event for keys {}", environmentChangeEvent.getKeys());
    }*/

    @Around("@annotation(Monitor)")
    public Object monitor(ProceedingJoinPoint joinPoint) throws Throwable {
        log.warn("Monitoring method {}", joinPoint.getSignature().getName());
        //System.out.println("Input :\n" + joinPoint.getArgs()[0]);

        log.warn(joinPoint.getThis().toString());

        Object result = joinPoint.proceed();
        if (result != null)
            log.warn(result.toString());
        //System.out.println(result);

        return result;
    }

}

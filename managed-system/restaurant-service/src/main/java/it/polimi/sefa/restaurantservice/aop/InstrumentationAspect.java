package it.polimi.sefa.restaurantservice.aop;

import it.polimi.sefa.restaurantservice.exceptions.ForcedException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Random;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Component
@Aspect
@Slf4j
@RestController
public class InstrumentationAspect {
    private Double sleepMean;
    private Double sleepVariance;
    private Double exceptionProbability;
    @Autowired
    private Environment env;
    

    public InstrumentationAspect(Environment env) {
        String sleepMean, sleepVariance, exceptionProbability;
        sleepMean = env.getProperty("SLEEP_MEAN");
        sleepVariance = env.getProperty("SLEEP_VARIANCE");
        exceptionProbability = env.getProperty("EXCEPTION_PROBABILITY");
        this.sleepMean = sleepMean == null ? null : Double.parseDouble(sleepMean);
        this.sleepVariance = sleepVariance == null ? null : Double.parseDouble(sleepVariance);
        this.exceptionProbability = exceptionProbability == null ? null : Double.parseDouble(exceptionProbability);
        log.debug("InstrumentationAspect: sleepMean={}, sleepVariance={}, exceptionProbability={}", sleepMean, sleepVariance, exceptionProbability);
    }

    @Pointcut("execution(public * it.polimi.sefa.restaurantservice.domain.RestaurantService.*(..))")
    public void restaurantServiceMethods() {}

    @Pointcut("execution(public void it.polimi.sefa.restaurantservice.domain.RestaurantService.*(..))")
    public void restaurantServiceVoidMethods() {}

    private void logInvocation(JoinPoint joinPoint) {
        final String args = Arrays.toString(joinPoint.getArgs());
        final String methodName = joinPoint.getSignature().getName().replace("(..)", "()");
        log.info("CALL RestaurantService.{} {}", methodName, args);
    }

    private void logTermination(JoinPoint joinPoint, Object retValue) {
        final String args = Arrays.toString(joinPoint.getArgs());
        final String methodName = joinPoint.getSignature().getName().replace("(..)", "()");
        log.info("     RestaurantService.{} {} -> {}", methodName, args, retValue.toString());
    }

    private void logVoidTermination(JoinPoint joinPoint) {
        final String args = Arrays.toString(joinPoint.getArgs());
        final String methodName = joinPoint.getSignature().getName().replace("(..)", "()");
        log.info("     RestaurantService.{} {} -> RETURN", methodName, args);
    }

    private void logException(JoinPoint joinPoint, Object exception) {
        final String args = Arrays.toString(joinPoint.getArgs());
        final String methodName = joinPoint.getSignature().getName().replace("(..)", "()");
        log.info("     ERROR IN RestaurantService.{} {} -> {}", methodName, args, exception.toString());
    }

    @Before("restaurantServiceMethods()")
    public void logBeforeExecuteMethod(JoinPoint joinPoint) {
        try {
            long sleepTime = generateSleep();
            log.debug("Sleep duration: "+sleepTime);
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {}
        logInvocation(joinPoint);
    }

    @AfterReturning(value="restaurantServiceMethods() &&! restaurantServiceVoidMethods()", returning="retValue")
    public void logSuccessMethod(JoinPoint joinPoint, Object retValue) {
        // Throw an exception with a certain probability
        shouldThrowException();
        logTermination(joinPoint, retValue);
    }

    @AfterReturning("restaurantServiceVoidMethods()")
    public void logSuccessVoidMethod(JoinPoint joinPoint) {
        // Throw an exception with a certain probability
        shouldThrowException();
        logVoidTermination(joinPoint);
    }

    @AfterThrowing(value="restaurantServiceMethods() || restaurantServiceVoidMethods()", throwing="exception")
    public void logErrorApplication(JoinPoint joinPoint, Exception exception) {
        logException(joinPoint, exception);
    }

    private long generateSleep() {
        if (sleepMean == null || sleepVariance == null)
            return 0;
        return Math.max((long)((new Random()).nextGaussian()*sleepVariance + sleepMean), 0);
    }

    private void shouldThrowException() throws ForcedException {
        if (exceptionProbability != null && (new Random()).nextDouble() < exceptionProbability){
            log.warn("Throwing artificial exception");
            throw new ForcedException("An artificial exception has been thrown! Host: "+ env.getProperty("HOST") + ":" + env.getProperty("SERVER_PORT"));
        }
    }


    @PutMapping("/rest/instrumentation/sleepMean")
    public void setSleepMean(@RequestParam Double sleepMean) {
        log.debug("Setting sleepMean to {}", sleepMean);
        this.sleepMean = sleepMean;
        this.sleepVariance = 0.0;
    }

    @PutMapping("/rest/instrumentation/exceptionProbability")
    public void setExceptionProbability(@RequestParam Double exceptionProbability) {
        log.debug("Setting exceptionProbability to {}", exceptionProbability);
        this.exceptionProbability = exceptionProbability;
    }
    
    @GetMapping("/rest/instrumentation/sleepMean")
    public String getSleepMean() {
        return sleepMean == null ? "0.0" : sleepMean.toString();
    }
    
    @GetMapping("/rest/instrumentation/exceptionProbability")
    public String getExceptionProbability() {
        return exceptionProbability == null ? "0.0" :  exceptionProbability.toString();
    }
}


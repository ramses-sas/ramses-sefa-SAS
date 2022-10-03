package it.polimi.saefa.restaurantservice.aop;

import it.polimi.saefa.restaurantservice.exceptions.ForcedException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collection;
import java.util.Random;

@Component
@Aspect
@Slf4j
public class InstrumentationAspect {
    private final Double sleepMean;
    private final Double sleepVariance;
    private final Double exceptionProbability;
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

    /* Pointcut per il servizio dei ristoranti */
    @Pointcut("execution(public * it.polimi.saefa.restaurantservice.domain.RestaurantService.*(..))")
    public void restaurantServiceMethods() {}

    @Pointcut("execution(public void it.polimi.saefa.restaurantservice.domain.RestaurantService.*(..))")
    public void restaurantServiceVoidMethods() {}

	/* metodi di log */ 
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

    /* Eseguito prima dell'esecuzione del metodo */
    @Before("restaurantServiceMethods()")
    public void logBeforeExecuteMethod(JoinPoint joinPoint) {
        try {
            long sleepTime = generateSleep();
            log.debug("Sleep duration: "+sleepTime);
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {}
        logInvocation(joinPoint);
    }

    /* Eseguito quando il metodo è terminato (con successo) */
    @AfterReturning(value="restaurantServiceMethods() &&! restaurantServiceVoidMethods()", returning="retValue")
    public void logSuccessMethod(JoinPoint joinPoint, Object retValue) {
        // Throw an exception with a certain probability
        shouldThrowException();
        logTermination(joinPoint, retValue);
    }

    /* Eseguito quando il metodo (void) è terminato (con successo) */
    @AfterReturning("restaurantServiceVoidMethods()")
    public void logSuccessVoidMethod(JoinPoint joinPoint) {
        // Throw an exception with a certain probability
        shouldThrowException();
        logVoidTermination(joinPoint);
    }

    /* Eseguito se è stata sollevata un'eccezione */
    @AfterThrowing(value="restaurantServiceMethods()", throwing="exception")
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

}


package polimi.saefa.orderingservice.aoplog;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@Aspect
@Slf4j
public class OrderingServiceLoggingAspect {

    /* Pointcut per il servizio dei ristoranti */
    @Pointcut("execution(public * polimi.saefa.orderingservice.domain.OrderingService.*(..))")
    public void orderingServiceMethods() {}

    @Pointcut("execution(public void polimi.saefa.orderingservice.domain.OrderingService.*(..))")
    public void orderingServiceVoidMethods() {}

	/* metodi di log */ 
    private void logInvocation(JoinPoint joinPoint) {
        final String args = Arrays.toString(joinPoint.getArgs());
//        final String methodName = joinPoint.getSignature().toShortString().replace("(..)", "()");
        final String methodName = joinPoint.getSignature().getName().replace("(..)", "()");
        log.info("CALL OrderingService.{} {}", methodName, args);
    }

    private void logTermination(JoinPoint joinPoint, Object retValue) {
        final String args = Arrays.toString(joinPoint.getArgs());
//        final String methodName = joinPoint.getSignature().toShortString().replace("(..)", "()");
        final String methodName = joinPoint.getSignature().getName().replace("(..)", "()");
        log.info("     OrderingService.{} {} -> {}", methodName, args, retValue.toString());
    }

    private void logVoidTermination(JoinPoint joinPoint) {
        final String args = Arrays.toString(joinPoint.getArgs());
//        final String methodName = joinPoint.getSignature().toShortString().replace("(..)", "()");
        final String methodName = joinPoint.getSignature().getName().replace("(..)", "()");
        log.info("     OrderingService.{} {} -> RETURN", methodName, args);
    }

    private void logException(JoinPoint joinPoint, Object exception) {
        final String args = Arrays.toString(joinPoint.getArgs());
//        final String methodName = joinPoint.getSignature().toShortString().replace("(..)", "()");
        final String methodName = joinPoint.getSignature().getName().replace("(..)", "()");
        log.info("     ERROR IN OrderingService.{} {} -> {}", methodName, args, exception.toString());
    }

    /* Eseguito prima dell'esecuzione del metodo */
    @Before("orderingServiceMethods()")
    public void logBeforeExecuteMethod(JoinPoint joinPoint) {
        logInvocation(joinPoint);
    }

    /* Eseguito quando il metodo è terminato (con successo) */
    @AfterReturning(value="orderingServiceMethods() &&! orderingServiceVoidMethods()", returning="retValue")
    public void logSuccessMethod(JoinPoint joinPoint, Object retValue) {
        logTermination(joinPoint, retValue);
    }

    /* Eseguito quando il metodo (void) è terminato (con successo) */
    @AfterReturning("orderingServiceVoidMethods()")
    public void logSuccessVoidMethod(JoinPoint joinPoint) {
        logVoidTermination(joinPoint);
    }

    /* Eseguito se è stata sollevata un'eccezione */
    @AfterThrowing(value="orderingServiceMethods()", throwing="exception")
    public void logErrorApplication(JoinPoint joinPoint, Exception exception) {
        logException(joinPoint, exception);
    }

}


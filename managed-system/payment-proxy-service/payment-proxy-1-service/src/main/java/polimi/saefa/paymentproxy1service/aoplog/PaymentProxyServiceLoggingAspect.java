package polimi.saefa.paymentproxy1service.aoplog;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@Aspect
@Slf4j
public class PaymentProxyServiceLoggingAspect {

    /* Pointcut per il servizio dei ristoranti */
    @Pointcut("execution(public * polimi.saefa.paymentproxy1service.domain.PaymentProxyService.*(..))")
    public void paymentProxyServiceMethods() {}

    @Pointcut("execution(public void polimi.saefa.paymentproxy1service.domain.PaymentProxyService.*(..))")
    public void paymentProxyServiceVoidMethods() {}

	/* metodi di log */ 
    private void logInvocation(JoinPoint joinPoint) {
        final String args = Arrays.toString(joinPoint.getArgs());
//        final String methodName = joinPoint.getSignature().toShortString().replace("(..)", "()");
        final String methodName = joinPoint.getSignature().getName().replace("(..)", "()");
        log.info("CALL PaymentProxyService from proxy 1.{} {}", methodName, args);
    }

    private void logTermination(JoinPoint joinPoint, Object retValue) {
        final String args = Arrays.toString(joinPoint.getArgs());
//        final String methodName = joinPoint.getSignature().toShortString().replace("(..)", "()");
        final String methodName = joinPoint.getSignature().getName().replace("(..)", "()");
        log.info("     PaymentProxyService from proxy 1.{} {} -> {}", methodName, args, retValue.toString());
    }

    private void logVoidTermination(JoinPoint joinPoint) {
        final String args = Arrays.toString(joinPoint.getArgs());
//        final String methodName = joinPoint.getSignature().toShortString().replace("(..)", "()");
        final String methodName = joinPoint.getSignature().getName().replace("(..)", "()");
        log.info("     PaymentProxyService from proxy 1.{} {} -> RETURN", methodName, args);
    }

    private void logException(JoinPoint joinPoint, Object exception) {
        final String args = Arrays.toString(joinPoint.getArgs());
//        final String methodName = joinPoint.getSignature().toShortString().replace("(..)", "()");
        final String methodName = joinPoint.getSignature().getName().replace("(..)", "()");
        log.info("     ERROR IN PaymentProxyService from proxy 1.{} {} -> {}", methodName, args, exception.toString());
    }

    /* Eseguito prima dell'esecuzione del metodo */
    @Before("paymentProxyServiceMethods()")
    public void logBeforeExecuteMethod(JoinPoint joinPoint) {
        logInvocation(joinPoint);
    }

    /* Eseguito quando il metodo è terminato (con successo) */
    @AfterReturning(value="paymentProxyServiceMethods() &&! paymentProxyServiceVoidMethods()", returning="retValue")
    public void logSuccessMethod(JoinPoint joinPoint, Object retValue) {
        logTermination(joinPoint, retValue);
    }

    /* Eseguito quando il metodo (void) è terminato (con successo) */
    @AfterReturning("paymentProxyServiceVoidMethods()")
    public void logSuccessVoidMethod(JoinPoint joinPoint) {
        logVoidTermination(joinPoint);
    }

    /* Eseguito se è stata sollevata un'eccezione */
    @AfterThrowing(value="paymentProxyServiceMethods()", throwing="exception")
    public void logErrorApplication(JoinPoint joinPoint, Exception exception) {
        logException(joinPoint, exception);
    }

}


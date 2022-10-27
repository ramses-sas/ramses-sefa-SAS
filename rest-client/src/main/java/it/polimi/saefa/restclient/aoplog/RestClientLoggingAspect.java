package it.polimi.saefa.restclient.aoplog;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;

import java.util.Arrays;

@Component
@Aspect
@Slf4j
public class RestClientLoggingAspect {

    @Pointcut("execution(public * it.polimi.saefa.restclient.domain.RequestGeneratorService.*(..))")
    public void restClientMethods() {}

    @Pointcut("execution(public void it.polimi.saefa.restclient.domain.RequestGeneratorService.*(..))")
    public void restClientVoidMethods() {}

	/* metodi di log */ 
    private void logInvocation(JoinPoint joinPoint) {
        final String args = Arrays.toString(joinPoint.getArgs());
        final String methodName = joinPoint.getSignature().getName().replace("(..)", "()");
        log.info("CALL RestClient.{} {}", methodName, args);
    }

    private void logTermination(JoinPoint joinPoint, Object retValue) {
        final String args = Arrays.toString(joinPoint.getArgs());
        final String methodName = joinPoint.getSignature().getName().replace("(..)", "()");
        log.info("     RestClient.{} {} -> {}", methodName, args, retValue.toString());
    }

    private void logVoidTermination(JoinPoint joinPoint) {
        final String args = Arrays.toString(joinPoint.getArgs());
        final String methodName = joinPoint.getSignature().getName().replace("(..)", "()");
        log.info("     RestClient.{} {} -> RETURN", methodName, args);
    }

    private void logException(JoinPoint joinPoint, Object exception) {
        final String args = Arrays.toString(joinPoint.getArgs());
        final String methodName = joinPoint.getSignature().getName().replace("(..)", "()");
        log.error("     ERROR IN RestClient.{} {} -> {}", methodName, args, ((Exception)exception).getMessage());
    }

    /* Eseguito prima dell'esecuzione del metodo */
    //@Before("restClientMethods()")
    //public void logBeforeExecuteMethod(JoinPoint joinPoint) { logInvocation(joinPoint); }

    /* Eseguito quando il metodo è terminato (con successo) */
    //@AfterReturning(value="restClientMethods() &&! restClientVoidMethods()", returning="retValue")
    public void logSuccessMethod(JoinPoint joinPoint, Object retValue) {
        logTermination(joinPoint, retValue);
    }

    /* Eseguito quando il metodo (void) è terminato (con successo) */
    //@AfterReturning("restClientVoidMethods()")
    public void logSuccessVoidMethod(JoinPoint joinPoint) {
        logVoidTermination(joinPoint);
    }

    /* Eseguito se è stata sollevata un'eccezione */
    //@AfterThrowing(value="restClientMethods()", throwing="exception")
    //@Around("restClientMethods() || restClientVoidMethods()")
    public Object logErrorApplication(ProceedingJoinPoint joinPoint) {
        try {
            return joinPoint.proceed();
        } catch (HttpStatusCodeException e) {
            logException(joinPoint, e);
            return null;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }


}


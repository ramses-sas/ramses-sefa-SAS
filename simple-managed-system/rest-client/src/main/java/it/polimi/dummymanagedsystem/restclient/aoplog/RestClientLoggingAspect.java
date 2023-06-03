package it.polimi.dummymanagedsystem.restclient.aoplog;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;

import java.util.Arrays;
import java.util.Objects;

@Component
@Aspect
@Slf4j
public class RestClientLoggingAspect {

    @Pointcut("execution(public * it.polimi.dummymanagedsystem.restclient.domain.RequestGeneratorService.*(..))")
    public void restClientMethods() {}

    @Pointcut("execution(public void it.polimi.dummymanagedsystem.restclient.domain.RequestGeneratorService.*(..))")
    public void restClientVoidMethods() {}

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

    @Around("restClientMethods() || restClientVoidMethods()")
    public Object logErrorApplication(ProceedingJoinPoint joinPoint) {
        try {
            return joinPoint.proceed();
        } catch (HttpStatusCodeException e) {
            if (!Objects.requireNonNull(e.getMessage()).toLowerCase().contains("artificial"))
                logException(joinPoint, e);
            return null;
        } catch (Throwable e) {
            logException(joinPoint, e);
            throw new RuntimeException(e);
        }
    }
}


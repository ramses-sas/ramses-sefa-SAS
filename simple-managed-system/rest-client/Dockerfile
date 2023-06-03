FROM eclipse-temurin@sha256:408521f62c9a7589b3960addc4e21552ca0f47bfb8653bdc0a7de3648021d6d9
RUN mkdir /app
WORKDIR /app
COPY build/libs/*-latest.jar ./spring-boot-application.jar
ENTRYPOINT ["java","-jar","/app/spring-boot-application.jar"]

ENV EUREKA_IP_PORT=sms-eureka:58082
ENV API_GATEWAY_IP_PORT=sms-api-gateway:58081
ENV MONITOR_URL=http://ramses-monitor:58001
ENV PLAN_URL=http://ramses-plan:58003

ENV TRIAL_DURATION_MINUTES=20
ENV ADAPT=1


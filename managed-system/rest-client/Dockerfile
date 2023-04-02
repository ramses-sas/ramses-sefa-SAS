FROM eclipse-temurin@sha256:408521f62c9a7589b3960addc4e21552ca0f47bfb8653bdc0a7de3648021d6d9
RUN mkdir /app
WORKDIR /app
COPY build/libs/*-latest.jar ./spring-boot-application.jar
ENTRYPOINT ["java","-jar","/app/spring-boot-application.jar"]

ENV EUREKA_IP_PORT=sefa-eureka:58082
ENV API_GATEWAY_IP_PORT=sefa-api-gateway:58081
ENV MONITOR_URL=http://ramses-monitor:58001
ENV PLAN_URL=http://ramses-plan:58003
ENV KNOWLEDGE_URL=http://ramses-knowledge:58005
ENV PROBE_URL=http://sefa-probe:58020
ENV DOCKER_ACTUATOR_URL=http://sefa-instances-manager:58015

ENV TRIAL_DURATION_MINUTES=20
ENV ADAPT=1


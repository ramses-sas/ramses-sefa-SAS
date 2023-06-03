FROM eclipse-temurin@sha256:408521f62c9a7589b3960addc4e21552ca0f47bfb8653bdc0a7de3648021d6d9
RUN mkdir /app
WORKDIR /app
COPY build/libs/*-latest.jar ./spring-boot-application.jar
ENTRYPOINT ["java","-jar","/app/spring-boot-application.jar"]

ENV KNOWLEDGE_URL=http://ramses-knowledge:58005
ENV MONITOR_URL=http://ramses-monitor:58001
ENV INSTANCES_MANAGER_ACTUATOR_URL=http://sefa-instances-manager:58015
ENV CONFIG_MANAGER_ACTUATOR_URL=http://sefa-config-manager:58016

EXPOSE 58004


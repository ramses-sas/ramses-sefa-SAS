FROM eclipse-temurin@sha256:408521f62c9a7589b3960addc4e21552ca0f47bfb8653bdc0a7de3648021d6d9
RUN mkdir /app
RUN mkdir /app/architecture_sla
WORKDIR /app
COPY build/libs/*-latest.jar ./spring-boot-application.jar
COPY architecture_sla ./architecture_sla

EXPOSE 58005
ENV MYSQL_SERVER=mysql
ENV CONFIGURATION_PATH=/app/architecture_sla/sefa
ENV PROBE_URL=http://sefa-probe:58020

ENTRYPOINT ["java","-jar","/app/spring-boot-application.jar"]
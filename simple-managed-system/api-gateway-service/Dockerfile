FROM eclipse-temurin@sha256:408521f62c9a7589b3960addc4e21552ca0f47bfb8653bdc0a7de3648021d6d9
RUN mkdir /app
WORKDIR /app
COPY build/libs/*-latest.jar ./spring-boot-application.jar
ENTRYPOINT ["java","-jar","/app/spring-boot-application.jar"]
ENV HOST=sms-api-gateway
ENV EUREKA_IP_PORT=sms-eureka:58082
ENV SERVER_PORT 58081
EXPOSE 58081

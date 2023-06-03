FROM eclipse-temurin@sha256:408521f62c9a7589b3960addc4e21552ca0f47bfb8653bdc0a7de3648021d6d9
RUN mkdir /app
WORKDIR /app
COPY build/libs/*-latest.jar ./spring-boot-application.jar
ENTRYPOINT ["java","-jar","/app/spring-boot-application.jar"]
ENV EUREKA_IP_PORT=sefa-eureka:58082
ENV KNOWLEDGE_URL=http://ramses-knowledge:58005
ENV ANALYSE_URL=http://ramses-analyse:58002
ENV PROBE_URL=http://sefa-probe:58020
ENV SERVER_PORT=58001
ENV SCHEDULING_PERIOD=5000
EXPOSE 58001


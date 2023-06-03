FROM eclipse-temurin@sha256:408521f62c9a7589b3960addc4e21552ca0f47bfb8653bdc0a7de3648021d6d9
RUN mkdir /app
WORKDIR /app
COPY build/libs/*-latest.jar ./spring-boot-application.jar
COPY or-tools .
ENTRYPOINT ["java","-jar","/app/spring-boot-application.jar","/app"]

ENV KNOWLEDGE_URL=http://ramses-knowledge:58005
ENV EXECUTE_URL=http://ramses-execute:58004

EXPOSE 58003

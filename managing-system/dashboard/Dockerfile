FROM eclipse-temurin@sha256:408521f62c9a7589b3960addc4e21552ca0f47bfb8653bdc0a7de3648021d6d9
RUN mkdir /app
WORKDIR /app
COPY build/libs/*-latest.jar ./spring-boot-application.jar
ENTRYPOINT ["java","-jar","/app/spring-boot-application.jar"]

ENV KNOWLEDGE_URL=http://ramses-knowledge:58005
ENV MONITOR_URL=http://ramses-monitor:58001
ENV ANALYSE_URL=http://ramses-analyse:58002
ENV PLAN_URL=http://ramses-plan:58003
ENV EXECUTE_URL=http://ramses-execute:58004

ENV MAX_HISTORY_SIZE=45
ENV ADAPTATION_HISTORY_SIZE=3

EXPOSE 58000


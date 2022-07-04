FROM adoptopenjdk/openjdk16
RUN mkdir /app
WORKDIR /app
COPY build/libs/restaurant-service-latest.jar ./spring-boot-application.jar
ENTRYPOINT ["java","-jar","/app/spring-boot-application.jar"]


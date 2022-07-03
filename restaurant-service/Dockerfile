FROM --platform=linux/arm64/v8 arm64v8/openjdk
RUN mkdir /app
WORKDIR /app
COPY build/libs/restaurant-service-latest.jar ./spring-boot-application.jar
ENTRYPOINT ["java","-jar","/app/spring-boot-application.jar"]


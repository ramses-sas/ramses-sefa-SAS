# SAEFA - A Self Adaptive eFood App
MSc final thesis project by Vincenzo Riccio and Giancarlo Sorrentino.

## Project architecture and structure

![Component Diagram](./documents/Component%20Diagram.png)

The project is made of multiple microservices, which can be found in the directories named _***-service**_.

1. `eureka-registry-service` - the **Eureka Registry Service** that the other microservices contact to register their availability, giving their name and IP address. Those information are also made available to the API Gateway service, in order to decouple the services from the machines they are run on.
2. `api-gateway-service` - the **API Gateway Service** is the microservice acting as an API Gateway for the other microservices according to the homonym design pattern.
3. `restaurant-service` - the **Restaurant Service** is the microservice used to search for restaurants and their menu, and to send each order to the respective restaurant.
4. `ordering-service` - the **Ordering Service** is the service used to create and process the order made from a user.
5. `delivery-proxy-service` - the **Delivery Proxy** is the service acting as a bridge to the 3rd party service used for the delivery of the orders, according to the _proxy_ design pattern.
6. `payment-proxy-service` - the **Payment Proxy** is the service acting as a bridge to the 3rd party service used to process the payment of the orders, according to the _proxy_ design pattern.

Furthermore, there are additional directories containing utilities for the execution:

1.  `client-rest` - a REST client made to simulate a complete interaction with the software
2.  `*-service-restapi` - directories containing the models for the request and response of the RESTful API endpoints for each service.


## Environment setup
Each microservice can be run in 2 different ways: 
- as a traditional Java application from a `.jar` artifact
- in a **Docker** container _(recommended way)_

### Execution as a Java application
Each microservice can be executed as an independent Java application from a `.jar` file. The `.jar` file can be obtained running `../gradlew build` in the microservice directory and can be found under `./build/libs/*-latest.jar`.

### Execution in a Docker container
Each microservice is provided with a `Dockerfile` providing the directives for the container, which requires the `.jar` file to execute. The `.jar` file can be obtained running `../gradlew build` in the microservice directory.

However, since the creation of the container and its configuration can be tricky, each microservice is provided with 2 bash scripts to create and run each container.

- `dockerBuild.sh` – it builds and creates the container for the microservice.
- `dockerRun.sh` – it run the microservice in the just created container.

To look at the configuration options of the scripts, run them with the `-h` option (e.g., `./dockerBuild.sh -h`). 

To execute all the microservices locally, with the exception of the Eureka Service, run the `setup.sh` bash script in the project root folder.

### Simple execution flow
1. Clone this GitHub repository
2. Navigate to the `eureka-registry-service` folder and run `./dockerBuild.sh; ./dockerRun.sh`
3. Make sure that the machine can be reached from the Internet and take note of the public IP address. You may need to enable port forwarding on the gateway for port 8761.
4. Run `export EUREKA_IP_PORT=public_ip_of_eureka:8761` and `./setup.sh` from the project root folder. This will create and run a container for each microservice.
5. The API Gateway (i.e., the REST API exposed by all the services) can be reached on the port 58080 of the hosting machine.


## Default deployment settings
1. the **Eureka Registry Service** is run by default on port 8761. This is the only service requiring a static IP address that the other microservices must know. That address, together with its port, must be specified as value of the key `EUREKA_IP_PORT` in the `application.properties` file of each microservice or set as an environmental variable (as before, with name `EUREKA_IP_PORT`).
2. the **API Gateway Service** is run by default on port 58080.
3. the **Restaurant Service** is run by default on port 58081.
4. the **Ordering Service** is run by default on port 58085.
5. the **Delivery Proxy** is run by default on port ???.
6. the **Payment Proxy** is run by default on port ???.


## REST API Documentation
Once the project is up and running, the REST API documentation is available under `/swagger-ui/index.html` of the API Gateway URL.
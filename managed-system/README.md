# SAEFA - A Self Adaptive eFood App
MSc final thesis project by Vincenzo Riccio and Giancarlo Sorrentino.

## Project architecture and structure

![Component Diagram](./documents/Component%20Diagram.png)

The project is made of multiple microservices, which can be found in the directories named _***-service**_.

1. `eureka-registry-service` - the **Eureka Registry Service** that the other microservices contact to register their availability, giving their name and IP address. Those information are also made available to the API Gateway service, in order to decouple the services from the machines they are run on.
2. `api-gateway-service` - the **API Gateway Service** is the microservice acting as API Gateway for the other microservices according to the homonym design pattern.
3. `web-service` - the **Web Service** is the microservice acting as web frontend for the project.
4. `restaurant-service` - the **Restaurant Service** is the microservice used to search for restaurants and their menu, and to send each order to the respective restaurant.
5. `ordering-service` - the **Ordering Service** is the service used to create and process the order made from a user.
6. `delivery-proxy-service` - the **Delivery Proxy** is the service acting as a bridge to the 3rd party service used for the delivery of the orders, according to the _proxy_ design pattern.
7. `payment-proxy-service` - the **Payment Proxy** is the service acting as a bridge to the 3rd party service used to process the payment of the orders, according to the _proxy_ design pattern.

Furthermore, there are additional directories containing utilities for the execution:

1.  `client-rest` - a REST client made to simulate a complete interaction with the software
2.  `services-restapi` - directories containing the models for the request and response of the RESTful API endpoints for each service.


## Environment setup
Each microservice can be run in 2 different ways: 
- as a traditional Java application from a `.jar` artifact
- in a **Docker** container _(recommended way)_

### Execution as a Java application
Each microservice can be executed as an independent Java application from a `.jar` file. The `.jar` file can be obtained running `../gradlew build` in the microservice directory and can be found under `./build/libs/SERVICE_NAME-latest.jar`.

### Execution in a Docker container
Each microservice is provided with a `Dockerfile` providing the directives for the container, which requires the `.jar` file to execute. The `.jar` file can be obtained running `../gradlew build` in the microservice directory.

However, since the creation of the container and its configuration can be tricky, it can be automated using 2 bash scripts which create and run each container.

- `dockerBuild.sh` – it builds and creates the container for the microservice.
- `dockerRun.sh` – it run the microservice in the just created container.

To use them, navigate to the microservice main directory and run `bash ../scripts/dockerXXX.sh`.

To look at the configuration options of the scripts, run them with the `-h` option (e.g., `dockerBuild -h`). If no option is specified, the microservice is run with the following configuration:
- The port exposed is the one specified in the `application.properties` file
- The container image is a `linux/arm64/v8`
- The instance IP is the public IP address of the host, obtained through an external service. The host must be reachable from that address (i.e., you might need to enable port forwarding).
- The Eureka Registry address is the one specified in the `application.properties` file

To execute all the microservices locally, run the `dockerRunAll.sh` bash script in the project root folder. If no option is specified, it uses the default Eureka Service made public for this project. Otherwise, run it with the `-e` option to run also the Eureka Service locally. 

### Simple execution flow - default Eureka Service
It requires that the machines hosting the microservices are publicly accessible from the Internet at the respective microservices ports. Hence, you may need to enable port forwarding on the gateway. **DO NOT USE THIS METHOD IF CONDITIONS ARE NOT MET.**

1. Clone this GitHub repository
2. Navigate to the project root directory
3. Run `bash dockerRunAll.sh`. This will create and run a container for each microservice.
4. The Web App can be reached at `localhost:58080`.
5. The API Gateway (i.e., the REST API exposed by all the services) can be reached at `localhost:58081`.


### Simple execution flow (services running locally)
Useful for testing all on the same machine.
1. Clone this GitHub repository
2. Navigate to the project root directory
3. Run `bash dockerRunAll.sh -e`. This will create and run a container for each microservice.
4. The Web App can be reached at `localhost:58080`.
5. The API Gateway (i.e., the REST API exposed by all the services) can be reached at `localhost:58081`.


## Default deployment settings
1. the **Web Service** is run by default on port 58080.
2. the **API Gateway Service** is run by default on port 58081.
3. the **Eureka Registry Service** is run by default on port 58082. This is the only service requiring a fixed IP address that the other microservices must know. That address, together with its port, must be specified as value of the key `EUREKA_IP_PORT` in the `application.properties` file of each microservice or set as an environmental variable (as before, with name `EUREKA_IP_PORT`).
4. the **Restaurant Service** is run by default on port 58085.
5. the **Ordering Service** is run by default on port 58086.
6. the **Payment Proxy** is run by default on port 58087.
7. the **Delivery Proxy** is run by default on port 58088.


## REST API Documentation
Once the project is up and running, the REST API documentation of each microservice is available under `/api.html` of the microservice URL .

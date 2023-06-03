# SEFA - SErvice-based eFood Application
SEFA is a microservice application used as Managed System for the MSc final thesis project *Engineering of Self-Adaptive Systems: the case of RAMSES* by Vincenzo Riccio and Giancarlo Sorrentino.

## Project architecture and structure

![Component Diagram](../documents/Managed%20System/HexagonComponentDiagram.png)

The project is made of multiple microservices, which can be grouped in **servers** and **functional services**.

The servers can be found under the [servers](./servers/) directory:
1. `eureka-registry-server` - the **Netflix Eureka Registry Service** that the microservices instances contact to register their availability, giving their name and IP address. Those information are also made available to the API Gateway service, in order to decouple the services from the machines they are run on.
1. `config-server` - the **Spring Config Server** that the instances of the functional services contact to retrieve the most up-to-date configuration. Runtime changes of the configurations are monitored by this microservice and propagated to the services that need to be updated.

The functional services are:
1. `api-gateway-service` - the **API Gateway Service** is the microservice acting as API Gateway for the other microservices according to the homonym design pattern.
1. `web-service` - the **Web Service** is the microservice acting as web frontend for the project.
1. `restaurant-service` - the **Restaurant Service** is the microservice used to search for restaurants and their menu, and to send each order to the respective restaurant.
1. `ordering-service` - the **Ordering Service** is the service used to create and process the order made from a user.
1. `delivery-proxy-X-service` - the **Delivery Proxy** is the service acting as a bridge to the 3rd party service used for the delivery of the orders, according to the _proxy_ design pattern. There is one delivery proxy per 3rd party delivery service.
1. `payment-proxy-X-service` - the **Payment Proxy** is the service acting as a bridge to the 3rd party service used to process the payment of the orders, according to the _proxy_ design pattern. There is one payment proxy per 3rd party payment service.

Furthermore, a [REST client](./rest-client/) is provided to act as load generator and to simulate complete interactions with the software.


## Environment setup
Each microservice is available as a Docker image on [Docker Hub](https://hub.docker.com/u/sbi98). Use the [installation script](../SEFA%2BRAMSES_setup.sh) for pulling and running all the microservices.. 

### General rules for running a container
Move to the main readme and Create this section. Explain how to run the containers (docker pull, docker create network, ...)


## Default deployment settings
The **Eureka Registry Service** is the only service requiring a fixed IP address that the other microservices must know. That address, together with its port, must be specified as value of the key `EUREKA_IP_PORT` in the `application.properties` file of each microservice or set as an environmental variable (as before, with name `EUREKA_IP_PORT`).

By default, the microservices expose one port, according to the following table:

|      Microservice       |       Port      |
|     :-------------:     | :-------------: |
|       Web Service       |      58080      |
|   API Gateway Service   |      58081      |
| Eureka Registry Service |      58082      |
|                         |                 |
|    Restaurant Service   |      58085      |
|     Ordering Service    |      58086      |
|                         |                 |
|     Payment Proxy 1     |      58090      |
|     Payment Proxy 2     |      58091      |
|     Payment Proxy 3     |      58092      |
|                         |                 |
|     Delivery Proxy 1    |      58095      |
|     Delivery Proxy 2    |      58096      |
|     Delivery Proxy 3    |      58097      |

The simulated third-party services are hosted on Vercel and they are available at the following URLs:

|         Service                                                                                  |
|     :-------------:                                                                              |
|    [Payment Service 1](https://payment-service-ramses.vercel.app/1)    |
|    [Payment Service 2](https://payment-service-ramses.vercel.app/2)    |
|    [Payment Service 3](https://payment-service-ramses.vercel.app/3)    |
|                                                                                                  |
|    [Delivery Service 1](https://delivery-service-ramses.vercel.app/1)   |
|    [Delivery Service 2](https://delivery-service-ramses.vercel.app/2)   |
|    [Delivery Service 3](https://delivery-service-ramses.vercel.app/3)   |


## EXTRA

### Create and run the Docker containers (only if you changed the codebase)
Each microservice is provided with a `Dockerfile` providing the directives for the container, which requires the artifact of the service to be under the `build/libs/***-latest.jar`. The `.jar` file can be obtained running `../gradlew build` in the microservice directory.

However, since the creation of the container and its configuration can be tricky, it can be automated using 2 bash scripts which create and run each container.

- `dockerBuild.sh` – it builds and creates the container for the microservice.
- `dockerRun.sh` – it run the microservice in the just created container.

To use them, navigate to the microservice main directory and run `bash ../scripts/dockerXXX.sh`.

To look at the configuration options of the scripts, run them with the `-h` option (e.g., `dockerBuild -h`). If no option is specified, the microservice is run with the following configuration:
- The port exposed is the one specified in the `application.properties` file
- The container image is a `linux/arm64/v8`
- Assume that all the needed services (e.g., the _Eureka Service Registry_, the _Config Server_, ...) are run on the same machine (i.e., they are reachable using the private IP address of the machine).

To execute all the microservices locally, run the [dockerRunAll.sh](./dockerRunAll.sh) bash script in the project root folder. If no option is specified, it runs everything locally. Run with the `-h` option to see the script help.

### REST API Documentation
Once the project is up and running, the REST API documentation of each microservice is available under `/api.html` of the microservice URL.

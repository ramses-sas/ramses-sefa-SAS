# RAMSES - A Reusable Autonomic Manager for MicroServicES
RAMSES is a reusable Managing System developed as part of the MSc final thesis project *Engineering of Self-Adaptive Systems: the case of RAMSES* by Vincenzo Riccio and Giancarlo Sorrentino.

RAMSES implements a microservice-based MAPE-K loop, and it has been designed to ease the (re)use of the system by a user who wants to adapt a preexisting service-based application. It is made of configurable and extendable components, whose behaviour does not depend on the specific Managed System to be adapted. It is implemented in Java using the Spring Boot and Spring Cloud frameworks. 

The design of RAMSES was driven by the adaptation scenarios described in the following table.

|         Scenario        |       Observable properties      |    Example of adaptation    |
|     :-------------      | :-------------  | :-------------  |
|  S1: Violation of QoS specifications  |  Values of the QoS indicators of the service over time (e.g., availability, average response time)  |  – Change the current service implementation<br/>– Add (load balanced) instances in parallel<br/>– Shutdown of an instance with low performance<br/>– Change configuration properties |
| S2: Service unavailable | Success or failure of each service invocation | – Change the current service implementation<br/>– Add instances in parallel |
| S3: Better service implementation available | Properties of the service implementations | Change the current service implementation |

## Managing System description

### Architecture
The architecture is represented in the figure below.

![Architecture Diagram](../documents/Managing%20System/ManagingArch.png)

### (External) Probe and Actuator
As anticipated before, the main characteristic of RAMSES is being reusable with different service-based Managed Systems. To enforce this feature, RAMSES is designed to interact with the Managed System via two components: a Probe component, that allows RAMSES to retrieve all the relevant data from the Managed System, and an Actuator component, that allows RAMSES to effectively perform operations on the system. These components must be provided together with the Managed System itself, and must offer a specific set of APIs, defined by RAMSES.

### Knowledge
The Knowledge component is the one holding the system model. For this reason, it is the source of truth for the entire loop. Indeed, the other loop components interact with the Knowledge to maintain and retrieve an up-to-date runtime model of the system.

### Monitor
At each iteration, the Monitor component queries the Probe component provided by the Managed System, asking to perform a snapshot of all the instances of each service. Each snapshot contains statistics about the resource usage of the instance (e.g., CPU and disk usage), about the processed HTTP requests (e.g., response time, number of errors) and about the circuit breakers, if any. <br/>
The Monitor routine runs periodically and asynchronously with respect to the rest of the loop: it accumulates all the snapshots in a temporally ordered buffer, and it stores them in the knowledge-base as soon as a new loop iteration starts.

### Analyse
The Analyse routine starts as soon as it is notified by the Monitor component. First of all, it analyses the status of all the instances of each managed service. If an instance is considered suitable for the next steps, the latest snapshots are processed in order to compute a new value for each QoS property.<br/>
If undesired behaviours are detected during the analysis of a service, RAMSES may impose some **forced** adaptation options, that will be applied in any case at the end of the current loop iteration. Otherwise, for each QoS indicator, their latest values are processed in order to determine whether a service requires adaptation. If so, the Analyse component proposes some adaptation options, that will be evaluated during the Plan stage.<br/>
For each service implementation *s*, the current RAMSES implementation includes four different types of adaptation options:
- the *Add Instance* option, which represents the action of adding a new instance of *s*;
- the *Shutdown Instance* option, which represents the action of shutting down the specific instance it refers to;
- the *Change Implementation* option, which represents the action of replacing the instances of *s* with instances of the service implementation specified by the option;
- the *Change Load Balancer Weights* option, which, for a service balanced using a *fitness proportionate selection* algorithm, represents the action of redistributing the weights associated with all the running instances of *s*.

Future versions of RAMSES may extend this list by including new adaptation options.

### Plan
The Plan routine starts as soon as it is notified by the Analyse component. For each managed service, if there is at least one forced option, the non-forced ones are discarded, while all the forced ones are directly chosen. Conversely, all the options are processed and compared to extract the one estimated to bring more benefits to the service it refers to.<br/>
The benefit of each option is computed by estimating the value that each QoS indicator is expected to have after applying option. When a service is load balanced using a *fitness proportionate selection* algorithm, the weights of the instances of the service involved are modified depending on the option to apply:
- when an instance should be added, the Plan assigns a fraction of the total weight to the new instance, resizing the weight of the other instances;
- when an instance should be shut down, the Plan equally redistributes its weight among the other instances;
- when the service implementation should be changed, the Plan equally splits the total weight between the instances of the new service implementation;
- when the weights of the running instances should be changed, the Plan redistributes the instance weights by solving a mixed integer linear programming (M-ILP) optimization problem.

### Execute
The Execute routine starts as soon as it is notified by the Plan component. For each adaptation option chosen by the Plan component, according to its type, the Execute contacts the Actuator component to effectively apply the changes required by the considered adaptation option.<br/>
The Execute component, and consequently RAMSES itself, assumes that all the operations requested to the Actuator are eventually executed, and that all the changes of service configurations are performed within a reasonably short amount of time.

### Workflow
The workflow is represented in the figure below.

![Workflow Sequence Diagram](../documents/Managing%20System/Sequence/ManagingSequenceDiagram.png)

## Environment setup
Each microservice can be run as a traditional Java application from a `.jar` artifact. The `.jar` file can be obtained running `../gradlew build` in the microservice directory and can be found under `./build/libs/COMPONENT_NAME-latest.jar`.

By default, the microservices expose one port, according to the following table:

|  Microservice |       Port      |
| :-----------: | :-------------: |
|   Dashboard   |      58000      |
|    Monitor    |      58001      |
|    Analyse    |      58002      |
|     Plan      |      58003      |
|    Execute    |      58004      |
|   Knowledge   |      58005      |

Check the `application.properties` files of each microservice for the configurable properties.

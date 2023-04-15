# RAMSES - A Reusable Autonomic Manager for MicroServicES
MSc final thesis project by Vincenzo Riccio and Giancarlo Sorrentino.

## Project composition

This project is a Self-Adaptive System made of:

1. a [managed system](./managed-system/README.md), SEFA, which is the software object of adaptation.
2. a reusable [managing system](./managing-system/README.md), RAMSES, which is the software responsible of adapting the managed system.
3. some simulated [third party services](./third-party-services/README.md) used by the managed system

## Software Architecture
The high-level software architecture is represented below.

![High-level architecture](./documents/Managed%20System/Managing%2BManaged.png)

## Installation guide
Together with the actual code of both RAMSES and SEFA, we also provide a set of ready-to-use docker images. By following the next steps, you can set up and run both systems on the same machine. 

To begin with, install [Docker](https://www.docker.com/) on your machine and run it. After the installation, we suggest adopting the following settings:
- CPU: 6
- Memory: 8GB
- Swap: 1GB

The next step involves the creation of a GitHub repository (if you don’t have one yet) to be used by the Managed System Configuration Server as the configuration repository. You can do so by forking [our repository](https://github.com/ramses-sas/config-server). Check that the application.properties file does not include any load balancer weight. If so, simply delete those lines and push on your repository. Once you have created your configuration repository, create an environmental variable storing its URL by running the following command, after replacing ``<YOUR_REPO_URL>`` with the URL of the repository you just created:
```
$ export GITHUB_REPOSITORY_URL=<YOUR_REPO_URL>
```

Now, generate a GitHub personal access token to grant the Managed System the permission to push data on your repository. You can do so by following [this guide](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/creating-a-personal-access-token).
Once again, create an environmental variable storing your access token by running the following command, after replacing ``<YOUR_TOKEN>`` with the token you just created:
```
$ export GITHUB_OAUTH=<YOUR_TOKEN> 
```

Finally, run the file ``SEFA_setup.sh`` if you want to run RAMSES together with SEFA. Otherwise, run the file ``SMS_setup.sh`` to run RAMSES together with the Simple Managed System.
Use option `-a` to specify the system architecture. The available ones are amd64 and arm64. The latter is the default one.

## Usage Guide
Once all the containers have been launched you can start interacting with both systems. 

To easily interact with SEFA you can open your browser and go to the url exposed by the sefa-web-service container, which is visible in docker. 

![Docker Container Example](./documents/Docker%20Container%20Example.png)

From there, you can interact with the app both as an admin, by adding and editing restaurants, or as a user, by placing orders. 

To interact with the RAMSES dashboard, open your browser and go to the url exposed by the ramses-dashboard container. From there, you can see all the managed services in the Home page and all the applied adaptation options in the Adaptation page, and you can modify the systems hyperparameters from the Configuration page. Notice that the monitor routine must be started manually from the Configuration page, and the adaptation process can be manually enabled or disabled from the same page.

From the Homepage you can track the availability and the average response time of each service and of their instances. Notice that these values are available only if new requests are made to the services. To generate artificial requests to SEFA, you can use our automatic load generator. If you did not launch it when asked by the setup script, you can instanciate it by running again the same script with the `-l` flag.






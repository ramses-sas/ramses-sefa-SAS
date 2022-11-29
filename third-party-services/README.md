# SEFA - A SErvice-based eFood Application
MSc final thesis project by Vincenzo Riccio and Giancarlo Sorrentino.

## Third party services

### Payment Service
Three payment services are simulated. 

Given that `cvv` is the integer value of the CVV of a credit/debit card used to pay an order, they return `HTTP.200` with a text response after waiting `cvv/10` seconds.
If `cvv` is `0`, they return `HTTP.500` with a text response to simulate a server error.

They run in the port range `58090-58092` by default.

To ease the development, they are also hosted in AWS Lambdas, which can be reached at the following URLs:

|         Service                                                                                  |
|     :-------------:                                                                              |
|    [Payment Service 1](https://xmximxmorpjwponbk2qp72p33i0fekhf.lambda-url.eu-west-1.on.aws/)    |
|    [Payment Service 2](https://guzsl5ufk2ztqhlxdlbatl7dzy0vaani.lambda-url.eu-west-1.on.aws/)    |
|    [Payment Service 3](https://aajv6txnny7uuycydk5lignrpy0fbrnq.lambda-url.eu-west-1.on.aws/)    |


### Delivery Service
Three delivery services are simulated. 

Given that `number` is the integer value of the street number of the delivery address of an order, they return `HTTP.200` with a text response after waiting `number/10` seconds.
If `number` is `0`, they return `HTTP.500` with a text response to simulate a server error.

They run in the port range `58095-58097` by default.

To ease the development, they are also hosted in AWS Lambdas, which can be reached at the following URLs:

|         Service                                                                                  |
|     :-------------:                                                                              |
|    [Delivery Service 1](https://x5xycqlbgwhi72r5lo2nwevrw40tomhi.lambda-url.eu-west-1.on.aws/)   |
|    [Delivery Service 2](https://63lm7vm6mhuunmisf3ym3kmtpq0rsxll.lambda-url.eu-west-1.on.aws/)   |
|    [Delivery Service 3](https://b7435mgabew2hf67gupfyyy2fe0tdmlj.lambda-url.eu-west-1.on.aws/)   |

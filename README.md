# Data Commandr REST Service

This service exposes functionality as REST interface.

# Build and Deploy

The following steps have to be performed: 
* Set the necessary parameters either in application.properties or in start.bat:
  * log file and log level
  * session timeout and age
  * port number (this port has to be be used by the web application)
* Open the port on the server where this service will run (Windows or Linux)
* Build the project by executing gradle build 
* Start server by using the file in build/libs: java -jar sc-rest-0.1.0.jar

# History

* 2016-09-05, v0.1.0. First working version with simple arithmetic formulas.

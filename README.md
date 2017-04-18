# DataCommandr REST Service

This service exposes functionality of the DataCommandr engine as a HTTP REST service.

# Build

The following steps have to be performed: 
* Build the project with the DataCommandr core library.
* Set the necessary parameters either in application.properties or in start.bat:
  * log file and log level
  * session timeout and age
  * port number (this port has to be be used by the web application)
* Build the project by executing gradle build 

# Deploy and Run

* In the firewall, open the port on the server where this service will run (Windows or Linux)
* Start web server by using the file in build/libs: java -jar sc-rest-0.2.0.jar

# Change Log

A list of changes for each release can be found in the UI project.

* v0.6.0 (2017-04-xx)
* v0.5.0 (2017-03-19)
* v0.4.0 (2017-02-12)
* v0.3.0 (2017-01-22)
* v0.2.0 (2016-12-10)
* v0.1.0 (2016-09-05)

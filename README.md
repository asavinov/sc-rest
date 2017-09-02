# Stream Commandr REST Service

This service exposes functionality of the Stream Commandr engine as a HTTP REST service.

# Build

The following steps have to be performed: 
* Build the project with the Stream Commandr core library.
* Set the necessary parameters either in application.properties or in start.bat:
  * log file and log level
  * session timeout and age
  * port number (this port has to be be used by the web application)
* Build the project by executing gradle build 

# Deploy and Run

* In the firewall, open the port on the server where this service will run.
  * Windows Server. Go to Server Manager, Menu Tools, Windows Firewall and Advanced Security, Inbound Rules.
  * Linux.
  * Azure. Go to Azure Portal and open Network Security Group (NSG) node used by VM. Open ports in Inbound Rules section. 
* Install necessary software:
  * Linux:
    * sudo apt-get update
    * sudo apt-get install default-jre
    * sudo apt-get install nginx
* Copy:    
  * Linux:
    * /var/www/html - application
    * /etc/nginx/sites-available/default - nginx configuration with default html files
* Start services:    
  * Linux:
    * sudo service nginx [start|stop|restart|status]
    * nohup java -jar /web/server.jar &
    * ps -ef | grep  server

# Change Log

A list of changes for each release can be found in the UI project.

* v0.7.0 (2017-09-02)
* v0.6.0 (2017-05-14)
* v0.5.0 (2017-03-19)
* v0.4.0 (2017-02-12)
* v0.3.0 (2017-01-22)
* v0.2.0 (2016-12-10)
* v0.1.0 (2016-09-05)

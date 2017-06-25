#
# https://spring.io/guides/gs/spring-boot-docker/
#
FROM frolvlad/alpine-oraclejdk8:slim
VOLUME /tmp
ADD sc-rest-0.6.0.jar app.jar
RUN sh -c 'touch /app.jar'
ENV JAVA_OPTS=""
ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app.jar" ]

#
# https://stackoverflow.com/questions/35061746/run-jar-file-in-docker-image
#
#FROM anapsix/alpine-java
#MAINTAINER myNAME 
#COPY sc-rest-0.6.0.jar app.jar /home/sc-rest-0.6.0.jar app.jar
#CMD ["java","-jar","/home/testprj-1.0-SNAPSHOT.jar"]

#EXPOSE 8080

#
# https://runnable.com/docker/java/dockerize-your-java-application
#
#FROM  phusion/baseimage:0.9.17
#MAINTAINER  Alexandr Savinov <savinov@conceptoriented.org>
#RUN echo "deb http://archive.ubuntu.com/ubuntu trusty main universe" > /etc/apt/sources.list
#RUN apt-get -y update
#...

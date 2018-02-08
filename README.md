# Document Smart Highlights

## Version

${project.version}

## Introduction

## Package/Folders Description

## Installation

### Pre-requisites

* Java 1.8

* Maven 3.3.9

* MongoDB 3.4 (windows 10)

* MongoDB 3.6.2 (Ubuntu 16.04LTS)

* RabbitMQ 3.6.14 (windows 10)

* RabbitMQ 3.7.2-1 (Ubuntu 16.04LTS)

* Tomcat 8.0.X

### Installing/Building the Application

#### Java

#### Maven

#### MongoDB

##### Windows

1. Install MongoDB using the instructions at [this link](https://docs.mongodb.com/v3.4/tutorial/install-mongodb-on-windows/)
2. Enable security following general guidelines at [this link](https://medium.com/@raj_adroit/mongodb-enable-authentication-enable-access-control-e8a75a26d332)
3. Start MongoDB:

   ```
   "C:\Program Files\MongoDB\Server\3.4\bin\mongod.exe"
   ```
5. In another prompt connect to MongoDB

   ```
   "C:\Program Files\MongoDB\Server\3.4\bin\mongo.exe"
   ```
4. Create super user

   ```
   $ use admin
   $ db.createUser(
   {
     user: "superAdmin",
     pwd: "[your admin password]",
     roles: [ { role: "root", db: "admin" } ]
    })   
   ```
5. Disconnect and re-connect at MongoDB as super user:

   ```
   connect-mongo-super-user.bat [your admin password]
   ```
6. Create user access (readWrite) for specific dsh database

   ```
   $ use dsh
   $ db.createUser(
     {
      user: "dshuser",
      pwd: "[your password]",
      roles: [ "readWrite"]
     })   
   ```
7. Disconnect and re-connect at MongoDB as specific user:

   ```
   connect-mongo.bat [your dshuser passoword]
   ```
   
##### Linux Ubuntu 16.04LTS

1. Install MongoDB following the instructions at [https://docs.mongodb.com/manual/tutorial/install-mongodb-on-ubuntu/](https://docs.mongodb.com/manual/tutorial/install-mongodb-on-ubuntu/)
2. Enable security following general guidelines at [this link](https://medium.com/@raj_adroit/mongodb-enable-authentication-enable-access-control-e8a75a26d332)
3. Start MongoDB service

   ```
   sudo service mongod start
   ```
4. In another prompt connect to MongoDB

   ```
   mongo --host 127.0.0.1:27017
   ```
5. Create super user

   ```
   $ use admin
   $ db.createUser(
   {
     user: "superAdmin",
     pwd: "[your admin password]",
     roles: [ { role: "root", db: "admin" } ]
    })   
   ```
6. Disconnect and re-connect at MongoDB as super user:

   ```
   ./connect-mongo-super-user.sh [your admin password]
   ```
7. Create user access (readWrite) for specific dsh database

   ```
   $ use dsh
   $ db.createUser(
     {
      user: "dshuser",
      pwd: "[your password]",
      roles: [ "readWrite"]
     })   
   ```

8. Disconnect and re-connect at MongoDB as specific user:

   ```
   ./connect-mongo.sh [your dshuser passoword]
   ```

#### RabitMQ

##### Windows

1. Follow the instructions at [http://www.rabbitmq.com/install-windows.html](http://www.rabbitmq.com/install-windows.html)
2. Enable the ports mentioned at the link above at the firewall.
3. Enable the management plugin:

   ```
    rabbitmq-plugins.bat enable rabbitmq_management
	rabbitmq-service.bat stop  
    rabbitmq-service.bat remove	
	rabbitmq-service.bat install  
	rabbitmq-service.bat start   
   ``` 
3. Test it with `http://localhost:15672/mgmt`. User: guest. Password: guest.

##### Linux Ubuntu 16.04LTS

1. Follow the instructions at [https://www.rabbitmq.com/install-debian.html](https://www.rabbitmq.com/install-debian.html)
   1. As Ubunt has a 3.5.x version it is better to download the .deb for version 3.7.x from link above
   1. Or follow the instructions at the link and add RabbitMQ Ubuntu repositories before to run the `apt-get install`.
2. Enable the management plugin:

   ```
    sudo rabbitmq-plugins enable rabbitmq_management
	service rabbitmq-server stop  
	service rabbitmq-server start
   ``` 
3. Test it with `http://localhost:15672/mgmt`. User: guest. Password: guest.
 
#### Building From Sources

1. In order to build, both MongoDB and RabbitMQ services should be running. 
2. Maven development user settings should be correctly configured (see configuration
   section below)
3. At the root dsh folder type:

```
mvn clean install

```
#### Tomcat

##### Windows

1. Download Tomcat 8.0.X 32-bit/64-bit Windows Service Installer at 
   [https://tomcat.apache.org/download-80.cgi](https://tomcat.apache.org/download-80.cgi). 
   This will install Tomcat as a windows service.
2. Start Tomcat windows service using windows services application.
3. Look at the address: http://localhost:8080 

##### Linux Ubuntu 16.04LSTS

1. Download Tomcat 8.0.x .zip or .tar.gz file at 
   [https://tomcat.apache.org/download-80.cgi](https://tomcat.apache.org/download-80.cgi). 
2. Unpack the contents on a folder.
3. Go to the `bin` folder and type `./startup.sh`.
4. Look at the address: http://localhost:8080 

## Configuration

### MongoDB Access Properties

Edit or create the maven user `settings.xml` file typically at `$HOME/.m2` (or `%HOMEPATH%\.m2` 
at windows) folder and add a default activated profile similar to this:

```xml
<profile>
	<id>development-properties</id>
	<activation>
		<activeByDefault>true</activeByDefault>
	</activation>
	<properties>
		<mongo.host>localhost</mongo.host>
		<mongo.port>27017</mongo.port>
		<mongo.user>dshuser</mongo.user>
		<mongo.password>[password you have configured in the steps above when installing mongo]</mongo.password>
	</properties>
</profile>
```

Or add the `properties` section at any default activated profile already present at `settings.xml` file.

### Tomcat Admin User Configuration

Stop Tomcat if it is already started, and edit the file `TOMCAT_HOME/conf/tomcat-users.xml`.
If the `tomcat-users` tag is empty or with all elements commented, add the following 
content inside the `<tomcat-users>` tag.

```xml
	<role rolename="tomcat" />  
	<role rolename="manager-gui" />  
	<role rolename="manager-script" />  
	<role rolename="admin-gui" />  
				  
	<user username="admin" password="[your admin password]" roles="tomcat,manager-gui,manager-script,admin-gui" />
	<user username="tomcat" password="[your tomcat user password]" roles="tomcat,manager-gui,manager-script,admin-gui" />
```
Replace the admin and tomcat's password with any desired password. 

## Usage

### Running Application from Eclipse Embedded Tomcat

The module `DSH-rest-api` is a web application. The type tag in pom.xml file is .war. Thus the
first step is to install a Tomcat (8.0.X) at 
[https://tomcat.apache.org/download-80.cgi](https://tomcat.apache.org/download-80.cgi). 
After that, if you have Eclipse Oxygen JEE version correctly installed and configured, 
then is just a matter of showing the Servers view and adding a new server. At eclipse menu, 
follow the path: `Window -> Show View -> Other -> Servers -> Server`. When the `Servers` 
view opens, add a new Tomcat Server. You will need to have a Tomcat already installed at your 
system, since eclipse will ask for an installed Tomcat root directory. When creating a new server
inside eclipse, it will show the DSH-rest-api as a potential project to be installed in that server.

After having the DSH-rest-api inside the server, configure the server `startup` and `shutdown` 
timeouts to something like 120s each.

Start the server and access the application swagger UI at: `http://localhost:8080/DSH-rest-api/swagger-ui.html`. 

### Using Application .war File on a Servlet Container

After the build, the folder DSH-rest-api/target should have a file named DSH-rest-api-[version].war. 
That war file can be dropped to a servlet container to be used as a web application. At this moment 
the server having the servlet container should be the same having MongoDB and RabbitMQ installed, up 
and running.

#### Using Tomcat Application Manager

Access the Tomcat's manager usually at the address `http://localhost:8080/manager/html`. 
The browser will ask for user and password. Enter the user and password configured at the 
`TOMCAT_HOME/conf/tomcat-users.xml` (see the configuration section above).

After login, at the Deploy section, fulfill the fields:

```
Context Path: 	DSH-rest-api
WAR or Directory URL:	[absolute path to the generated DSH-rest-api-<version>.war file] 
```
You can also upload the war file from `DSH-rest-api/target` folder, using the `Choose File` button
at the Tomcat's manager application. However, in this case, it is recommenDed
to rename the file `DSH-rest-api-<version>.war` to just `DSH-rest-api.war` just to not have the
version name associated with the web application, which will then be used to access the application
at the web browser.

Start the server and access the application swagger UI at: `http://localhost:8080/DSH-rest-api/swagger-ui.html`.

#### Just Dropping Application .war File

Rename the file `DSH-rest-api/target/DSH-rest-api-<version>.war` to `DSH-rest-api.war` and
drop it at Tomcat's `webapps` folder. Restart Tomcat if needed.

Start the server and access the application swagger UI at: `http://localhost:8080/DSH-rest-api/swagger-ui.html`.

### Running Application Using Spring Boot Maven Plugin

Go to `DSH-rest-api` module root folder project, by using `cd DSH-rest-api`  at the sources root, and run:

```
mvn spring-boot:run
```
Wait until the application boots up. Typically when the following output is present:

```
.
.
.
08:37:47.665 [main] INFO  o.s.c.s.DefaultLifecycleProcessor - Starting beans in phase 2147483647
08:37:47.665 [main] INFO  s.d.s.w.p.DocumentationPluginsBootstrapper - Context refreshed
08:37:47.702 [main] INFO  s.d.s.w.p.DocumentationPluginsBootstrapper - Found 1 custom documentation plugin(s)
08:37:47.746 [main] INFO  s.d.s.w.s.ApiListingReferenceScanner - Scanning for api listing references
08:37:47.962 [main] INFO  o.a.coyote.http11.Http11NioProtocol - Initializing ProtocolHandler ["http-nio-8080"]
08:37:47.979 [main] INFO  o.a.coyote.http11.Http11NioProtocol - Starting ProtocolHandler ["http-nio-8080"]
08:37:47.984 [main] INFO  o.a.tomcat.util.net.NioSelectorPool - Using a shared selector for servlet write/read
08:37:48.016 [main] INFO  o.s.b.w.e.tomcat.TomcatWebServer - Tomcat started on port(s): 8080 (http)
08:37:48.022 [main] INFO  c.m.dsh.restapi.DshRestApplication - Started DshRestApplication in 9.385 seconds (JVM running for 18.084)
08:37:48.025 [main] INFO  c.m.dsh.restapi.DshRestApplication - Main application run!!!
```
Start the server and access the application swagger UI at: `http://localhost:8080/swagger-ui.html`.

### Swagger User Interface

Swagger UI has two methods for a document resource:

* submit: uploads a PDF file and returns a token.

* status: use the token returned in the first method to ask for the document processing status. At the moment
  the only status would be `QUEUED_FOR_INDEXING_SUCCESS`.
  
![Swagger UI](/src/site/resources/images/swagger-ui.png)

In order to use the methods, click on the method and after in the `Try it out` button (firstly for the submit method).

A new form will open with the fields to fulfill. In case of submit you will need to choose a file to upload and
inform its title. In case of status, you just needs to enter the the token returned by the previous method.

## Release Notes

${current.release.issues}

## Release History

${older.releases.issues}

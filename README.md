# Document Smart Highlights

## Version

${project.version}

## Introduction

## Package/Folders Description

## Installation

### Pre-requisites

* MongoDB 3.4 (windows 10)

* MongoDB 3.6.2 (Ubuntu 16.04LTS)

* RabbitMQ 3.6.14 (windows 10)

* RabbitMQ 3.7.2-1 (Ubuntu 16.04LTS)

### Installing/Building the Application

#### MongoDB

##### Windows

1. Install Mongo using the instructions at [this link](https://docs.mongodb.com/v3.4/tutorial/install-mongodb-on-windows/)
2. Enable security following general guidelines at [this link](https://medium.com/@raj_adroit/mongodb-enable-authentication-enable-access-control-e8a75a26d332)
3. Start mongo:

   ```
   "C:\Program Files\MongoDB\Server\3.4\bin\mongod.exe"
   ```
5. In another prompt connect to mongo

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
5. Disconnect and re-connect at mongo as super user:

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
   
7. Disconnect and re-connect at mongo as specific user:

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
4. In another prompt connect to mongo

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
6. Disconnect and re-connect at mongo as super user:

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
   
8. Disconnect and re-connect at mongo as specific user:

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
 

## Configuration

### Mongo DB Access Properties

Edit the maven user `settings.xml` file typically at `$HOME/.m2` (or `%HOMEPATH%\.m2` at windows) folder and
add a default activated profile similar to this:

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

Or add the properties at any default activated profile already present at `settings.xml` file.

## Usage

## Release Notes

${current.release.issues}

## Release History

${older.releases.issues}

# Document Smart Highlights

![Coverage](https://mriss-projects.github.io/dsh/${release.type}/products/dsh/dsh-coverage-report/badges/jacoco.svg)

## Version

${project.build.version}

## Introduction

Welcome to **DSH - Document Smart Highlights**. Document Smart Highlights aims to provide web
services that accept PDF and HTML files and return a list of keywords and the most relevant
sentences, extracted with NLP techniques; the sentences through automatic document summarization.
More details at the [wiki](https://github.com/MRISS-Projects/dsh/wiki).

**What 0.3.0 does today.** It accepts a PDF document, stores it in a NoSQL store, puts its id on a
queue for indexing, and reports the document's processing status. Nothing consumes that queue
yet, so 0.3.0 does not index documents and does not extract keywords or sentences.

This project is distributed as source code; no binary distribution is provided yet. Building it
produces the REST API as an executable Spring Boot jar, which runs on its own, with no separate
server to install.

The goals for this project are basically two:

1. Implement an NLP based system to extract relevant information from PDF files in a potentially
   scalable fashion, using a NoSQL store and queues. This infrastructure stores requests and chains
   a workflow of operations (workers) that extract keywords and relevant sentences, all provided as
   a REST API.

2. Learn concepts of NLP associated with scaleable cloud based REST API building. The
technology used is java based, so as another goal here we can mention the build
learning process using [Spring Framework](http://spring.io/) in order to build the
API and infrastructure.

As part of the goals is learning about technologies, comments and contributions are
welcome. However, this is not a final product, application or concept. Just a
point for experimentation and proofing.

If anyone out there is interested in contribute or apply this project on a more
product-oriented environment, please get in touch through the email:
marcelo.riss@gmail.com.

Wiki: https://github.com/MRISS-Projects/dsh/wiki

Project Development Documentation: https://mriss-projects.github.io/dsh-docs/

## Package/Folders Description

* **dsh-test-dataset**: the PDF files used as fixtures by automated tests.
* **dsh-data**: the data models, **Document**, **Keyword** and **Sentence**, and the
  [workflow](https://github.com/MRISS-Projects/dsh/wiki/Workflow) of statuses a document moves
  through while it is processed. Also the MongoDB persistence of documents.
* **dsh-rest-api**: the application. A Spring Boot jar exposing the REST API: document submission
  and processing status. Querying the results (keywords and relevant sentences) is not available
  yet.
* **dsh-solr**: two Solr plugins, a numbers filter and a term vector component that orders terms,
  and a `solrconfig.xml`. Nothing in 0.3.0 runs Solr, and replacing it is proposed in
  [ADR-001](https://github.com/MRISS-Projects/dsh/blob/DEVELOP/specs/architecture/ADR-001-GCP-based-components.md).
* **dsh-doc-indexer-worker**: a Spring Boot application holding only its main class so far. Its job
  will be to take a document id off the indexing queue, read the document from the database, send
  it to the indexer, and split its text into paragraphs, sentences and terms.
* **dsh-doc-analyser**: a parent module grouping the analysis modules below.
  * **dsh-doc-processor-worker**: an empty Spring Boot application for now; the plan is for it to
    take indexed documents off a queue and run both extractors on them.
  * **dsh-keyword-extractor**: not implemented yet. It will score each term of a document with
    combinations of TF/IDF and return the best ranked terms as keywords.
  * **dsh-top-sentences-extractor**: not implemented yet; planned to rank sentences with
    [automatic summarization](https://en.wikipedia.org/wiki/Automatic_summarization) techniques
    and return the most relevant ones.
* **dsh-coverage-report**: aggregates the test coverage of every module into one report and the
  coverage badge.

## Installation

### Pre-requisites

* Java 17 (Temurin)

* Maven 3.9.16

* MongoDB 3.4 (windows 10)

* MongoDB 4.0.6 (Ubuntu 18.04 LTS)

* RabbitMQ 3.6.14 (windows 10)

* RabbitMQ 3.7.14 (Ubuntu 18.04 LTS)

* Tomcat 8.0.X

### Installing/Building the Application

#### Java

##### Download and Installation

 1. Download an Eclipse Temurin JDK 17 from [https://adoptium.net/temurin/releases/?version=17](https://adoptium.net/temurin/releases/?version=17).
    1. **IMPORTANT NOTE**: Download and install a **JDK, not a JRE**. On the
       download page select Version **17 (LTS)**, your operating system and
       architecture, and the **JDK** package type.
             
 2. Windows
    1. There should be a .msi windows installer. Just follow the
       instructions.
       
 3. Linux	
    1. Download the .tar.gz file. After downloading it, uncompress it at a folder of your preference.
    2. Create a link. Open a command prompt, go to the JDK parent folder (the folder where you extract JDK into), and type:
    
       ```
       # replace jdk-17.0.20.1+1 with the directory the archive extracted to
       ln -s jdk-17.0.20.1+1 java
       ```

##### Setting environment variables

###### Linux

1. Open the file `/home/[YOUR_USER]/.profile`. This file might be hidden.
   If it does not appear at your home folder, using the file explorer, type
   `Ctrl+h`. Go to the end of the file and add:
       
    ```
    export JAVA_HOME=/your/jdk/parent/folder/java
   export PATH=$JAVA_HOME/bin:$PATH
   ```

###### Windows 

1. Open Control Panel go to System, Advanced system settings, `Environment
   Variables` button.     
2. At the System Variables section, click New.
3. Set JAVA_HOME and point to the root of JDK folder.
4. Search for the variable named <<Path>> in the list, click on it and press 
   Edit.
5. Prepend the value with:
 
   ```
   %JAVA_HOME%\bin;
   ```

##### Verifying the installation

1. Open a command prompt and type:
 
   ```
   java -version
   ```
2. The result should be something like:

    ```
   openjdk version "17.0.20.1" 2026-08-18
   OpenJDK Runtime Environment Temurin-17.0.20.1+1 (build 17.0.20.1+1)
   OpenJDK 64-Bit Server VM Temurin-17.0.20.1+1 (build 17.0.20.1+1, mixed mode, sharing)
   ```

#### Maven

1. Download maven **3.9.16** from [https://archive.apache.org/dist/maven/maven-3/3.9.16/binaries/apache-maven-3.9.16-bin.zip](https://archive.apache.org/dist/maven/maven-3/3.9.16/binaries/apache-maven-3.9.16-bin.zip)    
2. Unzip it on a folder of your preference
3. Set environment variables.
   1. Linux   
      1. Put it at your `$HOME/.profile` file
             
         ```
         export M2_HOME=/path/to/where/you/extracted/maven/apache-maven-3.9.16
         export PATH=$M2_HOME/bin:$PATH
         export MAVEN_OPTS='-Xmx1024m'
         ```    
      2. If you already have java set up, your `.profile`, it 
         should look like this: 

         ```
         export JAVA_HOME=/your/jdk/parent/folder/java
         export M2_HOME=/path/to/where/you/extracted/maven/apache-maven-3.9.16
         export PATH=$JAVA_HOME/bin:$M2_HOME/bin:$PATH
         export MAVEN_OPTS='-Xmx1024m'
         ```
      3. Logout and login again.            
      4. Test by opening a terminal and typing:
             
         ```
         mvn -version
         ```
      5. The result should be similar to:
             
         ```
         Apache Maven 3.9.16 (2bdd9fddda4b155ebf8000e807eb73fd829a51d5)
         Maven home: /home/[YOUR_USER]/apps/apache-maven-3.9.16
         Java version: 17.0.20.1, vendor: Eclipse Adoptium, runtime: /home/[YOUR_USER]/apps/jdk-17.0.20.1+1
         Default locale: en_US, platform encoding: UTF-8
         OS name: "linux", version: "6.8.0-45-generic", arch: "amd64", family: "unix"
         ```
    2. Windows       
       1. Open Control Panel go to System, Advanced system settings, **Environment Variables** button.
       2. At the System Variables section, click New.
       3. Set `M2_HOME` and point to the root of maven folder.
       4. Search for the variable `Path` in the list, click on it and press Edit.
       5. Put the maven bin folder right after java home:
             
          ```
          %JAVA_HOME%\bin;%M2_HOME%\bin;
          ```   
       6. Set the variable `MAVEN_OPTS`.
			 
          ```
          MAVEN_OPTS=-Xmx1024m
          ```	 
       7. The result should be similar to:
             
          ```
          Apache Maven 3.9.16 (2bdd9fddda4b155ebf8000e807eb73fd829a51d5)
          Maven home: C:\data\apache-maven-3.9.16
          Java version: 17.0.20.1, vendor: Eclipse Adoptium, runtime: C:\Program Files\Eclipse Adoptium\jdk-17.0.20.1+1
          Default locale: en_US, platform encoding: Cp1252
          OS name: "windows 11", version: "10.0", arch: "amd64", family: "windows"
          ```

#### MongoDB

##### Windows

1. Install MongoDB using the instructions at [this link](https://docs.mongodb.com/v3.4/tutorial/install-mongodb-on-windows/)
2. Enable security following general guidelines at [this link](https://medium.com/@raj_adroit/mongodb-enable-authentication-enable-access-control-e8a75a26d332)
3. Start MongoDB:

    ```
    "C:\Program Files\MongoDB\Server\3.4\bin\mongod.exe"
    ```
4. In another prompt connect to MongoDB

   ```
   "C:\Program Files\MongoDB\Server\3.4\bin\mongo.exe"
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
   connect-mongo-super-user.bat [your admin password]
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
   connect-mongo.bat [your dshuser passoword]
   ```
   
##### Linux Ubuntu 18.04 LTS

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
4. Test it with `http://localhost:15672/mgmt`. User: guest. Password: guest.

##### Linux Ubuntu 18.04 LTS

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
2. Maven development user settings should be correctly configured — see
   [Maven Settings for GitHub Packages](#maven-settings-for-github-packages) below, which is
   what lets Maven resolve the parent POM `com.mriss.mriss-parent:products`.
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

##### Linux Ubuntu 19.04 LSTS

1. Download Tomcat 8.0.x .zip or .tar.gz file at 
   [https://tomcat.apache.org/download-80.cgi](https://tomcat.apache.org/download-80.cgi). 
2. Unpack the contents on a folder.
3. Go to the `bin` folder and type `./startup.sh`.
4. Look at the address: http://localhost:8080 

## Configuration

### Maven Settings for GitHub Packages

The parent POM `com.mriss.mriss-parent:products` is published to GitHub Packages, so no build
of this repository resolves until your `~/.m2/settings.xml` authenticates against that registry.
Three server ids and one property are needed — the same four things that
`.github/workflows/ci.yml` writes into its own `settings.xml`:

| Entry | Used for |
| --- | --- |
| server `MRISS-Projects-maven-repo` | resolving the parent POM and other MRISS artifacts |
| server `MRISS-Projects-maven-repo-plugins` | resolving MRISS Maven plugins |
| server `github.com` | `maven-scm-plugin`, when a `-Ddeployment` build commits the regenerated `README.md` |
| property `github.personal.token` | `maven-changes-plugin`, which reads the closed milestone issues that go into the generated `README.md`. It is a Maven property, not a server credential — the `readme-generation` profile in `parent-poms` wires it into the plugin's `personalToken` parameter — so **omitting it fails silently**: `failOnError` is `false`, the issue list is never produced, and the generated `README.md` comes out with an empty Release Notes section |

The credential is one and the same classic personal access token, carrying `read:packages` and
nothing else, issued from an account with read access to the `MRISS-Projects` organisation. This
repository is public, so no issue-reading scope is required on top. Paste it into all four places
in your own `~/.m2/settings.xml`; it is never committed to this repository. The CI side of the same
token is described under Secrets in `docs/devops/README.md`.

```xml
<settings>
  <servers>
    <server>
      <id>MRISS-Projects-maven-repo</id>
      <username>YOUR-GITHUB-USERNAME</username>
      <password>YOUR-PACKAGES-READ-TOKEN</password>
    </server>
    <server>
      <id>MRISS-Projects-maven-repo-plugins</id>
      <username>YOUR-GITHUB-USERNAME</username>
      <password>YOUR-PACKAGES-READ-TOKEN</password>
    </server>
    <server>
      <id>github.com</id>
      <username>YOUR-GITHUB-USERNAME</username>
      <password>YOUR-PACKAGES-READ-TOKEN</password>
    </server>
  </servers>
  <profiles>
    <profile>
      <id>github-packages</id>
      <properties>
        <github.personal.token>YOUR-PACKAGES-READ-TOKEN</github.personal.token>
      </properties>
      <repositories>
        <repository>
          <id>MRISS-Projects-maven-repo</id>
          <url>https://maven.pkg.github.com/MRISS-Projects/maven-repo</url>
          <releases><enabled>true</enabled></releases>
          <snapshots><enabled>true</enabled></snapshots>
        </repository>
      </repositories>
      <pluginRepositories>
        <pluginRepository>
          <id>MRISS-Projects-maven-repo-plugins</id>
          <url>https://maven.pkg.github.com/MRISS-Projects/maven-repo</url>
          <releases><enabled>true</enabled></releases>
          <snapshots><enabled>true</enabled></snapshots>
        </pluginRepository>
      </pluginRepositories>
    </profile>
  </profiles>
  <activeProfiles>
    <activeProfile>github-packages</activeProfile>
  </activeProfiles>
</settings>
```

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

The application authenticates to MongoDB as `mongo.user`, against the `dsh` database, through a
connection string: `mongodb://<user>:<password>@<host>:<port>/dsh`. A password containing `@`, `:`,
`/`, `?`, `#` or `%` must be percent-encoded in `mongo.password` (`@` as `%40`, for example), or the
connection string breaks. Any value can be overridden at runtime, e.g. `--mongo.port=27018`.

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

The module `dsh-rest-api` is a web application. The type tag in pom.xml file is .war. Thus the
first step is to install a Tomcat (8.0.X) at 
[https://tomcat.apache.org/download-80.cgi](https://tomcat.apache.org/download-80.cgi). 
After that, if you have Eclipse Oxygen JEE version correctly installed and configured, 
then is just a matter of showing the Servers view and adding a new server. At eclipse menu, 
follow the path: `Window -> Show View -> Other -> Servers -> Server`. When the `Servers` 
view opens, add a new Tomcat Server. You will need to have a Tomcat already installed at your 
system, since eclipse will ask for an installed Tomcat root directory. When creating a new server
inside eclipse, it will show the dsh-rest-api as a potential project to be installed in that server.

After having the dsh-rest-api inside the server, configure the server `startup` and `shutdown` 
timeouts to something like 120s each.

Start the server and access the application swagger UI at: `http://localhost:8080/dsh-rest-api/swagger-ui.html`. 

### Using Application .war File on a Servlet Container

After the build, the folder dsh-rest-api/target should have a file named dsh-rest-api-[version].war. 
That war file can be dropped to a servlet container to be used as a web application. At this moment 
the server having the servlet container should be the same having MongoDB and RabbitMQ installed, up 
and running.

#### Using Tomcat Application Manager

Access the Tomcat's manager usually at the address `http://localhost:8080/manager/html`. 
The browser will ask for user and password. Enter the user and password configured at the 
`TOMCAT_HOME/conf/tomcat-users.xml` (see the configuration section above).

After login, at the Deploy section, fulfill the fields:

```
Context Path: 	dsh-rest-api
WAR or Directory URL:	[absolute path to the generated dsh-rest-api-<version>.war file] 
```
You can also upload the war file from `dsh-rest-api/target` folder, using the `Choose File` button
at the Tomcat's manager application. However, in this case, it is recommenDed
to rename the file `dsh-rest-api-<version>.war` to just `dsh-rest-api.war` just to not have the
version name associated with the web application, which will then be used to access the application
at the web browser.

Start the server and access the application swagger UI at: `http://localhost:8080/dsh-rest-api/swagger-ui.html`.

#### Just Dropping Application .war File

Rename the file `dsh-rest-api/target/dsh-rest-api-<version>.war` to `dsh-rest-api.war` and
drop it at Tomcat's `webapps` folder. Restart Tomcat if needed.

Start the server and access the application swagger UI at: `http://localhost:8080/dsh-rest-api/swagger-ui.html`.

### Running Application Using Spring Boot Maven Plugin

Go to `dsh-rest-api` module root folder project, by using `cd dsh-rest-api`  at the sources root, and run:

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
  
![Swagger UI](/src/site/resources/images/swagger-ui.jpg)

In order to use the methods, click on the method and after in the `Try it out` button (firstly for the submit method).

A new form will open with the fields to fulfill. In case of submit you will need to choose a file to upload and
inform its title. In case of `status`, you just needs to enter the the token returned by the previous `submit` method call.

## Release Notes

${issues.text.list}

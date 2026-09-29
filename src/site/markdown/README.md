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
* Docker, to run MongoDB and RabbitMQ, and for the integration build
* MongoDB 6, run from the `mongo:6` image
* RabbitMQ 3, run from the `rabbitmq:3` image

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

#### MongoDB and RabbitMQ

Run both from the images the integration build is verified against:

```bash
docker run -d --name dsh-mongo -p 127.0.0.1:27017:27017 mongo:6
docker run -d --name dsh-rabbitmq -p 127.0.0.1:5672:5672 rabbitmq:3
```

MongoDB takes a few seconds to start. Then create the user the application connects to it as, with
read and write access to the `dsh` database. Choose your own password:

```bash
docker exec dsh-mongo mongosh dsh --quiet --eval \
  'db.createUser({user: "dshuser", pwd: "YOUR-DSH-PASSWORD", roles: [{role: "readWrite", db: "dsh"}]})'
```

RabbitMQ needs no set-up: the application connects as its default `guest` user. Stop both with
`docker stop dsh-mongo dsh-rabbitmq`, and start them again with `docker start`; the MongoDB user
lives in the container, so it survives a restart but not a `docker rm`.

#### Building From Sources

1. Configure Maven's user settings as described in
   [Maven Settings for GitHub Packages](#maven-settings-for-github-packages) below, which is what
   lets Maven resolve the parent POM `com.mriss.mriss-parent:products`.
2. At the root dsh folder type:

   ```bash
   mvn clean install
   ```

This runs the unit tests only, and needs neither MongoDB nor RabbitMQ running. The integration
tests run with `mvn clean install -DintegrationTests`, which starts its own MongoDB and RabbitMQ
containers, so it needs a running Docker daemon.

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
    <mongo.password>[the password you chose when creating the MongoDB user]</mongo.password>
  </properties>
</profile>
```

Or add the `properties` section at any default activated profile already present at `settings.xml` file.

The application authenticates to MongoDB as `mongo.user`, against the `dsh` database, through a
connection string: `mongodb://<user>:<password>@<host>:<port>/dsh`. A password containing `@`, `:`,
`/`, `?`, `#` or `%` must be percent-encoded in `mongo.password` (`@` as `%40`, for example), or the
connection string breaks. Any value can be overridden at runtime, e.g. `--mongo.port=27018`.

## Usage

With MongoDB and RabbitMQ running and the build done, start the application in either of two ways.

### Running the Jar

```bash
java -jar dsh-rest-api/target/dsh-rest-api-<version>.jar
```

The MongoDB connection comes from the `mongo.*` properties in your Maven settings (see
[MongoDB Access Properties](#mongodb-access-properties)), resolved when the jar was built. Any of
them can be overridden when starting it, as can the RabbitMQ address:

```bash
java -jar dsh-rest-api/target/dsh-rest-api-<version>.jar \
  --mongo.host=localhost --mongo.port=27017 \
  --mongo.user=dshuser --mongo.password=YOUR-DSH-PASSWORD \
  --spring.rabbitmq.host=localhost --spring.rabbitmq.port=5672
```

### Running with the Spring Boot Maven Plugin

From the `dsh-rest-api` folder:

```bash
mvn spring-boot:run
```

Either way, the application is up when the log shows a line like:

```text
c.m.dsh.restapi.DshRestApplication - Started DshRestApplication in 9.385 seconds
```

### Calling the API

The REST API has the two operations of the document resource:

* **submit**, `POST /v1/dsh/document/submit`: uploads a PDF file (`contents`) with a `title`, and
  returns a token. If the submission fails, the token is `ERROR` and the message says why.
* **status**, `GET /v1/dsh/document/status/{token}`: returns the processing status of the document
  the token was issued for. Right after a submit it can show `QUEUED_FOR_INDEXING` for a moment;
  in 0.3.0 a document then ends at `QUEUED_FOR_INDEXING_SUCCESS`: stored, and its id queued for
  indexing. It shows `QUEUED_FOR_INDEXING_ERROR` if the id could not be queued, and an unknown
  token returns `TOKEN_NOT_FOUND`. No later status is reached until the indexing worker exists.

Submit a PDF file, then ask for its status with the token the submit returned:

```bash
curl -F title="My document" -F contents=@/path/to/file.pdf \
  http://localhost:8080/v1/dsh/document/submit
curl http://localhost:8080/v1/dsh/document/status/<token>
```

The API description, in Swagger 2 format, is served at
`http://localhost:8080/v2/api-docs?group=dsh-app`. The Swagger UI is not served in 0.3.0; see
[#144](https://github.com/MRISS-Projects/dsh/issues/144).

## Release Notes

${issues.text.list}

# Document Smart Highlights

## Version

${project.version}

## Introduction

## Package/Folders Description

## Installation

### Pre-requisites

* MongoDB 3.4

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

##### Linux

TBD.

## Configuration

## Usage

## Release Notes

${current.release.issues}

## Release History

${older.releases.issues}

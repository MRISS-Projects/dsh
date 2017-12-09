# Document Smart Highlights

## Version

${project.version}

## Introduction

## Package/Folders Description

## Installation

### Pre-requisites

### Installing/Building the Application

#### MongoDB

##### Windows

1. Install Mongo using the instructions at [this link](https://docs.mongodb.com/v3.4/tutorial/install-mongodb-on-windows/)
2. Enable security following general guidelines at [this link](https://medium.com/@raj_adroit/mongodb-enable-authentication-enable-access-control-e8a75a26d332)

   ```
   $ use admin
   $ db.createUser(
   {
     user: "superAdmin",
     pwd: "[your admin password]",
     roles: [ { role: "root", db: "admin" } ]
    })   
   ```
   
##### Linux

TBD.

## Configuration

## Usage

## Release Notes

${current.release.issues}

## Release History

${older.releases.issues}

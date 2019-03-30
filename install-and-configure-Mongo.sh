#!/bin/bash

#echo "deb [ arch=amd64,arm64 ] https://repo.mongodb.org/apt/ubuntu xenial/mongodb-org/4.0 multiverse" | sudo tee /etc/apt/sources.list.d/mongodb-org-4.0.list
#sudo apt-get update && sudo apt-get install -y mongodb-org

cat /etc/mongod.conf
service mongod status
mongo --host localhost --eval 'db.createUser( { user: "superAdmin", pwd: "superAdmin01", roles: [ { role: "root", db: "admin" } ] } )' admin
mongo --host localhost -u superAdmin -p superAdmin01 --authenticationDatabase 'admin' --eval 'db.createUser({user:"dshuser",pwd:"dsh01!",roles:[ "readWrite"]})' dsh

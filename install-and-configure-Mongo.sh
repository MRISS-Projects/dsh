#!/bin/bash

#echo "deb [ arch=amd64,arm64 ] https://repo.mongodb.org/apt/ubuntu xenial/mongodb-org/4.0 multiverse" | sudo tee /etc/apt/sources.list.d/mongodb-org-4.0.list
#sudo apt-get update && sudo apt-get install -y mongodb-org

cat /etc/mongod.conf

sudo echo "\nsecurity:\n authorization: enabled" >> /etc/mongod.conf

sudo service mongod start

mongo --eval "use admin; db.createUser( { user: \"superAdmin\", pwd: \"superAdmin01\", roles: [ { role: \"root\", db: \"admin\" } ] } )"
mongo -u superAdmin -p superAdmin01 --eval "use dsh; db.createUser( { user: \"dshuser\", pwd: \"dsh01!\", roles: [ \"readWrite\"] } )"

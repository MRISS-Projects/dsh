#!/bin/bash

sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 9DA31620334BD75D9DCB49F368818C72E52529D4 && echo "deb [ arch=amd64 ] https://repo.mongodb.org/apt/ubuntu bionic/mongodb-org/4.0 multiverse" | sudo tee /etc/apt/sources.list.d/mongodb-org-4.0.list
sudo apt-get update && sudo apt-get install -y mongodb-org

sudo echo "\nsecurity:\n authorization: enabled"

sudo service mongod start

mongo --eval "use admin; db.createUser( { user: \"superAdmin\", pwd: \"superAdmin01\", roles: [ { role: \"root\", db: \"admin\" } ] } )"
mongo -u superAdmin -p superAdmin01 --eval "use dsh; db.createUser( { user: \"dshuser\", pwd: \"dsh01!\", roles: [ \"readWrite\"] } )"

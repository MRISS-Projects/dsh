#!/bin/bash

#echo "deb [ arch=amd64,arm64 ] https://repo.mongodb.org/apt/ubuntu xenial/mongodb-org/4.0 multiverse" | sudo tee /etc/apt/sources.list.d/mongodb-org-4.0.list
#sudo apt-get update && sudo apt-get install -y mongodb-org

printf "\nsecurity:\n  authorization: enabled\n" >> /etc/mongod.conf
cat /etc/mongod.conf

echo "######### before mongo start ##########"
service mongod start
echo "######### after mongo start ##########"

mongo --eval "use admin; db.createUser( { user: \"superAdmin\", pwd: \"superAdmin01\", roles: [ { role: \"root\", db: \"admin\" } ] } )"
mongo -u superAdmin -p superAdmin01 --eval "use dsh; db.createUser( { user: \"dshuser\", pwd: \"dsh01!\", roles: [ \"readWrite\"] } )"

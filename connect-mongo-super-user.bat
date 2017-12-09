@echo off

"C:\Program Files\MongoDB\Server\3.4\bin\mongod.exe" --port 27017 -u "superAdmin" -p "$1" --authenticationDatabase "admin"
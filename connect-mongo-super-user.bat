@echo off

"C:\Program Files\MongoDB\Server\3.4\bin\mongo.exe" --port 27017 -u "admin" -p "%1" --authenticationDatabase "admin"
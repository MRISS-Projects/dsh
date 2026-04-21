#!/bin/bash

mongo --host 127.0.0.1:27017 -u "superAdmin" -p "$1" --authenticationDatabase "admin"

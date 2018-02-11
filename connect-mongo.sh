#!/bin/bash

mongo --host 127.0.0.1:27017 -u "dshuser" -p "$1" --authenticationDatabase "dsh"

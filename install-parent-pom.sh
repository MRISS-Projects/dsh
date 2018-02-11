#!/bin/bash

current_dir=`pwd`
cp parent-pom.xml /tmp
cd /tmp
mvn install:install-file -Dfile=parent-pom.xml -DpomFile=parent-pom.xml
cd $current_dir

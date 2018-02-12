#!/bin/bash

current_dir=`pwd`
cp parent-pom.xml /tmp
mkdir -p /tmp/src/site
cp ./src/site-desc/site.xml /tmp/src/site
cd /tmp
mvn -Dfile=parent-pom.xml -DpomFile=parent-pom.xml install:install-file
mvn -f parent-pom.xml site:attach-descriptor
cd $current_dir

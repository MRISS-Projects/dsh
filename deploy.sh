#!/bin/bash

mvn -gs ~/apps/maven/conf/empty-settings.xml -Ddeployment -DskipTests clean deploy && mvn -gs ~/apps/maven/conf/empty-settings.xml -Ddeployment site-deploy

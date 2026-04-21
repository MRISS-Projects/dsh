#!/bin/bash

mvn -gs ~/apps/maven/conf/empty-settings.xml -P deployment -DskipTests clean deploy && mvn -gs ~/apps/maven/conf/empty-settings.xml -P deployment site-deploy

#!/bin/bash

set -e

chmod 755 *.sh

./install-parent-pom.sh

cp maven/settings-security.xml $HOME/.m2
cp maven/settings-security.xml $HOME/.settings-security.xml

git checkout ${TRAVIS_BRANCH}

mvn -B -s settings.xml -P deployment,process-badges -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn -Drelease.type=rcs clean install && mvn -B -s settings.xml -P deployment -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn -Drelease.type=rcs site-deploy

rm -rf settings.xml
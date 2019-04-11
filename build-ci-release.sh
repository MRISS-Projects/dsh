#!/bin/bash

set -e

chmod 755 *.sh

./install-parent-pom.sh

cp maven/settings-security.xml $HOME/.m2
cp maven/settings-security.xml $HOME/.settings-security.xml

git checkout ${TRAVIS_BRANCH}

# pre-release
./pre-release-script.sh

# release
#mvn -B -s settings.xml -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn release:clean
#mvn -B -s settings.xml -P release-prepare -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn release:prepare
#mvn -B -s settings.xml -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn release:perform

#cd target/checkout
#mvn -N --batch-mode exec:exec
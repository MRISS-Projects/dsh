#!/bin/bash

set -e

chmod 755 *.sh

./install-parent-pom.sh

cp maven/settings-security.xml $HOME/.m2
cp maven/settings-security.xml $HOME/.settings-security.xml

echo Travis Pull Request: ${TRAVIS_PULL_REQUEST}
if [ "${TRAVIS_PULL_REQUEST}" != "false" ]; then
  echo This is a PR. Just testing ...
  mvn -B -s settings.xml -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn clean install
else
  git checkout ${TRAVIS_BRANCH}
  if [ "${TRAVIS_BRANCH}" = "DEVELOP" ]; then
    echo This is an on-goging development version. Just deploying site ...
    mvn -B -s settings.xml -P deployment -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn clean install && mvn -s settings.xml -P deployment -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn site-deploy
    mvn -B -s settings.xml -P process-badges -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn -Drelease.type=rcs -f DSH-Coverage-Report/pom.xml process-resources
  else
    echo This is the master or a feature branch: ${TRAVIS_BRANCH}. Just testing ...
    mvn -B -s settings.xml -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn clean install
  fi
fi
rm -rf settings.xml
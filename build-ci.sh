#!/bin/bash

set -e

chmod 755 *.sh

./install-parent-pom.sh

cp maven/settings-security.xml $HOME/.m2
cp maven/settings-security.xml $HOME/.settings-security.xml

echo Travis Pull Request: ${TRAVIS_PULL_REQUEST}
if [ "${TRAVIS_PULL_REQUEST}" != "false" ]; then
  echo This is a PR. Just testing ...
  mvn -B -s settings.xml clean install
else
  git checkout ${TRAVIS_BRANCH}
  if [ "${TRAVIS_BRANCH}" = "DEVELOP" ]; then
    echo This is an on-goging development version. Just deploying site ...
    mvn -B -s settings.xml -P deployment clean install && mvn -s settings.xml -P deployment site-deploy
  else
    echo This is a feature branch. Just testing ...
    mvn -B -s settings.xml clean install
  fi
fi
rm -rf settings.xml
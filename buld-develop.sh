#!/bin/bash

set -e

./install-parent-pom.sh

cp maven/settings-security.xml $HOME/.m2
cp maven/settings-security.xml $HOME/.settings-security.xml

if [ "${TRAVIS_BRANCH}" = "DEVELOP" ]; then
  git checkout ${TRAVIS_BRANCH}
  echo This is an on-goging development version. Just deploying site ...
  mvn -s settings.xml -P deployment clean install && mvn -s settings.xml -P deployment site-deploy
fi
rm -rf settings.xml
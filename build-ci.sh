#!/bin/bash

set -e

./install-parent-pom.sh

cp maven/settings-security.xml $HOME/.m2
cp maven/settings-security.xml $HOME/.settings-security.xml

echo Travis Pull Request: ${TRAVIS_PULL_REQUEST}
if [ "${TRAVIS_PULL_REQUEST}" != "false" ]; then
  echo This is a PR. Just testing ...
  mvn -s settings.xml clean install
else
  git checkout ${TRAVIS_BRANCH}
  if [ "${TRAVIS_BRANCH}" = "master" ]; then
    echo This is master branch. Usual build ...
    mvn -s settings.xml clean install
  else
    echo This is a feature branch. Just testing ...
    mvn -s settings.xml clean install
  fi
fi
rm -rf settings.xml
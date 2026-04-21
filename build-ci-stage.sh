#!/bin/bash

set -e

chmod 755 *.sh

./install-parent-pom.sh

cp maven/settings-security.xml $HOME/.m2
cp maven/settings-security.xml $HOME/.settings-security.xml

git checkout ${TRAVIS_BRANCH}

# start release branch process
mvn -s settings.xml -DdevelopmentVersion=$NEXT_DEV_VERSION release:branch

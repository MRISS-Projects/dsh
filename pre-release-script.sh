#!/bin/bash

git fetch --tags origin +refs/heads/*:refs/remotes/origin/*
git branch --all
git merge --strategy=recursive --strategy-option=theirs ${BRANCH_TO_RELEASE}
if [ $? -ne 0 ]
then
    exit 1
else
    CURRENT_DIR=`pwd`
    echo Pushing contents of $CURRENT_DIR
    mvn -B -s settings.xml -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn -Dbasedir=$CURRENT_DIR -Dmessage="release message [skip travis]" scm:checkin
    if [ $? -ne 0 ]
    then
        exit 1
    else    
        BRANCH_NAME=`echo ${BRANCH_TO_RELEASE} | cut -c 8-`
        echo Tracking branch: ${BRANCH_NAME}
        git branch --all
        git checkout -b ${BRANCH_NAME}
    fi
fi
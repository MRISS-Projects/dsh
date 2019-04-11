#!/bin/bash

git fetch --tags origin +refs/heads/*:refs/remotes/origin/*
git branch --all
git merge --strategy=recursive --strategy-option=theirs ${BRANCH_TO_RELEASE}
if [ $? -ne 0 ]
then
    exit 1
else
    git push origin DEVELOP
    if [ $? -ne 0 ]
    then
        exit 1
    else    
        BRANCH_NAME=`echo ${BRANCH_TO_RELEASE} | cut -c 8-`
        echo Tracking branch: ${BRANCH_NAME}
        git checkout -b ${BRANCH_NAME} --track ${BRANCH_TO_RELEASE}
    fi
fi
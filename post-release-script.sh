!#/bin/bash

MAVEN_GENERATED_TAG=$1

git fetch --tags origin 
git checkout -b master --track origin/master
git reset --hard
git merge --strategy=recursive --strategy-option=theirs ${MAVEN_GENERATED_TAG}
if [ $? -ne 0 ]
then
    exit 1
else
    git push origin master
    mvn -N -P update-readme,process-badges clean process-resources
    #mvn -f DSH-Coverage-Report/pom.xml -P process-badges process-resources
    git push origin --delete `echo ${BRANCH_TO_RELEASE} | cut -c 8-`
fi
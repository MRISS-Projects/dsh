# init
./install-parent-pom.sh

# start release branch process
mvn -U -gs ~/apps/maven/conf/empty-settings.xml -DdevelopmentVersion=0.2.0-SNAPSHOT release:branch

# pre-release
git fetch --tags origin
git merge --strategy=recursive --strategy-option=theirs origin/staging-0.0.1-SNAPSHOT-RC
git push origin DEVELOP
git checkout -b staging-0.0.1-SNAPSHOT-RC --track origin/stating-0.0.1-SNAPSHOT-RC

# release
mvn -gs ~/apps/maven/conf/empty-settings.xml --batch-mode -Dsite.deployment.personal.main=file:///tmp/sites release:prepare
mvn -gs ~/apps/maven/conf/empty-settings.xml --batch-mode -Dsite.deployment.personal.main=file:///tmp/sites release:perform

# post-release
cd target/checkout
git fetch --tags origin 
git checkout -b master --track origin/master
git reset --hard
git merge --strategy=recursive --strategy-option=theirs dsh-0.0.1
git push origin master
mvn -N -P update-readme clean process-resources
git push origin --delete staging-0.0.1-SNAPSHOT-RC

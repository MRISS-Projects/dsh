#!/bin/bash

# DEPRECATED - do not use in new workflows.
#
# This installs com.mriss:mriss-parent:1.2.4 from the local parent-pom.xml.
# No module in this repository inherits from that artifact: the real parent is
# com.mriss.mriss-parent:products, resolved from GitHub Packages via settings.xml.
#
# Retained only because build-ci*.sh (Travis-era) still call it.
# Removal is tracked as a Wave 0 task in specs/product/PRD.md.

current_dir=`pwd`
cp parent-pom.xml /tmp
mkdir -p /tmp/src/site
cp ./src/site-desc/site.xml /tmp/src/site
cd /tmp
mvn -Dfile=parent-pom.xml -DpomFile=parent-pom.xml install:install-file
mvn -f parent-pom.xml site:attach-descriptor
cd $current_dir

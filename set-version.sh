#!/bin/bash

mvn --batch-mode -DdevelopmentVersion=$1 -DautoVersionSubmodules=true release:update-versions
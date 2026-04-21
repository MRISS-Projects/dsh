#!/bin/bash

mvn -Dsite.deployment.personal.main=file:///tmp clean site-deploy

#!/bin/bash

mvn -P deployment clean install && mvn -P deployment site-deploy

#!/bin/bash
# Re-versions the whole 13-module reactor in place. The local counterpart of what
# project-release.yml does in CI; nothing in CI calls this script.
#
# -Dproject.dev.<groupId>:<artifactId> is used rather than -DdevelopmentVersion because
# parent-poms/pom.xml binds <developmentVersion> to ${build.NEXT_DEVELOPMENT_VERSION};
# plugin configuration beats the -DdevelopmentVersion user property, so that flag is
# silently ignored here and the reactor is auto-incremented instead. The per-project
# property does override it. parent-poms' own set-version.sh uses the same mechanism.
set -euo pipefail

if [ $# -ne 1 ]; then
    echo "Usage: ${0##*/} <version>   e.g. ${0##*/} 0.4.0-SNAPSHOT" >&2
    exit 1
fi

mvn --batch-mode -DautoVersionSubmodules=true \
    "-Dproject.dev.com.mriss.products:dsh=$1" \
    release:update-versions

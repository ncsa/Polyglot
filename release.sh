#!/bin/bash

# on develop branch, release.sh will tag extactor with tag of develop and push to docker hub repos (you can specified the your own server)
# on master branch, you can specify a list of VERSION and release.sh will tag them and push to docker hub repos.
# on other branches, release.sh will not do docker tag and push.

# exit on error, with error code
set -e

# can use the following to push to isda-registry for testing:
# BRANCH="master" SERVER=isda-registry.ncsa.illinois.edu/ ./release.sh

# use DEBUG=echo ./release.sh to print all commands
export DEBUG=${DEBUG:-"echo"}

# use SERVER=XYZ/ to push to a different server
SERVER=${SERVER:-""}

# what branch are we on
BRANCH=${BRANCH:-"$(git rev-parse --abbrev-ref HEAD)"}

# make sure docker is build
$(dirname $0)/docker.sh

# find out the version
if [ "${BRANCH}" = "master" ]; then
    VERSION=${VERSION:-"2.4.0 2.4 2 latest"}
elif [ "${BRANCH}" = "develop" ]; then
    VERSION="develop"
else
    VERSION="develop"
    echo exit 0
fi

# tag all images and push if needed
for x in $( find ${PWD} -name Dockerfile ); do
    FOLDER=$( echo $x | sed 's#\(.*\)/Dockerfile#\1#' )
    NAME=${FOLDER##*/}

    for v in ${VERSION}; do
        if [ "$v" != "latest" -o "$SERVER" != "" ]; then
            ${DEBUG} docker tag clowder/${NAME}:latest ${SERVER}clowder/${NAME}:${v}
        fi
        ${DEBUG} docker push ${SERVER}clowder/${NAME}:${v}
        if [ "$v" != "latest" -o "$SERVER" != "" ]; then
            ${DEBUG} docker rmi ${SERVER}clowder/${NAME}:${v}
        fi
    done
    ${DEBUG} docker rmi clowder/${NAME}:latest
done

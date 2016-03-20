#!/bin/sh

# variables that can be set
# DEBUG   : set to echo to print command and not execute
# PUSH    : set to push to push, anthing else not to push. If not set
#           the program will push if master or develop.
# PROJECT : the project to add to the image, default is NCSA

#DEBUG=echo

# make sure PROJECT ends with /
PROJECT=${PROJECT:-"ncsa"}
if [ ! "${PROJECT}" = "" ]; then
  if [ ! "$( echo $PROJECT | tail -c 2)" = "/" ]; then
    REPO="${PROJECT}/polyglot-server"
  else
    REPO="polyglot-server"
  fi
else
  REPO="polyglot-server"
fi

# copy dist file to docker folder
ZIPFILE=$( /bin/ls -1rt target/universal/polyglot-*.zip 2>/dev/null | tail -1 )
if [ "$ZIPFILE" = "" ]; then
  echo "Running ./sbt dist"
  ./sbt dist
  ZIPFILE=$( /bin/ls -1rt target/universal/polyglot-*.zip 2>/dev/null | tail -1 )
  if [ "$ZIPFILE" = "" ]; then
    exit -1
  fi
fi
${DEBUG} rm -rf docker/files
${DEBUG} mkdir -p docker/files
${DEBUG} unzip -q -d docker ${ZIPFILE}
${DEBUG} mv docker/$( basename ${ZIPFILE} .zip ) docker/files/polyglot
${DEBUG} mkdir docker/files/polyglot/custom

# find version if we are develop/latest/release and if should be pushed
BRANCH="$(git rev-parse --abbrev-ref HEAD)"
VERSION=${VERSION:-""}
if [ "$VERSION" = "" ]; then
  if [ "$BRANCH" = "master" ]; then
    PUSH=${PUSH:-"push"}
    VERSION="latest"
  elif [ "$BRANCH" = "develop" ]; then
    PUSH=${PUSH:-"push"}
    VERSION="develop"
  elif [ "$( echo $BRANCH | sed -e 's#^release/.*$#release#')" = "release" ]; then
    PUSH=${PUSH:-"push"}
    VERSION="$( echo $BRANCH | sed -e 's#^release/\(.*\)$#\1#' )"
  else
    PUSH=${PUSH:-""}
  fi
else
  PUSH=${PUSH:-""}
fi

# create image using temp id
${DEBUG} docker build --pull --tag $$ docker
if [ $? -ne 0 ]; then
  echo "FAILED build of $1/Dockerfile"
  exit -1
fi

# tag all versions and push if need be
for v in $VERSION; do
  ${DEBUG} docker tag $$ ${REPO}:${v}
  if [ ! -z "$PUSH" -a ! "$PROJECT" = "" ]; then
    ${DEBUG} docker push ${REPO}:${v}
  fi
done

# cleanup
${DEBUG} docker rmi $$
${DEBUG} rm -rf docker/files

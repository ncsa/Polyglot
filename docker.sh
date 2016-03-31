#!/bin/sh

# variables that can be set
# DEBUG   : set to echo to print command and not execute
# PUSH    : set to push to push, anthing else not to push. If not set
#           the program will push if master or develop.
# PROJECT : the project to add to the image, default is NCSA

#DEBUG=echo

# make sure PROJECT ends with /
PROJECT=${PROJECT:-"ncsa"}

# copy dist file to docker folder
ZIPFILE=$( /bin/ls -1rt target/polyglot-*.zip 2>/dev/null | tail -1 )
if [ "$ZIPFILE" = "" ]; then
  echo "Running mvn package"
  mvn package
  ZIPFILE=$( /bin/ls -1rt target/polyglot-*.zip 2>/dev/null | tail -1 )
  if [ "$ZIPFILE" = "" ]; then
    exit -1
  fi
fi
FILES=docker.polyglot-server/files
${DEBUG} rm -rf ${FILES}
${DEBUG} mkdir -p ${FILES}
${DEBUG} unzip -q -d ${FILES} ${ZIPFILE}
${DEBUG} mv ${FILES}/$( basename ${ZIPFILE} -bin.zip ) ${FILES}/polyglot

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

for app in polyglot-server ss-imagemagick ss-htmldoc; do

    if [ ! "${PROJECT}" = "" ]; then
        if [ ! "$( echo $PROJECT | tail -c 2)" = "/" ]; then
            REPO="${PROJECT}/${app}"
        else
            REPO="${app}"
        fi
    else
        REPO="${app}"
    fi

    # create image using temp id
    ${DEBUG} docker build --pull --tag ${app}__$$ docker.${app}
    if [ $? -ne 0 ]; then
        echo "FAILED build of docker.${app}/Dockerfile"
        exit -1
    fi

    # tag all versions and push if need be
    for v in $VERSION; do
        ${DEBUG} docker tag ${app}__$$ ${REPO}:${v}
        if [ ! -z "$PUSH" -a ! "$PROJECT" = "" ]; then
            ${DEBUG} docker push ${REPO}:${v}
        fi
    done

    # cleanup
    ${DEBUG} docker rmi ${app}__$$
    ${DEBUG} rm -rf ${FILES}

done


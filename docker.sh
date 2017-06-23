#!/bin/sh

# variables that can be set
# DEBUG   : set to echo to print command and not execute
# PUSH    : set to push to push, anthing else not to push. If not set
#           the program will push if master or develop.
# PROJECT : the project to add to the image, default is NCSA

#DEBUG=echo

# make sure PROJECT ends with /
PROJECT=${PROJECT:-"bingzhang"}

# copy dist file to docker folder
ZIPFILE=$( /bin/ls -1rt target/polyglot-*.zip 2>/dev/null | tail -1 )

if [ "$ZIPFILE" = "" ]; then
  echo "Running mvn package"
  ${DEBUG} mvn package -Dmaven.test.skip=true -Dmaven.javadoc.skip=true
  ZIPFILE=$( /bin/ls -1rt target/polyglot-*.zip 2>/dev/null | tail -1 )

  if [ "$DEBUG" = "" -a "$ZIPFILE" = "" ]; then
    exit -1
  fi
fi

FILES=docker/polyglot/files
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

#for app in polyglot converters-imagemagick converters-htmldoc; do
for app in polyglot; do
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
    ${DEBUG} docker build --tag ${app}_$$ docker/${app}

    if [ $? -ne 0 ]; then
        echo "FAILED build of docker/${app}/Dockerfile"
        exit -1
    fi

    # Tag this polyglot image as ncsa/polyglot:latest, since the converters are based on it.
    if [ "$app" = "polyglot" ]; then
        ${DEBUG} docker tag ${app}_$$ ncsapolyglot/polyglot:latest
    fi

    # tag all versions and push if need be
    for v in $VERSION; do
        ${DEBUG} docker tag ${app}_$$ ${REPO}:${v}
        if [ ! -z "$PUSH" -a ! "$PROJECT" = "" ]; then
            ${DEBUG} docker push ${REPO}:${v}
        fi
    done

    # cleanup
    ${DEBUG} docker rmi ${app}_$$
    ${DEBUG} rm -rf ${FILES}
done

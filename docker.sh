#!/bin/sh

# docker.sh will search Dockerfile(s) containning in all sub-folders and current folder.
# it will build extractor on each founded Dockerfile with the tag of latest and foldername as part of docker image.
# Note, Dockerfile can be under current folder or any subfolders.

# exit on error, with error code
set -e

# use DEBUG=echo ./release.sh to print all commands
DEBUG=${DEBUG:-"echo"}

# Create the docker containers
for x in $( find ${PWD} -name Dockerfile ); do
  FOLDER=$( echo $x | sed 's#\(.*\)/Dockerfile#\1#' )
  # get basename from filepath. reference:
  # https://linuxgazette.net/18/bash.html
  # https://stackoverflow.com/questions/2664740/extract-file-basename-without-path-and-extension-in-bash
  NAME=${FOLDER##*/}
  ${DEBUG} docker build -t ncsapolyglot/${NAME}:latest ${FOLDER}
done

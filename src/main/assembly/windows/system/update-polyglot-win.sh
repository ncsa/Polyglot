#!/bin/bash
# Windows version of the script to be run via Cygwin.  Only stops/starts the Software Server.

# POL-MAIN is the main branch for this server
POL_BRANCH=${POL_BRANCH:-"POL-MAIN"}
POL_BUILD=${POL_BUILD:-"latestSuccessful"}

# HipChat token for notifications
HIPCHAT_TOKEN=""
HIPCHAT_ROOM="Software Installs"

# change to folder where script is installed
cd /cygdrive/c/Users/polyglot/Desktop

# fetch software
if [[ ${POL_BUILD} == latest* ]]; then
  BB="${POL_BRANCH}/${POL_BUILD}"
else
  BB="${POL_BRANCH}-${POL_BUILD}"
fi
wget -q -e robots=off -A "polyglot-*-bin.zip" -nd -r -N -l1 https://opensource.ncsa.illinois.edu/bamboo/browse/${BB}/artifact/JOB1/dist/
LATEST=$( ls -1rt polyglot-*-bin.zip | tail -1 )

if [ -s ${LATEST} ]; then
  if [ "$1" == "--force" -o ${LATEST} -nt polyglot ]; then
    exec 3>&1
    exec &> "/tmp/$$.txt"

    echo "UPDATING POLYGLOT ON ${HOSTNAME}"
    echo "BRANCH ${POL_BRANCH}"
    echo "BUILD ${POL_BUILD}"

    # stop the services
    taskkill /f /im java.exe

    # save old work
    if [ -d polyglot ]; then
      mv polyglot polyglot.$$
    fi

    # install new version
    unzip -q ${LATEST}
    mv $( basename ${LATEST} -bin.zip ) polyglot
    touch polyglot

    # restore local modifications
    if [ -d  ]; then
      cd polyglot.$$
      for x in tmp/SoftwareServer tmp/PolyglotRestlet SoftwareServer.conf SoftwareServerRestlet.conf PolyglotRestlet.conf PolyglotStewardAMQ.conf scripts/*/.aliases.txt; do
        if [ -e $x ]; then
          if [ -e ../polyglot/$x ]; then
            mv ../polyglot/$x ../polyglot/$x.orig
          fi
          mv $x ../polyglot/$x
        fi
      done
      cd ..
    fi
    rm -rf polyglot.$$

    # change permissions
		chmod 755 polyglot/SoftwareServerRestlet.bat
		chmod -R 755 polyglot/scripts

    # start all services again
		cd polyglot
		cygstart ./SoftwareServerRestlet.bat

    # send message by hipchat
    if [ "${HIPCHAT_TOKEN}" != "" -a "${HIPCHAT_ROOM}" != "" ]; then
      url="https://hipchat.ncsa.illinois.edu/v1/rooms/message?format=json&auth_token=${HIPCHAT_TOKEN}"
      txt=$(cat /tmp/$$.txt | sed 's/ /%20/g;s/!/%21/g;s/"/%22/g;s/#/%23/g;s/\$/%24/g;s/\&/%26/g;s/'\''/%27/g;s/(/%28/g;s/)/%29/g;s/:/%3A/g;s/$/<br\/>/g')
      room=$(echo ${HIPCHAT_ROOM} | sed 's/ /%20/g;s/!/%21/g;s/"/%22/g;s/#/%23/g;s/\$/%24/g;s/\&/%26/g;s/'\''/%27/g;s/(/%28/g;s/)/%29/g;s/:/%3A/g')
      body="room_id=${room}&from=polyglot&message=${txt}"
      result=$(curl -X POST -d "${body}" $url)
      if [ "${result}" != '{"status":"sent"}' ]; then
        cat /tmp/$$.txt >&3
        echo ${result} >&3
      fi
    else
      cat /tmp/$$.txt >&3
    fi
    rm /tmp/$$.txt
  fi
fi

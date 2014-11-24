#!/bin/bash

# change to folder where script is installed
cd $(dirname $0)

# POL-MAIN is the master branch
BRANCH=POL-MAIN

# HipChat token for notifications
HIPCHAT_TOKEN=""		#Add key before deploying!
HIPCHAT_ROOM="BrownDog"

# install startup scripts
if [ ! -e /etc/init/softwareserver ]; then
cat << EOF > /etc/init/softwareserver.conf
# Software Server
# this runs a software server as user browndog
# place this file in /etc/init
 
description "Software Server Restlet"
 
start on runlevel [2345]
stop on runlevel [!2345]
 
kill timeout 30

respawn
 
script
    exec start-stop-daemon --start --chuid browndog --name SoftwareServer --chdir /home/browndog/polyglot-2.1.0-SNAPSHOT --exec /usr/bin/java -- -cp polyglot.jar:lib/* -Xmx1g edu.illinois.ncsa.isda.softwareserver.SoftwareServerRestlet
end script
EOF
fi

if [ ! -e /etc/init/polyglot ]; then
cat << EOF > /etc/init/polyglot.conf
# Polyglot Server
# this runs the polyglot server as user browndog
# place this file in /etc/init
 
description "Polyglot Restlet"
 
start on runlevel [2345]
stop on runlevel [!2345]
 
kill timeout 30

respawn
 
script
    exec start-stop-daemon --start --chuid browndog --name Polyglot --chdir /home/browndog/polyglot-2.1.0-SNAPSHOT --exec /usr/bin/java -- -cp polyglot.jar:lib/* -Xmx1g edu.illinois.ncsa.isda.softwareserver.polyglot.PolyglotRestlet
end script
EOF
fi

if [ ! -e /etc/init.d/softwareserver ]; then
  ln -s /lib/init/upstart-job /etc/init.d/softwareserver
  update-rc.d softwareserver defaults
fi

if [ ! -e /etc/init.d/polyglot ]; then
  ln -s /lib/init/upstart-job /etc/init.d/polyglot
  update-rc.d polyglot defaults
fi

# fetch software
/usr/bin/wget -N -q -O polyglot-2.1.0-SNAPSHOT-bin.zip https://opensource.ncsa.illinois.edu/bamboo/browse/${BRANCH}/latest/artifact/JOB1/polyglot-2.1.0-SNAPSHOT-bin.zip/polyglot-2.1.0-SNAPSHOT-bin.zip
if [ $? == 0 -a polyglot-2.1.0-SNAPSHOT-bin.zip -nt polyglot-2.1.0-SNAPSHOT ]; then
  exec 3>&1
  exec &> "/tmp/$$.txt"

  echo "UPDATING POLYGLOT"

  /sbin/stop polyglot
  /sbin/stop softwareserver

  /bin/rm -rf polyglot-2.1.0-SNAPSHOT
  /usr/bin/unzip -q polyglot-2.1.0-SNAPSHOT-bin.zip
  /usr/bin/touch polyglot-2.1.0-SNAPSHOT

  # change permissions
  /bin/chown -R browndog.users polyglot-2.1.0-SNAPSHOT
  /bin/chmod -R o-w polyglot-2.1.0-SNAPSHOT

  /sbin/start softwareserver
  /sbin/start polyglot

  # update web components
  mkdir -p /var/www/html/dap
  cp polyglot-2.1.0-SNAPSHOT/web/dap/*.php /var/www/html/dap
  
  mkdir -p /var/www/html/dap/images
  cp polyglot-2.1.0-SNAPSHOT/images/favicon.png /var/www/html/dap/images 
  cp polyglot-2.1.0-SNAPSHOT/images/browndog-small.gif /var/www/html/dap/images 
  cp polyglot-2.1.0-SNAPSHOT/images/browndog-small-transparent.gif /var/www/html/dap/images 
  cp polyglot-2.1.0-SNAPSHOT/images/poweredby.gif /var/www/html/dap/images 
  cp polyglot-2.1.0-SNAPSHOT/images/poweredby-transparent.gif /var/www/html/dap/images 
  cp polyglot-2.1.0-SNAPSHOT/images/poweredby-borders.gif /var/www/html/dap/images 

  mkdir -p /var/www/html/dap/bookmarklet
  cp polyglot-2.1.0-SNAPSHOT/web/dap/bookmarklet/*.php /var/www/html/dap/bookmarklet
  cp polyglot-2.1.0-SNAPSHOT/web/dap/bookmarklet/*.css /var/www/html/dap/bookmarklet
  
  mkdir -p /var/www/html/dap/tests
  mkdir -p /var/www/html/dap/tests/tmp
  cp polyglot-2.1.0-SNAPSHOT/web/dap/tests/*.php /var/www/html/dap/tests
  cp polyglot-2.1.0-SNAPSHOT/web/dap/tests/*.py /var/www/html/dap/tests
  cp polyglot-2.1.0-SNAPSHOT/web/dap/tests/*.txt /var/www/html/dap/tests
  cp -r polyglot-2.1.0-SNAPSHOT/web/dap/tests/css /var/www/html/dap/tests
  cp -r polyglot-2.1.0-SNAPSHOT/web/dap/tests/fonts /var/www/html/dap/tests
  cp -r polyglot-2.1.0-SNAPSHOT/web/dap/tests/js /var/www/html/dap/tests

  mkdir -p /var/www/html/dap/plots
  mkdir -p /var/www/html/dap/plots/tmp
  cp polyglot-2.1.0-SNAPSHOT/web/dap/plots/*.php /var/www/html/dap/plots
  cp polyglot-2.1.0-SNAPSHOT/web/dap/plots/*.gnuplot /var/www/html/dap/plots

  # send message by hipchat
  if [ "${HIPCHAT_TOKEN}" != "" -a "${HIPCHAT_ROOM}" != "" ]; then
    url="https://api.hipchat.com/v1/rooms/message?format=json&auth_token=${HIPCHAT_TOKEN}"
    txt=$(cat /tmp/$$.txt | sed 's/ /%20/g;s/!/%21/g;s/"/%22/g;s/#/%23/g;s/\$/%24/g;s/\&/%26/g;s/'\''/%27/g;s/(/%28/g;s/)/%29/g;s/:/%3A/g;s/$/<br\/>/g')
    room=$(echo ${HIPCHAT_ROOM} | sed 's/ /%20/g;s/!/%21/g;s/"/%22/g;s/#/%23/g;s/\$/%24/g;s/\&/%26/g;s/'\''/%27/g;s/(/%28/g;s/)/%29/g;s/:/%3A/g')
    body="room_id=${room}&from=${HOSTNAME}&message=${txt}"
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

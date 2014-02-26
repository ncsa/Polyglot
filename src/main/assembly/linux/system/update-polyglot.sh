#!/bin/bash

# change to folder where script is installed
cd $(dirname $0)

# POL-MAIN is the master branch
BRANCH=POL-MAIN

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

if [ polyglot-2.1.0-SNAPSHOT-bin.zip -nt polyglot-2.1.0-SNAPSHOT ]; then
  echo "UPDATING POLYGLOT TO NEWER VERSION"

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
fi
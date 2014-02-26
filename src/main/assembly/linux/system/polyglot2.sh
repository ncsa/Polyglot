#!/bin/bash

### BEGIN INIT INFO
# Provides:          extractor
# Required-Start:    $remote_fs
# Required-Stop:     $remote_fs
# Default-Start:     2 3 4 5
# Default-Stop:      1
# Short-Description: MMDB Extraction Server
# Description:       Extracts metadata from datasets.
### END INIT INFO
#
# Author:       Rob Kooper <kooper@ncsa.uiuc.edu>
#

#Source function library
. /lib/lsb/init-functions

umask 0002

PATH=/bin:/usr/bin:/sbin:/usr/sbin
DIR=/usr/local/polyglot2
DAEMON_NAME=PolyglotServer
DAEMON=$DIR/$DAEMON_NAME.sh
NAME=Polyglot
LOCK=/tmp/$NAME.lck
USER=polyglot

RETVAL=0

pid_of_daemon() {
  ps auxwww | grep $DAEMON_NAME | grep -v grep | awk '{print $2}'
}

start() {
  log_daemon_msg "Starting $NAME daemon" "$NAME"

  #su -s /bin/bash - $USER -c "cd $DIR && nice -n +20 nohup $DAEMON >& /dev/null &"
  su -s /bin/bash - $USER -c "cd $DIR && nice -n +20 nohup $DAEMON >& /var/log/polyglot2/$DAEMON_NAME.log &"

  pid_of_daemon > /dev/null
  RETVAL=$?

  [ $RETVAL = 0 ] && touch "$LOCK"

  log_end_msg $RETVAL
}

stop() {
  log_daemon_msg "Stopping $NAME daemon" "$NAME"

  for p in `pid_of_daemon`; do pid="$pid $p"; done
  [ -n "$pid" ] && kill $pid
  RETVAL=$?
  cnt=20

  while [ $RETVAL = 0 -a $cnt -gt 0 ] &&
        { pid_of_daemon > /dev/null ; } ; do
      sleep 0.5
      ((cnt--))
  done

  [ $RETVAL = 0 ] && rm -f "$LOCK"
  
  log_end_msg $RETVAL
}

status() {
  for p in `pid_of_daemon`; do pid="$pid $p"; done
  if [ -n "$pid" ]; then
      log_success_msg "$NAME (pid $pid) is running..."
      RETVAL=0
      return
  fi
  if [ -f "$LOCK" ]; then
      log_failure_msg "$NAME is dead but lock exists"
      RETVAL=2
      return
  fi
  log_failure_msg "$NAME is stopped"
  RETVAL=3
}

#See how we were called
case "$1" in
  start)
    start
    ;;
  stop)
    stop
    ;;
  restart)
    stop
    start
    ;;
  status)
    status
    ;;
  *)
    echo "Usage: $0 {start|stop|restart|status}"
    exit 1
    ;;
esac

exit $RETVAL

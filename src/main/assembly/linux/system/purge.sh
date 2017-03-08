#!/bin/sh

#/sbin/stop polyglot
#/sbin/stop softwareserver

# Change this path according to this POL-SS installation.
POL_ROOT_DIR=/home/polyglot/polyglot

PO_RootPath=`grep RootPath $POL_ROOT_DIR/PolyglotRestlet.conf | cut -d= -f2 | tr -d '\r\n'`
SS_RootPath=`grep RootPath $POL_ROOT_DIR/SoftwareServer.conf | cut -d= -f2 | tr -d '\r\n'`
case "$PO_RootPath" in
    /*) P1=$PO_RootPath ;;
    *)  P1=$POL_ROOT_DIR/$PO_RootPath ;;
esac
case "$SS_RootPath" in
    /*) S1=$SS_RootPath ;;
    *)  S1=$POL_ROOT_DIR/$SS_RootPath ;;
esac

# Purge.
find $P1/Public -atime +1 -exec /bin/rm {} +
find $S1/Cache -atime +1 -exec /bin/rm {} +

#/sbin/start softwareserver
#/sbin/start polyglot

/sbin/stop polyglot
/sbin/stop softwareserver

find tmp/SoftwareServer/Cache000 -atime +1 -exec /bin/rm {} +

/sbin/start softwareserver
/sbin/start polyglot

#/sbin/stop polyglot
#/sbin/stop softwareserver

find /home/polyglot/tmp/SoftwareServer/Cache -atime +1 -exec /bin/rm {} +
find /home/polyglot/tmp/PolyglotRestlet/Public -atime +1 -exec /bin/rm {} +

#/sbin/start softwareserver
#/sbin/start polyglot

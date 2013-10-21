Polyglot Server Installation (Ubuntu):

1) Add a user "polyglot" belonging to group "polyglot".
2) Edit /etc/group and add polylot to www-data group.
3) Edit /etc/apache2/envars and add "umask 002".

   > service apache2 restart

4) Install Polyglot2:
   
   > cd /usr/local
   > mkdir polyglot2
   > cd polyglot2
   > wget http://isda.ncsa.uiuc.edu/~kmchenry/tmp/Polyglot2/Polyglot2.zip
   > unzip Polyglot2.zip
   > cd ..
   > chown -R polyglot:polyglot polyglot2

5) Install Polyglot2 web interface:
   
   > cp -r web/polyglot /var/www
   > cd /var/www
   > chown -R polyglot:www-data polyglot
   > chmod -R 775 polyglot

6) Edit /usr/local/polyglot2/PolyglotWebInterface.conf and set "PolyglotPath=/var/www/polyglot".

7) Configure service for startup

   > cd /var/log
   > mkdir polyglot2
   > chown polyglot:polyglot polyglot2
   > cp /usr/local/polyglot2/misc/polyglot2.sh /etc/init.d
   > cp /usr/local/polyglot2/misc/dssr.sh /etc/init.d
   > cd /etc/init.d
   > chmod 755 polyglot2.sh
   > chmod 755 dssr.sh
   > update-rc.d polyglot2.sh defaults
   > update-rc.d dssr.sh defaults

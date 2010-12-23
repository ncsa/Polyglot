Polyglot Server Installation (Ubuntu):

1) Edit /etc/group and add polylot to www-data group.
2) Edit /etc/apache2/envars and add "umask 002".

   > service apache2 restart

3) Install Polyglot2:
   
   > cd Desktop
   > mkdir Polyglot2
   > cd Polyglot2
   > wget http://isda.ncsa.uiuc.edu/~kmchenry/tmp/Polyglot2/Polyglot2.zip
   > unzip Polyglot2.zip

4) Install Polyglot2 web interface:
   
   > cp -r web/polyglot /var/www
   > cd /var/www
   > chown -R polyglot:www-data polyglot
   > chmod -R 775 polyglot


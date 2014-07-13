#!/bin/bash

mkdir -p /var/www/bookmarklet
cp *.php /var/www/bookmarklet
cp *.css /var/www/bookmarklet

mkdir -p /var/www/bookmarklet/images
cp ../../../../../images/favicon.png /var/www/bookmarklet/images 
cp ../../../../../images/browndog-small.gif /var/www/bookmarklet/images 
cp ../../../../../images/poweredby.gif /var/www/bookmarklet/images 

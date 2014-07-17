#!/bin/bash

mkdir -p /var/www/dap/bookmarklet
cp *.php /var/www/dap/bookmarklet
cp *.css /var/www/dap/bookmarklet

mkdir -p /var/www/dap/images
cp ../../../../../images/favicon.png /var/www/dap/images 
cp ../../../../../images/browndog-small.gif /var/www/dap/images 
cp ../../../../../images/poweredby.gif /var/www/dap/images 

#!/bin/sh
#Rar
#container
#rar
#html

mkdir -p $2
unrar -o+ e "$1" "$2"

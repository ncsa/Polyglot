#!/bin/sh
#tar
#container
#tar, tgz, tar.gz
#html

input_filename=$(basename "$1")
input_format="${input_filename##*.}" 
#mkdir -p $2

if [ "$input_format" = "tar" ]; then
	tar xvf "$1" -C "$2"
else
	tar xzvf "$1" -C "$2"
fi

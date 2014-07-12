#!/bin/sh
#Tar
#container
#tar, tgz
#html

output_filename=$(basename "$2")
output_format="${output_filename##*.}" 
mkdir -p $2

if [ "$output_format" = "tgz" ]; then
	tar xzf "$1" -C "$2"
else
	tar xf "$1" -C "$2"
fi

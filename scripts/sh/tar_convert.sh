#!/bin/sh
#tar
#container
#tar, tgz, tar.gz
#html

output_filename=$(basename "$2")
output_format="${output_filename##*.}" 
mkdir -p $2

if [ "$output_format" = "tar" ]; then
	tar xf "$1" -C "$2"
else
	tar xzf "$1" -C "$2"
fi

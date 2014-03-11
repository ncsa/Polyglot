#!/bin/bash
#soundconverter (2.0.4)
#sound
#mp4, ogg, wmv, mpeg, flv, ogv, mkv, vob, asf
#ogg, wmv, mpeg, flv, ogv, mkv, vob, asf

#echo $1
#echo $2

avconv -i "$1" "$2"

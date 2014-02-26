#!/bin/bash
#soundconverter (2.0.4)
#sound
#ogg, aac, mp3, flac, wav, avi, mpeg, mov, m4a, ac3, dts, alac, mpc, shorten, ape, sid, mod, xm, s3m
#ogg, flac, wav, aac, mp3, mpeg

extO=`echo ${2##*.}`

oF=`echo ${2##*/}`
name=`echo ${oF%.*}`
#echo $name

if   [ "ogg" = "$extO" ]; then
    soundconverter -b  $1
elif [ "mpeg" = "$extO" ]; then    
    soundconverter -b   --mime-type=audio/mpeg  --suffix=."$extO"  $1
else    
    soundconverter -b   --mime-type="$extO"  --suffix=."$extO"  $1
fi

mv "$name"."$extO" "$2"


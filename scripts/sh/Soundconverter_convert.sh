#!/bin/bash
#soundconverter (2.1.2)
#sound
#ogg, aac, mp3, flac, wav, avi, mpeg, mov, m4a, ac3, dts, alac, mpc, shorten, ape, sid, mod, xm, s3m, mp4
#ogg, flac, wav, aac, mp3, mpeg, m4a

inputFile=$(basename "$1")
#echo $inputFile
extO=${2##*.}
#echo $extO


outputFile=${1%%.*}
echo $outputFile.$extO

if   [ "ogg" = "$extO" ]; then
    soundconverter -b  "$1"
elif [ "mpeg" = "$extO" ]; then    
    soundconverter -b   -m audio/mpeg  -s ."$extO"  "$1"
elif [ "m4a" = "$extO" ]; then    
    soundconverter -b   -m audio/x-m4a -s ."$extO"  "$1"
else    
    soundconverter -b   -m "$extO"  -s ."$extO"  "$1"
fi

mv $outputFile.$extO "$2"

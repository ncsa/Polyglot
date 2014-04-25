#!/bin/bash
#FLAC (1.2.1)
#audio
#flac, fla, wav, aiff, aif
#flac, fla, wav, aiff, aif

extI=${1##*.}
extO=${2##*.}
randomDir=$RANDOM
#echo $extI
#echo $extO

if [ "$extI" = "$extO" ]                    || 
   [ "$extI" = "flac" -a "$extO" = "fla" ]  ||  
   [ "$extI" = "fla" -a "$extO" = "flac" ]  || 
   [ "$extI" = "aiff" -a "$extO" = "aif" ]  ||  
   [ "$extI" = "aif" -a "$extO" = "aiff" ]   ; then
    cp "$1" "$2"
elif [  "$extI" = "flac" ] || [ "$extI" = "fla" ]; then
    flac -d "$1" -f -o "$2"
elif [  "$extO" = "flac" ] || [ "$extO" = "fla" ]; then
    flac --best "$1" -f -o "$2"
else  # this is to convert wav <-falc-> aiff or aiff <-flac-> wav  
    mkdir /tmp/FlacConversion_$randomDir
    flac --best "$1" -f -o "/tmp/FlacConversion_$randomDir/temp.flac"
    flac -d "/tmp/FlacConversion_$randomDir/temp.flac" -f -o "$2"
    rm -rf "/tmp/FlacConversion_$randomDir"
fi

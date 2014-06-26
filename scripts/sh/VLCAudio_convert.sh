#!/bin/bash
#VLC Audio Converter (2.0.8)
#audio
#wav,mp3, mp4, aiff, aif 
#mp3, ogg, mp4, flac, wav

extI=${1##*.}
extO=${2##*.}
BitRate=192
#randomDir=$RANDOM
#echo $extI
#echo $extO

if [ "$extI" = "$extO" ]                     || 
   [ "$extI" = "flac" -a "$extO" = "fla"  ]  ||  
   [ "$extI" = "fla"  -a "$extO" = "flac" ]  || 
   [ "$extI" = "aiff" -a "$extO" = "aif"  ]  ||  
   [ "$extI" = "aif"  -a "$extO" = "aiff" ] ; then
    cp "$1" "$2"
else    
    if [ "$extO" = "mp3" ]; then
        ACODEC=mp3
        MUX=raw
    elif [ "$extO" = "ogg" ]; then
        ACODEC=vorbis
        MUX=ogg
    elif [ "$extO" = "mp4" ]; then
        ACODEC=mpga
        MUX=mp4
    elif [ "$extO" = "flac" -o  "$extO" = "fla"  ]; then
        ACODEC=flac
        MUX=raw
    elif [ "$extO" = "wav"  ]; then
        ACODEC=s16l
        MUX=wav
    fi
    
    cvlc "$1" --sout "#transcode{acodec=$ACODEC,  channels=2,ab=$BitRate}:std{access=file,mux=$MUX,dst='$2'}" vlc://quit
fi

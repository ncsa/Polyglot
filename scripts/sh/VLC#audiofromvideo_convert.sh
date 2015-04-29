#!/bin/bash
#VLC (2.0.8)
#audio
#mp4, mpg, wmv
#mp3, ogg, mp4, flac, fla, wav

extI=${1##*.}
extO=${2##*.}
BitRate=192
vBitRate=4096
#randomDir=$RANDOM
#echo $extI
#echo $extO

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

cvlc --no-sout-video --sout-audio "$1" --sout "#transcode{acodec=$ACODEC,ab=$BitRate,scale=1,channels=2,deinterlace,audio-sync }:std{access=file,mux=$MUX,dst='$2'}" vlc://quit

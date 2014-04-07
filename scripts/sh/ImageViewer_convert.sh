#!/bin/bash
#Image Viewer (v3.6.2)
#images
#jpeg, bmp, tiff, png, ico
#jpeg, bmp, tiff, png, ico

## This script need some debug.
## It does not write png files
## correctly when run from poliglot.

inputFile=`echo ${1##*/}`
#echo $inputFile
#echo $2

xdotool exec --sync   eog "$1"  2> /dev/null  &
sleep 3             ## waitting for file to open
xdotool search --name $inputFile windowactivate --sync  
sleep 1
xdotool key  --clearmodifiers  "ctrl+shift+s" 
sleep 1
xdotool search --name "Save Image" windowactivate --sync  
sleep 1
xdotool key --clearmodifiers 'Home+shift+End+Delete'
xdotool type "$2"
sleep 1
xdotool search --name "Save Image" windowactivate --sync  
xdotool key --clearmodifiers 'Return'
sleep 1
xdotool search --name $inputFile windowactivate --sync  
sleep 5
xdotool key --clearmodifiers 'ctrl+w'

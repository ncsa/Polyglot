#!/bin/bash
#GThumb (v3.0.2)
#image
#jpeg, jpg, tiff,tif, png, tga, svg
#jpeg, jpg, tiff,tif, png, tga

## This script need some debug.
## It does not write png files
## correctly when run from poliglot.

inputFile=`echo ${1##*/}`
outputFile=`echo ${2##*/}`
#echo $inputFile
#echo $outputFile

xdotool exec --sync   gthumb "$1"  2> /dev/null  &
sleep 3             ## waitting for file to open
xdotool search --name $inputFile.".*gThumb" windowactivate --sync  

sleep 1
xdotool key  --clearmodifiers  "alt+f" 
xdotool key  --clearmodifiers  "a" 
xdotool key  --clearmodifiers  "Return" 

sleep 1
xdotool search --name "Save Image" windowactivate --sync  
sleep 1
xdotool key --clearmodifiers 'Home+shift+End+Delete'
xdotool type "$2"
sleep 1
xdotool search --name "Save Image" windowactivate
xdotool key --clearmodifiers 'Return'

while [ ! -s "$2"  ]; do
     true      ## waitting for file to save
done
sleep 1        ## waitting a little more
xdotool search --name "$outputFile.*gThumb" windowactivate
sleep 1
xdotool key --clearmodifiers 'ctrl+q'

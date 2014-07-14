#!/bin/bash
#eog (v3.6.2)
#image
#jpeg, bmp, tiff, png, ico
#jpeg, bmp, tiff, png, ico

inputFile=`echo ${1##*/}`
#echo $inputFile
#echo $2

xdotool exec --sync   eog "$1"  2> /dev/null  &
sleep 3             ## waitting for file to open
xdotool search --name $inputFile windowactivate
sleep 1
xdotool key  --clearmodifiers  "ctrl+shift+s" 
sleep 1
xdotool search --name "Save Image" windowactivate
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
xdotool search --name $inputFile windowactivate
sleep 5
xdotool key --clearmodifiers 'ctrl+w'

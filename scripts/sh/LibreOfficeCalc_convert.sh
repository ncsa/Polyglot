#!/bin/bash
#LibreOffice Calc (v4.0.2.2)
# spreadsheet
# ods, ots, sxc, stc, fods, uos, xlsx, xml, xls, xlt, dif, dbf, html, slk, csv, xlsx
# ods, ots, sxc, stc, fods, uos, xlsx, xml, xls, xlt, dif, dbf, html, slk, csv, xlsx

outExt=`echo ${2##*.}`
inputFile=`echo ${1##*/}`
outputFile=`echo ${2##*/}`


xdotool exec --sync libreoffice --nologo --calc  $1 &

sleep 5             ## waitting for file to open

xdotool search --name $inputFile.".*LibreOffice Calc" windowactivate --sync  

sleep 1
xdotool key  --clearmodifiers  "ctrl+shift+s" 
sleep 1
xdotool key --clearmodifiers "Delete"
xdotool type $2
sleep 1
xdotool key --clearmodifiers "Return" 

xdotool search  --name  $outputFile.".*LibreOffice Calc" windowactivate

if [ $outExt = "csv" ]; then
    xdotool key --clearmodifiers "shift+Tab"
    xdotool key --clearmodifiers "shift+Tab"
    xdotool key --clearmodifiers "shift+Tab+Return"
elif [ $outExt = "dif" ] || [ $outExt = "dbf" ] ; then
    sleep 2 
    xdotool key --clearmodifiers "Tab+Return"
fi 

while [ ! -s $2  ]; do
     true      ## waitting for file to save
done

sleep 2 
xdotool search  --name  $outputFile.".*LibreOffice Calc" windowactivate
xdotool key --clearmodifiers "alt+f+c"


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


if [ $outExt = "csv" ]; then
    sleep 1 
    xdotool search  --name  "Export Text File" windowactivate
    xdotool key --clearmodifiers "shift+Tab"
    xdotool key --clearmodifiers "shift+Tab"
    xdotool key --clearmodifiers "shift+Tab+Return"
    sleep 1          # in case more than one sheet 
    xdotool search  --name  "LibreOffice.*" windowactivate
    xdotool key --clearmodifiers "Return"
elif [ $outExt = "dif" ] || [ $outExt = "dbf" ] ; then
    sleep 1
    xdotool search  --name  "Dif Export" windowactivate
    xdotool key --clearmodifiers "Tab+Return"
    sleep 1          # in case more than one sheet 
    xdotool search  --name  "LibreOffice.*" windowactivate
    xdotool key --clearmodifiers "Return"
fi 

xdotool search  --name  $outputFile.".*LibreOffice Calc" windowactivate

while [ ! -s $2  ]; do
     true      ## waitting for file to save
done

sleep 2 
xdotool search  --name  $outputFile.".*LibreOffice Calc" windowactivate key --clearmodifiers "alt+f+c"


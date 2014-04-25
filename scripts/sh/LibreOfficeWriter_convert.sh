#!/bin/bash
#LibreOffice Writer (v4.0.2.2)
#spreadsheet
#odt, ott, sxw, stw, fodt, uot, docx, xml, doc, html,rtf, txt
#odt, ott, sxw, stw, fodt, uot, docx, xml, doc, html,rtf, txt

inputFile=`echo ${1##*/}`
outputFile=`echo ${2##*/}`

xdotool exec --sync libreoffice --nologo --writer  $1 &

#echo "waiting...."
sleep 5             ## waitting for file to open
#echo "end of waiting...."

xdotool search --name $inputFile.".*LibreOffice Writer" windowactivate --sync  
sleep 1
xdotool key  --clearmodifiers  "ctrl+shift+s" 
sleep 1
xdotool key --clearmodifiers "Delete"
xdotool type $2
sleep 1
xdotool key --clearmodifiers "Return" 

while [ ! -s $2  ]; do
    true      ## waitting for file to save
done

sleep 2
xdotool search  --name  $outputFile.".*LibreOffice Writer" windowactivate key "alt+f+c"


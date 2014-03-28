#!/bin/bash
#Calibre (v1.29)
#ebook conversions
#cbz, cbr, cbc, chm, djvu, docx, epub, fb2, html, htmlz, lit, lrf, mobi, odt, pdf, prc, pdb, pml, rb, rtf, snb, tcr, txt, txtz
#azw3, epub, fb2, oeb, lit, lrf, mobi, htmlz, pdb, pml, rb, pdf, rtf, snb, tcr, txt, txtz

# This script do not work inside polyglot!
# use Ebookconvert_convert.sh for ebook conversions.
#
inputFile=`echo ${1##*/}`
outputFile=`echo ${2##*/}`
extI=`echo ${1##*.}`
extO=`echo ${2##*.}`

xdotool exec --sync calibre --detach "$1" 

#echo "waiting...."
sleep 20             ## waitting for file to open
#echo "end of waiting...."

xdotool search --name ".*Calibre Library.*" windowactivate 
xdotool type c
sleep 2             
xdotool search --name "Convert *" windowactivate 
sleep 2             
xdotool key "alt+o"
xdotool key "alt+o"

sleep 2             
xdotool type $extO

sleep 1
xdotool key "Return"

sleep 5

fileI=`find ~/Calibre\ Library/ -name "*.$extI"`
fileO=`echo ${fileI%.*}`
fileO=$fileO.$extO

#echo $fileI
#echo $fileO

while [ ! -s "$fileO"  ]; do
    true      ## waitting for file to save
done

sleep 10
cp "$fileO" "$2"

#sleep 2
#xdotool search --name ".*Calibre Library.*" windowactivate
#xdotool key  "Delete"
#xdotool search --name "Are you sure?" windowactivate
#xdotool key  "alt+o"
sleep 2

xdotool search --name ".*Calibre Library.*" windowactivate
xdotool key  "alt+F4"

sleep 2
#rm "$fileI" "$fileO"
rm -rf ~/Calibre\ Library/; mkdir ~/Calibre\ Library/


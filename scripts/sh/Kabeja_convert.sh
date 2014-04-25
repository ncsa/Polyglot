#!/bin/bash
#Kabeja (v0.4)
#model
#dxf
#svg, pdf, png, tif, tiff

inputFile=$(basename "$1")
#echo $inputFile

outputFile=$(basename "$2")
#echo $outputFile

extO="${2##*.}"
#echo $extO

if [ "$extO" = "tiff" ]; then
    outputFile=${outputFile%?}
    #echo "after removal of last char: "$outputFile
elif [ "$extO" = "tif" ]; then
    extO="tiff"
    #echo "adding character: "$extO
fi

cd /opt/kabeja-0.4/
cp "$1" "./$inputFile"

kabeja.sh  -nogui -pp conf/process.xml   -pipeline "$extO"   "$inputFile" "$2"
rm "$inputFile"
#mv "$outputFile" "$2"


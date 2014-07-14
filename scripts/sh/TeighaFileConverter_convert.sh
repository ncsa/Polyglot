#!/bin/bash
#TeighaFileConverter (v3.91)
#model
#dwg, dxf
#dwgv2013, dwgv2010, dwgv2007, dwgv2004, dwgv2000, dwgv14, dwgv13, dwgv12, dxfv2013tascii, dxfv2010tascii, dxfv2007tascii, dxfv2004tascii, dxfv2000tascii, dxfv14tascii, dxfv13tascii, dxfv12tascii, dxfv10tascii, dxfv9tascii, dxfv2013tbinary, dxfv2010tbinary, dxfv2007tbinary, dxfv2004tbinary, dxfv2000tbinary, dxfv14tbinary, dxfv13tbinary, dxfv12tbinary, dxfv10tbinary

#randomDir=$RANDOM
randomDir="001"
#echo $randomDir

inputFile=$(basename "$1")
#echo $inputFile

outputFile=$(basename "$2")
#echo $outputFile


extO=${outputFile##*.}
#echo $extO

realExt=${extO:0:3}
#echo "real extension: "$realExt

outputFileName=${inputFile%%.*}.$realExt
#echo "outputFileName: "$outputFileName

verAndType=${extO:3:255}
#echo "Version and type: "$verAndType

version=${verAndType##*v}
type=${version##*t}
version="acad"${version%%t*}
#echo "version: "$version
#echo "type: "$type


if   [ "$realExt" == "dxf" ]; then
    if  [ "$type" == "ascii" ]; then
        type="dxf"
    elif [ "$type" == "binary" ]; then
        type="dxb"
    fi    
elif  [ "$realExt" == "dwg" ]; then
    type="dwg"
fi

mkdir "/tmp/Teigha$randomDir"
cp "$1" "/tmp/Teigha$randomDir"
cd "/tmp/Teigha$randomDir"

# here is where the magic is done
TeighaFileConverter  "." "./converted"  "$version" "$type" "0" "0" 2> /dev/null

cd "converted"
outputFile=${outputFile%%_*}
mv "$outputFileName" "$2"
rm -rf "/tmp/Teigha$randomDir"

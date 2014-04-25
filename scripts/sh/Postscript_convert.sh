#!/bin/bash
#Poscript2Many
#documents
#ps
#ascii, eps, epsi, pdf, pdf12, pdf13, pdf14, pdfwr, ps, ps2, txt

extO=`echo ${2##*.}`

if [ "pdf" = "$extO" ];     then
    ps2pdf "$1" "$2"
elif [ "pdf12" = "$extO" ]; then
    ps2pdf12 "$1" "$2"
elif [ "pdf13" = "$extO" ]; then
    ps2pdf13 "$1" "$2"
elif [ "pdf14" = "$extO" ]; then
    ps2pdf14 "$1" "$2"
elif [ "ascii" = "$extO" ]; then
    ps2ascii "$1" "$2"
elif [ "txt" = "$extO" ];   then
    ps2txt "$1" "$2"
elif [ "eps" = "$extO" ];   then
    ps2eps -f "$1"
elif [ "epsi" = "$extO" ];  then
    ps2epsi "$1" "$2"
elif [ "ps" = "$extO" ];    then
    cp "$1" ./temp.ps
    ps2ps ./temp.ps "$2"
    rm ./temp.ps
elif [ "ps2" = "$extO" ];   then
    ps2ps2 "$1" "$2"
elif [ "pdfwr" = "$extO" ]; then
    ps2pdfwr "$1" "$2"
fi

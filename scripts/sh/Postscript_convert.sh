#!/bin/bash
#poscript2Many
#documents
#ps
#ascii, eps, epsi, pdf, pdf12, pdf13, pdf14, pdfwr, ps, ps2, txt

#inputFile=`echo ${1##*/}`
#outputFile=`echo ${2##*/}`
#extI=`echo ${1##*.}`
extO=`echo ${2##*.}`


if [ "pdf" = "$extO" ]; then
    ps2pdf $1 $2
elif [ "pdf12" = "$extO" ]; then
    ps2pdf12 $1 $2
elif [ "pdf13" = "$extO" ]; then
    ps2pdf13 $1 $2
elif [ "pdf14" = "$extO" ]; then
    ps2pdf14 $1 $2
elif [ "ascii" = "$extO" ]; then
    ps2ascii $1 $2
elif [ "tex" = "$extO" ]; then
    ps2tex $1 $2
elif [ "eps" = "$extO" ]; then
    ps2eps $1
elif [ "epsi" = "$extO" ]; then
    ps2epsi $1
elif [ "ps" = "$extO" ]; then
    ps2ps $1 $2.ps
elif [ "ps2" = "$extO" ]; then
    ps2ps2 $1 $2
elif [ "pdfwr" = "$extO" ]; then
    ps2pdfwr $1 $2
fi  


#ps2eps $1 $2

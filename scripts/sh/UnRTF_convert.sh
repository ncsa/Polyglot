#!/bin/sh
#UnRTF (v0.19.2)
#Documents
#rtf
#tex, ps, tex, html

extO=`echo ${2##*.}`

if [ "ps" = "$extO" ]; then
    unrtf -t ps "$1"  > "$2"
elif [ "html" = "$extO" ]; then
    unrtf -t html "$1"  > "$2"
elif [ "tex" = "$extO" ]; then
    unrtf -t latex "$1"  > "$2"
elif [ "txt" = "$extO" ]; then
    unrtf -t text "$1"  > "$2"
fi



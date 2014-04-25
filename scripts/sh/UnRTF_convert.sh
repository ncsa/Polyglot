#!/bin/sh
#UnRTF (v0.19.2)
#document
#rtf
#tex, ps, txt, html

extO="${2##*.}"

if [ "$extO" = "ps" ]; then
    unrtf -t ps "$1"  > "$2"
elif [ "$extO" = "html" ]; then
    unrtf -t html "$1"  > "$2"
elif [ "$extO" = "tex" ];  then
    unrtf -t latex "$1"  > "$2"
elif [ "$extO" = "txt" ]; then
    unrtf -t text "$1"  > "$2"
fi


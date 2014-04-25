#!/bin/bash
#Unoconv (v 0.5)
#document
#doc, docx, html, odp, ods, odt, ppt, rtf, sdw, sxw, txt, xls, xlsx, xlsm, xml
#odt, pdf, ppt, doc, docx, html, rtf, sxw, txt, xml, odp, ods, xls, xlsx

extO="${2##*.}"

unoconv   -f $extO  -o "$2"   "$1"

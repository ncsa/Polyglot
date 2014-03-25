#!/bin/bash
#Unoconv (LibreOffice 4.0)
#Dociments conversions
#doc, html, odp, ods, odt, ppt, rtf, sdw, sxw, txt, xls, xlsx, xlsm, xml
#odt, pdf, ppt, doc, html, rtf, sxw, txt, xml, odp, ods, xls, xlsx

extO=`echo ${2##*.}`

unoconv   -f $extO  -o "$2"   "$1"

;GIMP (v2.6)
;image
;jpg
;pgm, png

;Get arguments and correct slashes in path to unix style
arg1 = %1%
StringReplace arg1, arg1, \, /, All
arg2 = %2%
StringReplace arg2, arg2, \, /, All

;Run program
RunWait, "C:\Program Files\Gimp-2.0\bin\gimp-console-2.6.exe" -b "(gimp-convert \"%arg1%\" \"%arg2%\")"

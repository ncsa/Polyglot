;Microsoft Office Excel (2010) 
; open excel file
; xl, xsl, xlsx

;Parse input filename
arg1 = %1%
;arg1 = "test\test.xsl"
StringGetPos, index, arg1, \, R
ifLess, index, 0, ExitApp
index += 2
input_filename := SubStr(arg1, index)

;Run program if not already running
IfWinNotExist, Book1 - Microsoft Excel
{
  Run, "C:\Program Files (x86)\Microsoft Office\Office14\EXCEL.EXE" "%1%"
  WinWait,Book1 - Microsoft Excel
}

;Make sure image is loaded before continuing
Loop
{
  IfWinExist, %input_filename% - Microsoft Excel
  {
    break
  }

  Sleep, 500
}

return

;test opening a file using key strokes
; none of them works
SendInput, !fa
Send, {ALT}+{f}
Send, a
SendEvent, !fs


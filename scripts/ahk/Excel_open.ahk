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
IfWinNotExist, Microsoft Excel - Book1
{
  Run, "C:\Program Files (x86)\Microsoft Office\Office14\EXCEL.EXE" "%1%"
  ;WinWait, Microsoft Excel - Book1
}

;Make sure image is loaded before continuing
Loop
{
  IfWinExist, Microsoft Excel - %input_filename%
  {
    break
  }

  Sleep, 500
}




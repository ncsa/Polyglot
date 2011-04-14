;Microsoft Word (2010)
;document
;doc, docx, html, odt, rtf, txt, wpd, wps


;Parse input filename
arg1 = %1%
StringGetPos, index, arg1, \, R
ifLess, index, 0, ExitApp
index += 2
input_filename := SubStr(arg1, index)

;Run program
Run, "C:\Program Files (x86)\Microsoft Office\Office14\WINWORD.EXE" "%1%"

;Make sure image is loaded before continuing
Loop
{
  IfWinExist, %input_filename% - Microsoft Word
  {
    break
  }

  Sleep, 500
}

Sleep, 1000


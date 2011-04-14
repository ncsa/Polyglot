;Microsoft Office Word (2007)
;document
;doc, docx, html, odt, rtf, txt, wpd, wps
;doc, docx, odt

;Parse input filename
arg1 = %1%
StringGetPos, index, arg1, \, R
ifLess, index, 0, ExitApp
index += 2
input_filename := SubStr(arg1, index)

;Parse output filename
arg2 = %2%
StringGetPos, index, arg2, \, R
ifLess, index, 0, ExitApp
index += 2
output_filename := SubStr(arg2, index)

;Parse output format
StringGetPos, index, arg2, ., R
ifLess, index, 0, ExitApp
index += 2
out := SubStr(arg2, index)

;Run program
Run, C:\Program Files\Microsoft Office\Office12\WINWORD.EXE "%1%"

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

;Save document
Send, !f
Send, !f

if(out = "docx"){
  Send, !w
}else if(out = "doc"){
  Send, !9
}else if(out = "odt"){
  Send, !d

}

WinWait, Save As
ControlSetText, Edit1, %2%
ControlSend, Edit1, {Enter}

;Return to main window before exiting
Loop
{
  ;Continue on if main window is active
  IfWinActive, %output_filename% - Microsoft Word
  { 
    break
  }

  Sleep, 500
}

Send, !f
Send, !x
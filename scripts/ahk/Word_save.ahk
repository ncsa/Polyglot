;Microsoft Office Word (2010)
;document
;doc, docx, odt

;Parse output filename
arg1 = %1%
StringGetPos, index, arg1, \, R
ifLess, index, 0, ExitApp
index += 2
input_filename := SubStr(arg1, index)

;Parse output format
StringGetPos, index, arg1, ., R
ifLess, index, 0, ExitApp
index += 2
out := SubStr(arg1, index)

;allow partial match of the title
SetTitleMatchMode, 2
IfWinNotActive, Microsoft Word, MsoDockBottom, WinActivate,  Microsoft Word, MsoDockBottom
WinWaitActive,  Microsoft Word, MsoDockBottom


;Save document
Send, !f
Send, a

WinWait, Save As
ControlSetText, Edit1, %1%

ControlClick, ComboBox2, Save As
Send {Up 25}

if(out = "docx"){
}else if(out = "doc"){
  Send, {w 2}
}else if(out = "odt"){
  Send, {o}
}

Send, {Enter}

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
Send, x
;Notepad
; text document
;txt


;Parse output filename
arg1 = %1%
StringGetPos, index, arg1, \, R
ifLess, index, 0, ExitApp
index += 2
output_filename := SubStr(arg1, index)


;Parse output format
StringGetPos, index, arg1, ., R
ifLess, index, 0, ExitApp
index += 2
out := SubStr(arg1, index)

;allow partial match of the title
SetTitleMatchMode, 2
IfWinNotActive, Notepad, WinActivate,  Notepad
WinWaitActive,  Notepad

Send, !f
;Send, {ALT}+{f}
Send, a

;Save the model
WinWait, Save As
ControlSetText, Edit1, %1%

Send, {Enter}

;Return to main window before exiting
Loop
{
  ;Continue on if main window is active
  IfWinActive, %output_filename% - Notepad
  { 
    break
  }

Sleep, 500
}

Send, !f
Send, x


;Microsoft Office Excel (2010) 
; save excel file
; xlsx, xlsm, xlsb, xls

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
IfWinNotActive, Microsoft Excel 
WinActivate,  Microsoft Excel
WinWaitActive,  Microsoft Excel


;Save document
Send, !f
Send, a

WinWait, Save As
ControlSetText, Edit1, %1%

ControlClick, ComboBox2, Save As
Send {Up 27}


if(out = "xlsx"){
}else if(out = "xlsm"){
  Send, {e}
}else if(out = "xlsb"){
  Send, {e 2}
}else if(out = "xls"){
  Send, {e 3}
  }
; xlsx, xlsm, xlsb, xls
Send, {Enter}

ControlSend, Edit1, {Enter}



;Return to main window before exiting
Loop
{
  ;Continue on if main window is active
  IfWinActive, Microsoft Excel - %output_filename%
  { 
    break
  }

;MsgBox, %output_filename%

  Sleep, 500
}

Send, !f
Send, x
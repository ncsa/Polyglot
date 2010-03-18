;Adobe Acrobat (v9.3.0 Pro Extended)
;document
;doc, html, jpg, pdf, ps, rtf, txt

;Parse output format
arg1 = %1%
StringGetPos, index, arg1, ., R
ifLess, index, 0, ExitApp
index += 2
out := SubStr(arg1, index)

;Parse filename root
StringGetPos, index, arg1, \, R
ifLess, index, 0, ExitApp
index += 2
name := SubStr(arg1, index)
StringGetPos, index, name, ., R
ifLess, index, 0, ExitApp
name := SubStr(name, 1, index)

;Activate the window
WinActivate, %name%.pdf - Adobe Acrobat Pro Extended
WinWaitActive, %name%.pdf - Adobe Acrobat Pro Extended

;Save document
Send, ^S
WinWait, Save As

if(out = "doc"){
  ControlSend, ComboBox3, m
}else if(out = "html"){
  controlSend, ComboBox3, h
}else if(out = "jpg"){
  controlSend, ComboBox3, j
}else if(out = "pdf"){
  controlSend, ComboBox3, a
}else if(out = "ps"){
  controlSend, ComboBox3, p
  controlSend, ComboBox3, p
  controlSend, ComboBox3, p
  controlSend, ComboBox3, p
  controlSend, ComboBox3, p
}else if(out = "rtf"){
  controlSend, ComboBox3, r
}else if(out = "txt"){
  controlSend, ComboBox3, t
  controlSend, ComboBox3, t
}

ControlSetText, Edit1, %1%
ControlSend, Edit1, {Enter}

;Return to main window before exiting
Loop
{
  ;Continue on if main window is active
  IfWinActive, %name%.pdf - Adobe Acrobat Pro Extended
  { 
    break
  }

  ;Click "Yes" if asked to overwrite files
  IfWinExist, Save As
  {
    ControlGetText, tmp, Button1, Save As

    if(tmp = "&Yes")
    {
      ControlClick, Button1, Save As
    }
  }

  Sleep, 500
}

;Wait a lit bit more just in case
Sleep, 1000

;Close whatever document is currently open
Send, ^w

;Make sure it actually closed before exiting
Loop
{
  ;Continue on if main window is active
  IfWinActive, Adobe Acrobat Pro Extended
  { 
    break
  }

  Sleep, 500
}
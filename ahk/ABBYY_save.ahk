;ABBYY FineReader (v9.0 Professional Edition)
;image
;txt

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

;check for the opened window
WinWait, Untitled document [1] - ABBYY FineReader 9.0 Professional Edition

;Activate the window
WinActivate, Untitled document [1] - ABBYY FineReader 9.0 Professional Edition
WinWaitActive, Untitled document [1: Text] - ABBYY FineReader 9.0 Professional Edition

;Save the model
PostMessage, 0x111, 40567, 0,, Untitled document [1: Text] - ABBYY FineReader 9.0 Professional Edition
WinWait, Save Pages
ControlSetText, Edit1, %1%
ControlSend, Edit1, {Enter}
Sleep, 500

;Return to main window
Loop
{
  ;Continue on if main window is active
  ;IfWinActive, Untitled document [1: Text] - ABBYY FineReader 9.0 Professional Edition
  ;{ 
  ;  break
  ;}

  ;Click "OK" if asked to overwrite files
  IfWinExist, ABBYY FineReader 9.0 Professional Edition
  {
    ControlGetText, tmp, Button1, ABBYY FineReader 9.0 Professional Edition

    if(tmp = "OK")
    {
      ControlClick, Button1, ABBYY FineReader 9.0 Professional Edition
    }
  }

  ;Close WordPad
  IfWinExist, %name%.%out% - WordPad
  {
    WinActivate, %name%.%out% - WordPad
    WinWaitActive, %name%.%out% - WordPad
    PostMessage, 0x111, 57665, 0,, %name%.%out% - WordPad
    break
  }

  Sleep, 500
}

;Re-activate the window
WinActivate, Untitled document [1] - ABBYY FineReader 9.0 Professional Edition
WinWaitActive, Untitled document [1: Text] - ABBYY FineReader 9.0 Professional Edition

;Reset window
PostMessage, 0x111, 40444, 0,, Untitled document [1: Text] - ABBYY FineReader 9.0 Professional Edition

;Return to main window
Loop
{
  ;Click "No" if asked to save
  IfWinExist, ABBYY FineReader 9.0 Professional Edition
  {
    ControlGetText, tmp, Button2, ABBYY FineReader 9.0 Professional Edition

    if(tmp = "&No")
    {
      ControlClick, Button2, ABBYY FineReader 9.0 Professional Edition
      break
    }
  }

  Sleep, 500
}
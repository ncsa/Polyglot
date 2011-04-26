;3Ds Max (v11.0 2009 Educational)
;model
;max

;check for the opened window
SetTitleMatchMode, 2
WinWait, Autodesk 3ds Max Design 2009

;Activate the window
WinActivate, Autodesk 3ds Max Design 2009
WinWaitActive, Autodesk 3ds Max Design 2009

;Save the model
PostMessage, 0x111, 40007, 0,, Autodesk 3ds Max Design 2009
WinWait, Save File As
ControlSetText, Edit1, %1%
ControlSend, Edit1, {Enter}
Sleep, 500

;Return to main window
Loop
{
  ;Continue on if main window is active
  IfWinActive, Autodesk 3ds Max Design 2009
  { 
    break
  }

  ;Click "Yes" if asked to overwrite files
  IfWinExist, Save File As
  {
    Send, {Enter}
  }

  Sleep, 500
}

;Close the model
PostMessage, 0x111, 40005, 0,, Autodesk 3ds Max Design 2009

;Confirm reset
Loop
{
  ;Click "Yes" when asked to really reset
  ControlGetText, tmp, Static1, 3ds Max

  ifInString, tmp, Do you really want to reset?
  {
    ControlClick, Button1, 3ds Max
    break
  }

  Sleep, 500
}

;3Ds Max (v11.0, 2009 Educational)
;3d
;max

;Run program if not already running
SetTitleMatchMode, 2

IfWinNotExist, Autodesk 3ds Max Design 2009
{
  Run, C:\Program Files\Autodesk\3ds Max 2009\3dsmax.exe
  WinWait, Autodesk 3ds Max Design 2009
}

;Activate the window
WinActivate, Autodesk 3ds Max Design 2009
WinWaitActive, Autodesk 3ds Max Design 2009

;Open model
PostMessage, 0x111, 40003, 0,, Autodesk 3ds Max Design 2009
WinWait, Open File
ControlSetText, Edit1, %1%
ControlSend, Edit1, {Enter}
Sleep, 500

;Return to main window before exiting
Loop
{
  ;Continue on if main window is active
  IfWinActive, Autodesk 3ds Max Design 2009
  { 
    break
  }

  ;Click "Continue" if asked about missing files
  IfWinExist, Missing External Files
  {
    Send, {Enter}
  }

  Sleep, 500
}
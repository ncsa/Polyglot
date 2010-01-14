;3Ds Max (v11.0, 2009 Educational)
;3d
;3ds, ai, dae, dwf, dwg, dxf, fbx, flt, igs, lay, lp, m3g, obj, stl, vw, w3d, wrl

;check for the opened window
SetTitleMatchMode, 2
WinWait, Autodesk 3ds Max Design 2009

;Activate the window
WinActivate, Autodesk 3ds Max Design 2009
WinWaitActive, Autodesk 3ds Max Design 2009

;Save the model
PostMessage, 0x111, 40011, 0,, Autodesk 3ds Max Design 2009
WinWait, Select File to Export
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
  IfWinExist, Select File to Export
  {
    Send, {Enter}
  }

  ;Click "Export" if asked to merge files
  ControlGetText, tmp, Button1, Export

  if(tmp = "Export")
  {
    ControlClick, Button1, Export
  }

  ;Click "-=DONE=-" when exporting
  ifWinExist, Exporting
  {
    Send, {Enter}
  }

  ;Click "OK" when asked to preserve texture coordinates
  ifWinExist, Export Scene
  {
    ControlClick, Button1, Export Scene
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

  ;Click "No" if asked to overwrite files
  ControlGetText, tmp, Static2, 3ds Max

  ifInString, tmp, The scene has been modified.
  {
    ControlClick, Button2, 3ds Max
  }

  Sleep, 500
}
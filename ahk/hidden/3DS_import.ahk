;3Ds Max (v11.0, 2009 Educational)
;3d
;3ds, ai, ddf, dem, dwg, dxf, fbx, flt, iam, ige, iges, igs, ipt, lp, ls, obj, shp, stl, vw, wrl, wrz

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
PostMessage, 0x111, 40010, 0,, Autodesk 3ds Max Design 2009
WinWait, Select File to Import
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

  ;Click "OK" if asked to merge files
  ControlGetText, tmp, Button1, Import

  if(tmp = "OK")
  {
    ControlClick, Button1, Import
  }

  ;Click "Import" if asked for options
  ControlGetText, tmp, Button2, Import

  if(tmp = "Import")
  {
    ControlClick, Button2, Import
  }

  Sleep, 500
}
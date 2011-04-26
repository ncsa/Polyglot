;3Ds Max (v11.0 2009 Educational)
;model
;3ds, ai, dwg, dxf, fbx, igs, obj, stl, wrl

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
  
	;Click "OK" when asked to Export to AutoCAD file
  ifWinExist, Export to AutoCAD File
  {
    ControlClick, Button7, Export to AutoCAD File
  }
	
	;Click "OK" when told nothing to convert
  ifWinExist, DWG/DXF Export Warning
  {
    ControlClick, Button1, DWG/DXF Export Warning
  }
	
	;Click "OK" for FBX Export
  ifWinExist, FBX Export
  {
    ControlClick, Button1, FBX Export
  }
	
	;Click "OK" for STL Export
  ifWinExist, Export STL File
  {
    ControlClick, Button4, Export STL File
  }
	
	;Click "OK" for VRML97 Exporter 
  ifWinExist, VRML97 Exporter
  {
    ControlClick, Button1, VRML97 Exporter
  }
	
	;Click "OK" for AIEXP no data warning
  ifWinExist, AIEXP
  {
    ControlClick, Button1, AIEXP
  }
	
	;Click "OK" for IGES Export
  ifWinExist, IGES Export
  {
    ControlClick, Button2, IGES Export
  }
  
	;Click "Export" if asked to merge files
  ControlGetText, tmp, Button1, Export

  if(tmp = "Export")
  {
    ControlClick, Button1, Export
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

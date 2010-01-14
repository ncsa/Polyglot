;ParaView (v3.4.0)
;3d
;ply, pvd, stl, vtk

;check for the opened window
WinWait, Kitware ParaView 3.4.0

;Activate the window
WinActivate, Kitware ParaView 3.4.0
WinWaitActive, Kitware ParaView 3.4.0

;Save the model
Send, ^s
WinWait, Save File:
SetKeyDelay, 0
Send, %1%
Send, {Enter}
Sleep, 500

;Wait for save to go through (checking for overwrite warnings)
Loop
{
  ;Continue on if main window is active
  IfWinActive, Configure Writer
  { 
    Send, {Enter}
    break
  }

  ;Click "Yes" if asked to overwrite files
  IfWinExist, Save File:
  {
    Send, {Enter}
  }

  Sleep, 500
}

;Close the model
WinWaitActive, Kitware ParaView 3.4.0
Send, ^z
Send, ^z
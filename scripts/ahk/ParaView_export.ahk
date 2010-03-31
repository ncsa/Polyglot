;ParaView (v3.4.0)
;model
;pov, vrml, x3d, x3db

;check for the opened window
WinWait, Kitware ParaView 3.4.0

;Activate the window
WinActivate, Kitware ParaView 3.4.0
WinWaitActive, Kitware ParaView 3.4.0

;Save the model
Send, !f
Send, {Down}
Send, {Down}
Send, {Down}
Send, {Down}
Send, {Down}
Send, {Down}
Send, {Down}
Send, {Enter}
WinWait, Save File:
SetKeyDelay, 0
Send, %1%
Send, {Enter}
Sleep, 500

;Wait for save to go through (checking for overwrite warnings)
Loop
{
  ;Continue on if main window is active
  IfWinActive, Kitware ParaView 3.4.0
  { 
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
Send, ^z
Send, ^z
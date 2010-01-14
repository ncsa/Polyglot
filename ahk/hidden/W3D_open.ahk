;Wings 3D (v0.99.02)
;3d
;wings

;Run program if not already running
IfWinNotExist, Wings 3D
{
  Run, "C:\Program Files\wings3d_0.99.02\Wings3D.exe"
  WinWait, Wings 3D
}

;Activate the window
WinActivate, Wings 3D
WinWaitActive, Wings 3D

;Open the model
Send, ^w
WinWait, Open
ControlSetText, Edit1, %1%
ControlSend, Edit1, {Enter}

;Wait for main window to return
WinWaitActive, Wings 3D
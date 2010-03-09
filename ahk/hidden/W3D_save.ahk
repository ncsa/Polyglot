;Wings 3D (v0.99.02)
;model
;wings

;Activate the window
WinActivate, Wings 3D
WinWaitActive, Wings 3D

;Save the model
Send, !w
WinWait, Save
ControlSetText, Edit1, %1%
ControlSend, Edit1, {Enter}

;Close model to prepare for next open/import
WinWaitActive, Wings 3D
Send, ^c
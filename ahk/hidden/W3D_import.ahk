;Wings 3D (v0.99.02)
;3d
;3ds, ai, fbx, lwo, lxo, ndo, obj, stl

;Run program if not already running
IfWinNotExist, Wings 3D
{
  Run, "C:\Program Files\wings3d_0.99.02\Wings3D.exe"
  WinWait, Wings 3D
}

;Activate the window
WinActivate, Wings 3D
WinWaitActive, Wings 3D

;Parse input format
arg1 = %1%
StringGetPos, index, arg1, ., R
ifLess, index, 0, ExitApp
index += 2
in := SubStr(arg1, index)

;Import the model
if(in = "3ds"){
  Send, ^3
}else if(in = "ai"){
  Send, ^a
}else if(in = "fbx"){
  Send, ^f
}else if(in = "lwo"){
  Send, ^l
}else if(in = "lxo"){
  Send, ^l
}else if(in = "ndo"){
  Send, ^n
}else if(in = "obj"){
  SendInput, ^o
}else if(in = "stl"){
  Send, ^s
}

WinWait, Import
ControlSetText, Edit1, %1%
ControlSend, Edit1, {Enter}

;Wait for main window to return
WinWaitActive, Wings 3D
;Sleep, 1000	;Wait a bit more just in case
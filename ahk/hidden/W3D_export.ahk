;Wings 3D (v0.99.02)
;3d
;3ds, eps, fbx, lwo, lxo, ndo, obj, pov, rwx, stl, wrl, x

;Activate the window
WinActivate, Wings 3D
WinWaitActive, Wings 3D

;Parse output format
arg1 = %1%
StringGetPos, index, arg1, ., R
ifLess, index, 0, ExitApp
index += 2
out := SubStr(arg1, index)

;Export the model
if(out = "3ds"){
  Send, !3
}else if(out = "eps"){
  Send, !e
}else if(out = "fbx"){
  Send, !f
}else if(out = "lwo"){
  Send, !l
}else if(out = "lxo"){
  Send, !l
}else if(out = "ndo"){
  SendInput, !n
}else if(out = "obj"){
  Send, !o
}else if(out = "pov"){
  Send, !p
}else if(out = "rwx"){
  Send, !r
}else if(out = "stl"){
  Send, !s
}else if(out = "wrl"){
  Send, !v
}else if(out = "x"){
  Send, !x
}

WinWait, Export
ControlSetText, Edit1, %1%
ControlSend, Edit1, {Enter}

;Undo Import or Close to prepare for next import
WinWaitActive, Wings 3D
;Sleep, 1000	;Wait a bit more just in case
;Send, ^z
Send, ^c{Tab}{Enter}
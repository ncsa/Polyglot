;Google SketchUp (v7.0)
;3d
;skp

;Parse input format
arg1 = %1%
StringGetPos, index, arg1, ., R
ifLess, index, 0, ExitApp
index += 2
out := SubStr(arg1, index)

;Parse filename root
StringGetPos, index, arg1, \, R
ifLess, index, 0, ExitApp
index += 2
name := SubStr(arg1, index)
StringGetPos, index, name, ., R
ifLess, index, 0, ExitApp
name := SubStr(name, 1, index)

;check for the opened window
WinWait, Untitled - SketchUp

;Activate the window
WinActivate, Untitled - SketchUp
WinWaitActive, Untitled - SketchUp

;Save the model
PostMessage, 0x111, 57604, 0,, Untitled - SketchUp
WinWait, Save As
ControlSetText, Edit1, %1%
ControlSend, Edit1, {Enter}

;Wait for Export to complete
WinWaitActive, %name%.skp - SketchUp

;Close the model
Loop
{
  IfWinActive, Untitled - SketchUp
  {
    break
  }

  PostMessage, 0x111, 57600, 0,, %name%.skp - SketchUp

  Sleep, 500
}

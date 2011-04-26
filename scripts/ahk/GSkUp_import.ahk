;Google SketchUp (v7.0)
;model
;3ds, ddf, dem, dwg, dxf, skp

;Parse input format
arg1 = %1%
SplitPath, arg1,,, in

;Run program if not already running
IfWinNotExist, Untitled - SketchUp
{
  Run, C:\Program Files\Google\Google SketchUp 7\SketchUp.exe
  WinWait, Untitled - SketchUp
}

;Activate the window
WinActivate, Untitled - SketchUp
WinWaitActive, Untitled - SketchUp

;Open model
PostMessage, 0x111, 21933, 0,, Untitled - SketchUp
WinWait, Open

ControlClick, ComboBox3, Open
Send, {Up 12}

if(in = "skp"){
}else if(in = "dwg" or in = "dxf"){
	Send, {a}
}else if(in = "3ds"){
	Send, {3}
}else if(in = "dem" or in = "ddf"){
	Send, {d}
}

ControlSetText, Edit1, %1%
Sleep, 500
ControlSend, Edit1, {Enter}

;Wait for Import to complete
WinWaitActive, Import Results

;Close the imports results window
Loop
{
  IfWinActive, Untitled - SketchUp
  {
    break
  }

  ControlClick, Button1, Import Results

  Sleep, 500
}

;Set the model
MouseClick, left, 500, 500

;Google SketchUp (v7.0)
;model
;3ds, ddf, dem, dwg, dxf, skp

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
ControlSetText, Edit1, %1%
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

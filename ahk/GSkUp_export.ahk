;Google SketchUp (v7.0)
;model
;kmz

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

;Export the model
PostMessage, 0x111, 21149, 0,, Untitled - SketchUp
WinWait, Export Model
ControlSetText, Edit1, %1%
ControlSend, Edit1, {Enter}

;Wait for Export to complete and check for overwrite warnings
Loop
{
  ;Continue on if main window is active
  IfWinActive, Collada Export Results
  { 
    break
  }

  ;Click "Yes" if asked to overwrite files
  IfWinExist, Export Model
  {
    ControlGetText, tmp, Button1, Export Model

    if(tmp = "&Yes")
    {
      ControlClick, Button1, Export Model
    }
  }

  Sleep, 500
}

;Close the exports results window
Loop
{
  IfWinActive, Untitled - SketchUp
  {
    break
  }

  ControlClick, Button1, Collada Export Results

  Sleep, 500
}

;Close the model
PostMessage, 0x111, 57600, 0,, Untitled - SketchUp
Sleep, 500
WinWait, SketchUp

;Wait for main window to return
Loop
{
  ;Continue on if main window is active
  IfWinActive, Untitled - SketchUp
  { 
    break
  }

  ;Click "No" when asked to save
  IfWinExist, SketchUp
  {
    ControlGetText, tmp, Button2, SketchUp

    if(tmp = "&No")
    {
      ControlClick, Button2, SketchUp
    }
  }

  Sleep, 500
}
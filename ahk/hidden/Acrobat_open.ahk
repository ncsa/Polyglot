;Adobe Acrobat (v9.3.0 Pro Extended)
;document
;pdf

;Parse input filename
arg1 = %1%
StringGetPos, index, arg1, \, R
ifLess, index, 0, ExitApp
index += 2
input_filename := SubStr(arg1, index)

;Run program if not already running
IfWinNotExist, Adobe 3D Reviewer
{
  Run, C:\Program Files\Adobe\Acrobat 9.0\Acrobat\Acrobat.exe
  WinWait, Adobe Acrobat Pro Extended
}

;Activate the window
WinActivate, Adobe Acrobat Pro Extended
WinWaitActive, Adobe Acrobat Pro Extended

;Open document
Send, ^o
WinWait, Open
ControlSetText, Edit1, %1%
ControlSend, Edit1, {Enter}

;Make sure model is loaded before exiting
Loop
{
  IfWinExist, %input_filename% - Adobe Acrobat Pro Extended
  {
    break
  }

  Sleep, 500
}
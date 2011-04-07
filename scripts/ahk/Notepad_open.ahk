;Notepad
; text document
;txt, rtf

;Parse input filename
inputPath = %1%
;path split to get extension 
SplitPath, inputPath, input_filename

;Run program if not already running
IfWinNotExist, Untitled - Notepad
{
  Run, C:\Windows\System32\notepad.exe
  WinWait, Untitled - Notepad
}

;Activate the window
WinActivate, Untitled - Notepad
WinWaitActive, Untitled - Notepad

;Open document
Send, ^o
WinWait, Open
ControlSetText, Edit1, %1%
ControlSend, Edit1, {Enter}

;Make sure model is loaded before exiting
Loop
{
  IfWinExist, %input_filename% - Notepad
  {
    break
  }

  Sleep, 500
}
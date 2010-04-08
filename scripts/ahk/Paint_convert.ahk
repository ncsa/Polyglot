;Paint (v6.1)
;image
;bmp, dib, gif, ico, jpg, png, tif
;bmp, dib, gif, jpg, png

;Parse input filename
arg1 = %1%
StringGetPos, index, arg1, \, R
ifLess, index, 0, ExitApp
index += 2
input_filename := SubStr(arg1, index)

;Parse output filename
arg2 = %2%
StringGetPos, index, arg2, \, R
ifLess, index, 0, ExitApp
index += 2
output_filename := SubStr(arg2, index)

;Parse output format
StringGetPos, index, arg2, ., R
ifLess, index, 0, ExitApp
index += 2
out := SubStr(arg2, index)

;Run program
Run, mspaint "%1%"

;Make sure image is loaded before continuing
Loop
{
  IfWinExist, %input_filename% - Paint
  {
    break
  }

  ;Click "OK" if cannot read the file and exit
  IfWinExist, Paint
  {
    ControlGetText, tmp, Button1, Paint

    if(tmp = "OK")
    {
      ControlClick, Button1, Paint
      ExitApp
    }
  }

  Sleep, 500
}

;Activate the window
WinActivate, %input_filename% - Paint
WinWaitActive, %input_filename% - Paint

;Save image
Send, !f
Send, !v

if(out = "bmp" or out = "dib"){
  Send, !b
}else if(out = "gif"){
  Send, !g
}else if(out = "jpg"){
  Send, !j
}else if(out = "png"){
  Send, !p
}

WinWait, Save As
ControlSetText, Edit1, %2%
ControlSend, Edit1, {Enter}

;Return to main window before exiting
Loop
{
  ;Continue on if main window is active
  IfWinActive, %output_filename% - Paint
  { 
    break
  }

  ;Click "Yes" if asked to overwrite files
  IfWinExist, Confirm
  {
    ControlGetText, tmp, Button1, Confirm

    if(tmp = "&Yes")
    {
      ControlClick, Button1, Confirm
    }
  }

  ;Click "OK" if asked to continue when quality reduction occurs
  IfWinExist, Paint
  {
    ControlGetText, tmp, Button1, Paint

    if(tmp = "OK")
    {
      ControlClick, Button1, Paint
    }
  }

  Sleep, 500
}

Send, !f
Send, !x
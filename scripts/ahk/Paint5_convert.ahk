;Paint (v5.1)
;image
;bmp, gif, ico, jpg, png, tif
;bmp, dib, gif, jpg, png, tif

;Parse input filename
arg1 = %1%
SplitPath, arg1, input_filename

;Parse output filename and format
arg2 = %2%
SplitPath, arg2, output_filename,, out

;Run program
Run, mspaint "%1%"

;Make sure image is loaded before continuing
WinWait, %input_filename% - Paint

;Save image
Send, !f
Send, !f
Send, a

WinWait, Save As
ControlClick, ComboBox3, Save As
Send, {Up 8}

if(out = "bmp" or out = "dib"){
	Send, {2}
}else if(out = "jpg"){
	Send, {j}
}else if(out = "gif"){
	Send, {g}
}else if(out = "tif"){
	Send, {t}
}else if(out = "png"){
	Send, {p}
}

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

  ;Click "Yes" if asked to continue when loss occurs
  IfWinExist, Paint
  {
    ControlGetText, tmp, Button1, Paint

    if(tmp = "&Yes")
    {
      ControlClick, Button1, Paint
    }
  }

  Sleep, 500
}

;Exit
Send, !f
Send, !f
Send, x

;Adobe Photoshop CS5
;image
;png, jpg

wintitle=ahk_class Photoshop

;parse output file extension
arg1=%1%
SplitPath, arg1,,,output_extension
if(output_extension=="png"){
	filetype=PNG
}else if(output_extension="jpg"){
	filetype=JPEG (
}else{
	ExitApp
}

;check if program is open
IfWinNotExist, %wintitle%
{
	ExitApp
}

;activate the window
WinActivate, %wintitle%
WinWaitActive

;save the file
SendInput, ^S
WinWaitActive, Save As
ControlSetText, Edit1, %arg1%
SetControlDelay -1
ControlClick, ComboBox3
direction=down
Loop
{
	ControlGet, selected, Choice,, ComboBox3
	IfInString, selected, %filetype%
	{
		break
	}
	if(lastselected==selected){
		if(direction=="down"){
			direction=up
		}else{
			direction=down
		}
	}
	lastselected:=selected
	SendInput {%direction%}
	Sleep 10
}
ControlSend, ComboBox3, {Enter}
ControlSend, Edit1, {Enter}

;Return to main window before exiting
Loop
{
	;Continue on if main window is active
	SetTitleMatchMode, 2
	IfWinActive, %wintitle%
	{
		break
	}
	SetTitleMatchMode, 1
	;Click "Yes" if asked to overwrite files
	IfWinExist, Adobe Photoshop ahk_class #32770
	{
		SetControlDelay -1
		ControlClick, Button1
	}
	;click OK if asked for PNG options
	IfWinExist, PNG Options
	{
		Sleep, 400
		SetControlDelay, -1
		ControlClick, Button3
	}
	;click OK if asked for JPEG options
	IfWinExist, JPEG Options
	{
		SetControlDelay, -1
		ControlClick, Button4
	}
	
	Sleep, 500
}
WinWaitActive, %wintitle%
;Microsoft Access (2007)
;document
;accdb, mdb, csv, txt
;accdb, mdb

;parse input filename
arg1=%1%
Splitpath, arg1, input_filename

;parse input file extension
SplitPath, arg1,,,input_extension

;parse output file extension
file=%2%
SplitPath, file,,,extension

;determine text for Save As combobox option
if(extension="accdb"){
	filetype=Microsoft Access Database
	macro=!fdoy3
}else if(extension="mdb"){
	filetype=Microsoft Access Database
	macro=!fdly3
}else if(extension="accde"){
	filetyep=ACCDE File
	macro=!fdmy3
}else{
	ExitApp
}

;run program
Run, "C:\Program Files (x86)\Microsoft Office\Office14\MSACCESS.EXE" "%1%"

;make sure the window is loaded before continuing
SetTitleMatchMode 2
WinWaitActive, Microsoft Access -
WinGet, proc, PID
SetTitleMatchMode 1

;close any pre-existing save dialogs
while WinExist("Save As ahk_pid " . proc)
{
	WinClose, Save As ahk_pid %proc%
}

;take care of conversion dialogs
if(input_extension="txt" || input_extension="csv"){
	WinWaitActive, Link Text Wizard
	SendInput {Enter 4}
	WinWait, Link Text Wizard, Finished linking table
	SetControlDelay, -1
	ControlClick, Button1
	Sleep, 200
}

;Save As
SetTitleMatchMode 2
WinActivate, Microsoft Access - ahk_pid %proc%
WinWaitActive, Microsoft Access - ahk_pid %proc%
SetTitleMatchMode 1
Sleep, 200
SendInput %macro%
WinWait, Save As ahk_pid %proc%
ControlSetText, Edit1, %file%
SetControlDelay -1
ControlClick, ComboBox2
direction=down
Loop
{
	ControlGet, selected, Choice,, ComboBox2
	IfInString, selected, %filetype%
	{
		break
	}
	if(lastselected==selected){
		if(direction=="down"){
			direction="up"
		}else{
			direction="down"
		}
	}
	lastselected:=selected
	SendInput {%direction%}
	Sleep 10
}
ControlSend, ComboBox2, {Enter}
ControlClick, Button1, Save As ahk_pid %proc%

;Return to main window before exiting
Loop
{
	;Continue on if main window is active
	SetTitleMatchMode, 2
	IfWinActive, Microsoft Access - ahk_pid %proc%
	{
		break
	}
	SetTitleMatchMode, 1
	;Click "Yes" if asked to overwrite files
	IfWinExist, Confirm
	{
		ControlGetText, tmp, Button1, Confirm
		if(tmp = "&Yes")
		{
			SetControlDelay -1
			ControlClick, Button1, Confirm
		}
	}
	
	Sleep, 500
}

;kill the app
WinClose, ahk_pid %proc%
IfWinExist, ahk_pid %proc%
{
	Sleep, 1000
	Process, Close, %proc%
}
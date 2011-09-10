;Microsoft OneNote (2010)
;document
;one
;one, onepkg, mht, pdf, xps, docx, doc

;parse input filename
arg1=%1%
Splitpath, arg1, input_filename

;parse output file extension
file=%2%
SplitPath, file,,,extension

;determine text for Save As combobox option
if(extension="one"){
	filetype=OneNote Section
}else if(extension="onepkg"){
	filetype=OneNote Single File Package
}else if(extension="mht"){
	filetype=Single File Web Page
}else if(extension="pdf"){
	filetype=PDF
}else if(extension="xps"){
	filetype=XPS Document
}else if(extension="docx"){
	filetype=Microsoft Word XML Document
}else if(extension="doc"){
	filetype=Microsoft Word Document
}else{
	ExitApp
}

;run program
Run, "C:\Program Files (x86)\Microsoft Office\Office14\ONENOTE.EXE" "%1%",,,proc

;make sure the window is loaded before continuing
SetTitleMatchMode 2
WinWait, - Microsoft OneNote
WinGet, proc, PID
SetTitleMatchMode 1

;close any pre-existing save dialogs
while WinExist("Save As ahk_pid " . proc)
{
	WinClose, Save As ahk_pid %proc%
}

;Save As
SetTitleMatchMode 2
WinActivate, - Microsoft OneNote ahk_pid %proc%
WinWaitActive
SetTitleMatchMode 1
Sleep, 200
SendInput !fsa
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
ControlClick, Button4, Save As ahk_pid %proc%

;Return to main window before exiting
Loop
{
	;Continue on if main window is active
	SetTitleMatchMode, 2
	IfWinActive, - Microsoft OneNote ahk_pid %proc%
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
;WordPad (v6.1)
;document
;rtf, odt, docx, txt
;rtf, odt, docx, txt

;parse input filename
arg1=%1%
StringGetPos, index, arg1, \, R
ifLess, index, 0, ExitApp
index += 2
input_filename := SubStr(arg1, index)
StringGetPos, dotIndex, input_filename, ., R
if(dotIndex < 0){
	input_nofiletype := input_filename
}else{
	StringLeft, input_nofiletype, input_filename, %dotIndex%
}

;parse output file extension
file=%2%
StringGetPos, dotIndex, file, ., R
if(dotIndex < 0){
	ExitApp
}else{
	StringRight, extension, file, Strlen(file) - (dotIndex + 1)
}

;determine text for Save As combobox option
if(extension="rtf"){
	filetype=Rich Text Format
}else if(extension="docx"){
	filetype=Open Office XML Document
}else if(extension="odt"){
	filetype=OpenDocument Text
}else if(extension="txt"){
	filetype=Text Document
}else{
	ExitApp
}

;run program if not already running
IfWinNotExist, Document - WordPad
{
	Run, "write.exe" "%1%"
}

;make sure the window is loaded before continuing
SetTitleMatchMode RegEx
WinWait, (%input_filename%|%input_nofiletype%) \- WordPad
WinGet, proc, PID, (%input_filename%|%input_nofiletype%) \- WordPad
SetTitleMatchMode 1

;close any pre-existing save dialogs
while WinExist("Save As ahk_pid " . proc)
{
	WinClose, Save As ahk_pid %proc%
}

;Save As
SetTitleMatchMode RegEx
WinActivate, (%input_filename%|%input_nofiletype%) \- WordPad
SetTitleMatchMode 1
SendInput !fa
WinWait, Save As ahk_pid %proc%
ControlFocus, Edit1, Save As ahk_pid %proc%
SendInput {Raw}%file%
Sleep, 100
Control, ChooseString, %filetype%, ComboBox2, Save As ahk_pid %proc%
SetControlDelay -1
ControlClick, Button2, Save As ahk_pid %proc%,,,

;Return to main window before exiting
Loop
{
	;Continue on if main window is active
	SetTitleMatchMode, 2
	IfWinActive, - WordPad ahk_pid %proc%
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
			ControlClick, Button1, Confirm
		}
	}
	;Click "Yes" if asked to save text only
	IfWinExist, WordPad ahk_pid %proc%, You are about to save the document in a Text-Only format
	{
		ControlGetText, tmp, Button1
		if(tmp = "&Yes")
		{
			ControlClick, Button1
		}
	}
	
	Sleep, 500
}

WinClose, ahk_pid %proc%
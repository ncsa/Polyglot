;Microsoft Office Publisher (2010)
;document
;pub, mht, htm, txt, doc, docx, rtf, docm, wps
;pub, pdf, xps, mht, htm, rtf, docm, wps, docx, doc, gif, jpg, tif, png, bmp, wmf, emf

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

;parse input file extension
StringGetPos, dotIndex, input_filename, ., R
if(dotIndex < 0){
	ExitApp
}else{
	StringRight, input_extension, input_filename, Strlen(input_filename) - (dotIndex + 1)
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
if(extension="pub"){
	filetype=Publisher Files
}else if(extension="pdf"){
	filetype=PDF
}else if(extension="xps"){
	filetype=XPS Document
}else if(extension="mht"){
	filetype=Single File Web Page
}else if(extension="htm"){
	filetype=Web Page, Filtered
}else if(extension="rtf"){
	filetype=Rich Text Format
}else if(extension="docm"){
	filetype=Word 2010 Macro-enabled Document
}else if(extension="wps"){
	filetype=Works 6 - 9 Document
}else if(extension="docx"){
	filetype=Word 2010 Document
}else if(extension="doc"){
	filetype=Word 97-2003 Document
}else if(extension="gif"){
	filetype=GIF Graphics Interchange Format
}else if(extension="jpg"){
	filetype=JPEG File Interchange Format
}else if(extension="tif"){
	filetype=Tag Image File Format
}else if(extension="png"){
	filetype=PNG Portable Network Graphics Format
}else if(extension="bmp"){
	filetype=Device Independent Bitmap
}else if(extension="wmf"){
	filetype=Windows Metafile
}else if(extension="emf"){
	filetype=Enhanced Metafile
}else{
	ExitApp
}

;run program
Run, "C:\Program Files (x86)\Microsoft Office\Office14\MSPUB.EXE" "%1%",,,proc

;make sure the window is loaded before continuing
SetTitleMatchMode 2
WinWait, - Microsoft Publisher ahk_pid %proc%
SetTitleMatchMode 1

;take care of txt conversion dialog
if(input_extension="txt"){
	Sleep, 500
}
SetTitlematchMode RegEx
IfWinActive, File Conversion \- (%input_filename%|%input_nofiletype%) ahk_pid %proc%, Select the encoding that makes your document readable
{
	SetControlDelay -1
	ControlClick, Button1
	SetTitleMatchMode, 2
	WinWaitActive, - Microsoft Publisher ahk_pid %proc%
}
SetTitleMatchMode 1

;close any pre-existing save dialogs
while WinExist("Save As ahk_pid " . proc)
{
	WinClose, Save As ahk_pid %proc%
}

;Save As
SetTitleMatchMode 2
WinActivate, - Microsoft Publisher ahk_pid %proc%
WinWaitActive, - Microsoft Publisher ahk_pid %proc%
SetTitleMatchMode 1
Sleep, 200
SendInput !fa
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
Control, Uncheck,, Button2
ControlClick, Button3, Save As ahk_pid %proc%

;Return to main window before exiting
Loop
{
	;Continue on if main window is active
	SetTitleMatchMode, 2
	IfWinActive, - Microsoft Publisher ahk_pid %proc%
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
	;Click "OK" if asked to save text only
	IfWinExist, Microsoft Publisher, The file type you selected only supports text
	{
		ControlGetText, tmp, Button1
		if(tmp = "OK")
		{
			SetControlDelay -1
			ControlClick, Button1
		}
	}
	;failure
	IfWinExist, Microsoft Publisher, This publication cannot be saved in a text-only format
	{
		Process, Close %proc%
		ExitApp
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
;Microsoft Excel (2010)

;parse output file type
arg1=%1%
SplitPath, arg1,,,extension
if(extension=="pdf"){
	filetype=PDF
}else if(extension=="xps"){
	filetype=XPS Document
}else{
	ExitApp
}

;Activate the window
SetTitleMatchMode, 2
WinActivate, Microsoft Excel
WinWaitActive, Microsoft Excel

;move cursor to upper left cell
Send ^{up 2}^{left 2}

;navigate gui
Send, !nq{right 2}{Enter}

;cut the image and paste it in a blank spreadsheet
Send, ^x
Send, ^n
Send, ^v

;save as pdf
Send, ^s

WinWaitActive, Save As
ControlSetText, Edit1, %arg1%
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
Control, Uncheck,,Button5
ControlClick, Button7

;return to main window
Loop
{
	;Continue on if main window is active
	SetTitleMatchMode, 2
	IfWinActive, Microsoft Excel
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

;close current document to return to previous one
Send, ^w
WinWaitActive, Microsoft Excel ahk_class NUIDialog
;this is a weird dialog. The buttons have no identification
Send {right}{Enter}
WinWaitClose
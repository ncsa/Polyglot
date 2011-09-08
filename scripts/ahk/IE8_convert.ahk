;Internet Explorer (8)
;document
;html, htm, mht, txt
;htm, mht, txt

;parse filename
arg1=%1%
SplitPath, arg1, input_filename,,extension
if(input_filename=""){
	ExitApp
}

;parse output file type
arg2=%2%
SplitPath, arg2,,,output_extension
if(output_extension="htm"){
	filetype=Web page, HTML only
}else if(output_extension="mht"){
	filetype=Web Archive
}else if(output_extension="txt"){
	filetype=Text File
}else{
	ExitApp
}

;minimize currently open IE windows, so we can see the new one
SetTitleMatchMode, 2
Loop{
	IfWinActive, Windows Internet Explorer
	{
		WinMinimize
	}else{
		break
	}
}
SetTitleMatchMode, 1

;Run program
Run, "C:\Program Files (x86)\Internet Explorer\iexplore.exe" "%arg1%"

;make sure window is active before continuing
SetTitleMatchMode, 2
WinWaitActive, - Windows Internet Explorer
SetTitleMatchMode, 1

;Save As
SendInput, !fa
WinWaitActive, Save Webpage
Control, ChooseString, %filetype%, ComboBox2
ControlSetText, Edit1, %arg2%
ControlSend, Edit1, {Enter}

;Return to main window before exiting
Loop
{
	;Continue on if main window is active
	SetTitleMatchMode, 2
	IfWinActive, - Windows Internet Explorer
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
	
	Sleep, 500
}

WinClose
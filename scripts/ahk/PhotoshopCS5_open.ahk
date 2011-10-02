;Adobe PhotoShop CS5
;image
;png, jpg

wintitle=ahk_class Photoshop
commandLine:=0

;parse input file extension
arg1=%1%
SplitPath, arg1,,,input_extension
if(input_extension!="png" && input_extension!="jpg"){
	ExitApp
}

;open the program unless it's already running
IfWinExist, %wintitle%
{
	WinActivate, %wintitle%
}else{
	Run, "C:\Program Files (x86)\Adobe\Adobe Photoshop CS5\Photoshop.exe" "%arg1%"
	commandLine:=1
}
WinWaitActive,  %wintitle%

;open the file if not already opened through command line
if(commandLine==0){
	SendInput, ^o
	WinWaitActive, Open
	ControlSetText, Edit1, %arg1%
	ControlSend, Edit1, {Enter}
	WinWaitActive, %wintitle%
}
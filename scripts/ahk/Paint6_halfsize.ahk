;Paint (v6.1)

;Parse input filename
arg1 = %1%
SplitPath, arg1, input_filename
if(input_filename=""){
	ExitApp
}

;Run program
Run, mspaint "%1%",,,proc

;Make sure image is loaded before continuing
WinWait, ahk_pid %proc%
WinActivate, ahk_pid %proc%

;half size
SendInput !hre
WinWait, Resize and Skew ahk_pid %proc%
Control, Check,, Button2
ControlSetText, Edit1, 50
ControlSetText, Edit2, 50
SetControlDelay, -1
ControlClick, Button6

;save file
SendInput ^s
Sleep, 200

;close application
WinClose, ahk_pid %proc%
WinWaitClose, ahk_pid %proc%
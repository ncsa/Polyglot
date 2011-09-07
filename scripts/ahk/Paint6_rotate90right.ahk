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

;rotate 90 right
SendInput !hror

;save file
SendInput ^s
Sleep, 200

;close application
WinClose, ahk_pid %proc%
WinWaitClose, ahk_pid %proc%
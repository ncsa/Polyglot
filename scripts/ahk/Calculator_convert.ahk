;Calculator (v6.1)
;document
;txt
;txt

;parse input data
arg1=%1%
SplitPath, arg1,,,input_extension
if(input_extension!="txt"){
	ExitApp
}
FileRead, input_arithmetic, %arg1%

;check output filetype
arg2=%2%
SplitPath, arg2,,,output_extension
if(output_extension!="txt"){
	ExitApp
}

;run the calculator
IfWinExist, Calculator
{
	WinActivate, Calculator
}else{
	Run, "calc"
}
WinWaitActive, Calculator

;do the calculation
Send, {Esc}
SendRaw, %input_arithmetic%
Send, {Enter}

Sleep, 300

;get result and output it
ControlGetText, result, Static4

FileDelete, %arg2%
FileAppend, %result%, %arg2%
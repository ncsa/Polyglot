﻿;Calculator (v6.1)
;document
;txt
;txt

alias_cos:="co"
alias_sin:="s"
alias_tan:="t"
alias_ln:="n"

;recursively converts mathematical functions to calculator hotkeys
parseFunctions(arithmetic)
{
	foundPos:=RegexMatch(arithmetic, "i)(cos|sin|tan|ln)\(", match)
	;base case
	if(foundPos==0){
		return arithmetic
	}
	;find where the function's parenthesis become balanced
	parenthesis:=1
	currPos:=foundPos+4
	while parenthesis!=0
	{
		char:=SubStr(arithmetic, currPos, 1)
		if(char==")"){
			parenthesis--
		}else if(char=="("){
			parenthesis++
		}
		currPos++
	}
	nameLength:=Strlen(match1)
	inner:=SubStr(arithmetic, foundPos+nameLength, currPos-foundPos-nameLength)
	StringLeft, left_arithmetic, arithmetic, foundPos - 1
	StringRight, right_arithmetic, arithmetic, StrLen(arithmetic) - currPos + 1
	return parseFunctions(left_arithmetic . inner . alias_%match1% . right_arithmetic)
}

;parse input data
arg1=%1%
SplitPath, arg1,,,input_extension
if(input_extension!="txt"){
	ExitApp
}
FileRead, input_arithmetic, %arg1%
input_arithmetic:=parseFunctions(input_arithmetic)
;convert negated values to -1 * that value
input_arithmetic:=RegExReplace(input_arithmetic, "([\^\*\(\/\-\+])\s*\-", "$11{F9}*")
input_arithmetic:=RegExReplace(input_arithmetic, "^\s*\-", "1{F9}*")
;convert exponent operation to hotkey
StringReplace, input_arithmetic, input_arithmetic, ^, y
;get operations ready for autohotkey send
input_arithmetic:=RegExReplace(input_arithmetic, "([\+\-\*\/])", "{$1}")
MsgBox, %input_arithmetic%

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

;switch to scientific
Send, !2
Sleep, 500

;do the calculation
Send, {Esc}
Send, %input_arithmetic%
Send, {=}

Sleep, 300

;get result and output it
ControlGetText, result, Static4

FileDelete, %arg2%
FileAppend, %result%, %arg2%
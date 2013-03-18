; testing GUI capabilities of script recording for Polyglot
; Changes:
;	(0) add domain and inputs form to beginning of script - domain line is of format "domain image", inputs line format is "inputs jpg, bmp, xfig, tiff"
;	(1) add the option of reporting which were the input and output files used during the script recording
;	(2) remove the "obligatory step" option for now

; TODO list:
;	(1) deal with param that has options as a group of radio buttons - this is a bit complicate because 
;	groups of radio buttons (mutually exclusive) should be grouped under one single param, but how the user
;	will record each one of the options? after that, it is pretty much like a ddl, where the user has a predefined list of possible values
;	(2) advanced: if nothing works during the feedback phase, ask people to give words to be excluded from the win title/text
;	(3) to discuss: maybe the best default method should be based on the text of the controls, that way if in a new version a new control is added (like a new item in a menu list), 
;	the old script may be still usable 
 
#SingleInstance force


fileCount := 0
formatCount := 0
guiCount := 1
;debugging:
GoSub StartDebug
;GoSub Start
Return


WorkingSet:
	Gui %guiCount%:Destroy
	working_gui := guiCount
	guiCount += 1
	Gui %working_gui%:+AlwaysOnTop
	Gui %working_gui%:Add, Text,, Please mark all output formats you are currently working on:
	Loop %formatCount%
	{
		el := Format%A_Index%
		Gui %working_gui%:Add, Checkbox, vActive%A_Index%, %el%
		File%A_Index% := WorkingDirectory . "\" . el . "\"
	}
	GoSub StartLines
	;for obligatory steps:
	;Gui %working_gui%:Add, Checkbox, ym vAlways, Recording obligatory steps
	Gui %working_gui%:Add, Button, ym gWorkingSetStart, Start
	Gui %working_gui%:Add, Button, ym gWorkingSetStop, Stop
	Gui %working_gui%:Add, Button, ym gWorkingSetFinished, Finished!
	Gui %working_gui%:Show, x0 y0, Formats ;%working_gui%
	GuiControl Disable, Stop
Return



WorkingSetStart:
	Gui %working_gui%:Submit, NoHide
	GuiControl Disable, Start
	GuiControl Enable, Stop
	Loop %formatCount%
	{
		el := Format%A_Index%
		GuiControl Disable, %el%	
	}
	;for obligatory steps:
	;GuiControl Disable, Always
	
	GoSub PrepareParams
	
	fileCount += 1
	Hotkey ^LButton, ChooseControl, On
Return

WorkingSetStop:
	GuiControl Enable, Start
	GuiControl Disable, Stop
	Loop %formatCount%
	{
		el := Format%A_Index%
		GuiControl Enable, %el%	
	}
	;for obligatory steps:
	;GuiControl Enable, Always
	Hotkey ^LButton, Off
Return

ChooseControl:
	MouseGetPos, CurrentControlX, CurrentControlY, WindowID, CurrentControl
	WinGetClass, CurrentWindowClass, ahk_id %WindowID%
	WinGetTitle, CurrentWindowTitle, ahk_id %WindowID%
	Gui %guiCount%:Add, DropDownList, vControlType, Menu|Button|CheckBox|Insert Text|Dropdown Menu|Scrollbar|Save Text ;|Wait
	Gui %guiCount%:Add, Checkbox, Checked0 vIsParam, Is a Parameter
	; TODO deal with random pop-ups - add to guiCount dropdownlist popup window
	Gui %guiCount%:Add, Button, ym gFillControlForm, Fill Form
	Gui %guiCount%:Show, w300 xCenter y0, Choose control type ;%guiCount%
Return

Test:
	MsgBox win is %win% `ncommand is %command% `ncomment is %comment%
Return



FillControlForm:
	Gui %guiCount%:Submit
	Gui %guiCount%:Destroy
	method:=1
	if (ControlType = "Menu")
	{
		Gui %guiCount%:Add, Text,, 
		Gui %guiCount%:Add, Text,, Menu:
		Gui %guiCount%:Add, Text,, SubMenu 1:
		Gui %guiCount%:Add, Text,, SubMenu 2:
		Gui %guiCount%:Add, Text,, SubMenu 3:
		Gui %guiCount%:Add, Text,, SubMenu 4:
		Gui %guiCount%:Add, Text,, SubMenu 5:
		Gui %guiCount%:Add, Text,, SubMenu 6:
		
		Gui %guiCount%:Add, Text, ym, Text
		Gui %guiCount%:Add, Edit, vMenu1
		Gui %guiCount%:Add, Edit, vSubMenu1
		Gui %guiCount%:Add, Edit, vSubMenu2
		Gui %guiCount%:Add, Edit, vSubMenu3
		Gui %guiCount%:Add, Edit, vSubMenu4
		Gui %guiCount%:Add, Edit, vSubMenu5
		Gui %guiCount%:Add, Edit, vSubMenu6
		
		Gui %guiCount%:Add, Text, ym, Number
		Gui %guiCount%:Add, Edit, vMenu1Num
		Gui %guiCount%:Add, Edit, vSubMenu1Num
		Gui %guiCount%:Add, Edit, vSubMenu2Num
		Gui %guiCount%:Add, Edit, vSubMenu3Num
		Gui %guiCount%:Add, Edit, vSubMenu4Num
		Gui %guiCount%:Add, Edit, vSubMenu5Num
		Gui %guiCount%:Add, Edit, vSubMenu6Num
		
		if (IsParam = 1)
		{
			Gui %guiCount%:Add, Text,, Parameter Name
			Gui %guiCount%:Add, DropDownList, Sort vParamName, %ParamsToPresent%
		}
		Gui %guiCount%: Add, Button, gMenuFeedback, Try it
		Gui %guiCount%: Add, Button, gFillMenu, Done
		Gui %guiCount%: Add, Button, gWrongControl, Ooops Wrong Choice
		Gui %guiCount%:Show, xCenter y0, Menu Info ;%guiCount%
	}
	else if (ControlType = "Button")
	{
		Gui %guiCount%:Add, Text,, Button Text
		Gui %guiCount%:Add, Edit, w300 vButtonText
		
		if (IsParam = 1)
		{
			Gui %guiCount%:Add, Text,, Parameter Name
			Gui %guiCount%:Add, DropDownList, Sort vParamName, %ParamsToPresent%
		}
		Gui %guiCount%: Add, Button, gButtonFeedback, Try it
		Gui %guiCount%: Add, Button, gFillButton, Done
		Gui %guiCount%: Add, Button, gWrongControl, Ooops Wrong Choice
		Gui %guiCount%:Show, xCenter y0, Button Info ;%guiCount%
	}
	else if (ControlType = "CheckBox")
	{
		Gui %guiCount%:Add, Text,, Checkbox Text
		Gui %guiCount%:Add, Edit, w300 vCheckboxText
		
		if (IsParam = 1)
		{
			Gui %guiCount%:Add, Text,, Parameter Name
			Gui %guiCount%:Add, DropDownList, w300 Sort vParamName, %ParamsToPresent%
		}
		else
		{
			Gui %guiCount%:Add, Text,, Value (Checked: 1, Unchecked: 0)
			Gui %guiCount%:Add, Edit, vValue
		}
		Gui %guiCount%: Add, Button, gCheckboxFeedback, Try it
		Gui %guiCount%: Add, Button, gFillCheckbox, Done
		Gui %guiCount%: Add, Button, gWrongControl, Ooops Wrong Choice
		Gui %guiCount%:Show, xCenter y0, Checkbox Info ;%guiCount%
	}
	else if (ControlType = "Insert Text")
	{
		Gui %guiCount%:Add, Text,, Insert Text Title
		Gui %guiCount%:Add, Edit, w300 vTextTitle
		
		if (IsParam = 1)
		{
			t:=1
			Gui %guiCount%:Add, Text,, Parameter Name
			Gui %guiCount%:Add, DropDownList, Sort vParamName, %ParamsToPresent%
			Gui %guiCount%:Add, Checkbox, gParamRange vParamRange, this is a range
			Gui %guiCount%:Add, Text,, Minimum
			Gui %guiCount%:Add, Edit, vMinParam
			Gui %guiCount%:Add, Text,, Maximum
			Gui %guiCount%:Add, Edit, vMaxParam
			
			
			GuiControl Disable, Minimum
			GuiControl Disable, MinParam
			GuiControl Disable, Maximum
			GuiControl Disable, MaxParam	
			
			
			Gui %guiCount%:Add, Text,, Text for trying
			Gui %guiCount%:Add, Edit, vValue
				
		}
		else
		{
			Gui %guiCount%:Add, Text,, Text to Insert
			Gui %guiCount%:Add, Edit, vValue
			Gui %guiCount%:Add, Checkbox, Checked0 vinputFile, This is the input file
			Gui %guiCount%:Add, Checkbox, Checked0 voutputFile, This is the output file
			Gui %guiCount%:Add, Text,, If you did check any of checkbox please write the whole path of the file used
			Gui %guiCount%:Add, Edit, vFileUsed
		}
		Gui %guiCount%: Add, Button, gTextFeedback, Try it
		Gui %guiCount%: Add, Button, gFillInserText, Done
		Gui %guiCount%: Add, Button, gWrongControl, Ooops Wrong Choice
		Gui %guiCount%:Show, xCenter y0, Insert Text Info ;%guiCount%
	}
	else if (ControlType = "Dropdown Menu")
	{

		if (IsParam = 1)
		{
			;Gui %guiCount%:Add, Text,, Parameter Name
			;Gui %guiCount%:Add, DropDownList, Sort vParamName, %ParamsToPresent%
			GoSub ParamDropdown
		}
		else
		{
			GoSub SimpleDropdown
		}

	}
	else if (ControlType = "Scrollbar")
	{
		ScrollbarText :=""
		ScrollbarMin :=""
		ScrollbarMax :=""
		ParamName :=""
		Value :=""
		
		Gui %guiCount%:Add, Text,, Scrollbar Text
		Gui %guiCount%:Add, Edit, w300 vScrollbarText

		Gui %guiCount%:Add, Text,, Scrollbar Range Min
		Gui %guiCount%:Add, Edit, vScrollbarMin
		
		Gui %guiCount%:Add, Text,, Scrollbar Range Max
		Gui %guiCount%:Add, Edit, vScrollbarMax
		
		if (IsParam = 1)
		{
			Gui %guiCount%:Add, Text,, Parameter Name
			Gui %guiCount%:Add, DropDownList, Sort vParamName, %ParamsToPresent%
		}
		else
		{
			Gui %guiCount%:Add, Text,, Value
			Gui %guiCount%:Add, Edit, vValue
		}
		Gui %guiCount%: Add, Button, gScrollbarFeedback, Try it
		Gui %guiCount%: Add, Button, gFillScrollbar, Done
		Gui %guiCount%: Add, Button, gWrongControl, Ooops Wrong Choice
		Gui %guiCount%:Show, xCenter y0, Scrollbar Info ;%guiCount%
	}
	else if (ControlType = "Wait")
	{
		;GoSub FillWait
		Gui %guiCount%:Add, Text, w300 , Please select the type of wait desired
		Gui %guiCount%:Add, DropDownList, w280 vWaitType, Until text is present on status bar|Until text is present on clicked Window|Until image is present on clicked Window (not recommended)|Until clicked Window appears|For pre-defined amount of Time
		Gui %guiCount%:Add, Button, gWaitOK, OK
		Gui %guiCount%:Add, Button, gWrongControl, Ooops Wrong Choice
		Gui %guiCount%:Show, xCenter y0, Wait for Event ;%guiCount%
	}
	else if (ControlType = "Save Text")
	{
		if (IsParam = 1)
		{
			Gui %guiCount%:Add, Text,, Parameter Name
			Gui %guiCount%:Add, DropDownList, Sort vParamName, %ParamsToPresent%
		}
		Gui %guiCount%:Add, Text, w300, Please enter the name of the file where we should store this text
		Gui %guiCount%:Add, Edit, vFileName

		Gui %guiCount%:Add, Text,, Title Text
		Gui %guiCount%:Add, Edit, vTitleText
		
		Gui %guiCount%: Add, Button, gSaveTextFeedback, Try it
		Gui %guiCount%:Add, Button, gSaveTextOK, Done
		Gui %guiCount%:Add, Button, gWrongControl, Ooops Wrong Choice
		Gui %guiCount%:Show, xCenter y0, Save Text ;%guiCount%		
	}
Return


ParamRange:
	If (t = 1)
	{
		t := 0
		GuiControl Enable, Minimum
		GuiControl Enable, MinParam
		GuiControl Enable, Maximum
		GuiControl Enable, MaxParam
	}
	else
	{
		t := 1
		GuiControl Disable, Minimum
		GuiControl Disable, MinParam
		GuiControl Disable, Maximum
		GuiControl Disable, MaxParam	
	}
Return


;----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
;-------------------------------------------- STARTING UP -------------------------------------------------------------------------------------------------------------------------
;----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

StartDebug:
	Gui Add, Text,, Folder:
	Gui Add, Text,, Program Name:
	Gui Add, Text,, Program Path:
	Gui Add, Text,, Program Domain:
	Gui Add, Text,, Program Input Formats:
	Gui Add, Text,, Params File Path:
	Gui Add, Edit, vFolder ym, C:\temp
	Gui Add, Edit, vProgram, IrfanView
	Gui Add, Edit, vProgramPath, C:\Program Files\IrfanView\i_view32.exe
	Gui Add, Edit, vProgramDomain, Image
	Gui Add, Edit, vProgramInputFormats, ani, cur, avi, wmv, asf, b3d, bmp, dib, rle,
	Gui Add, Edit, vParamFilePath, C:\ImageParamNames.txt
	Gui Add, Button, gStartOK Default, OK
	Gui Add, Button, gCancelExit, Cancel
	Gui Show,, Script Builder ;%guiCount%
Return


Start:
	Gui Add, Text,, Folder:
	Gui Add, Text,, Program Name:
	Gui Add, Text,, Program Path:
	Gui Add, Text,, Program Domain:
	Gui Add, Text,, Program Input Formats:
	Gui Add, Text,, Params File Path:
	Gui Add, Edit, w400 vFolder ym
	Gui Add, Edit, w400 vProgram
	Gui Add, Edit, w400 vProgramPath
	Gui Add, Edit, w400 vProgramDomain
	Gui Add, Edit, w400 vProgramInputFormats
	Gui Add, Edit, w400 vParamFilePath
	Gui Add, Button, gStartOK ym, OK
	Gui Add, Button, gCancelExit, Cancel
	Gui Show,, AHK Path Recorder ;%guiCount%
Return

StartOK:
	Gui Submit
	IfNotExist %Folder%
		FileCreateDir %Folder%
	IfNotExist %Folder%\%Program%
		FileCreateDir %Folder%\%Program%
	WorkingDirectory := Folder . "\" . Program
	GoSub AddFormats
Return


AddFormats:
	Gui %guiCount%:Destroy
	Loop %formatCount%
	{
		Gui %guiCount%:Add, Text,, format %A_Index%:
	}
	Gui %guiCount%:Add, Text,, add format:
	Loop %formatCount%
	{
		el := Format%A_Index%
		if (A_Index=1)
			Gui %guiCount%:Add, Text,ym , %el%
		else
			Gui %guiCount%:Add, Text,, %el%
	}
	formatCount += 1
	Gui %guiCount%:Add, Edit, vCurrFormat
	Gui %guiCount%:Add, Button, ym gFormatsDone, Done
	Gui %guiCount%:Add, Button, gAddMoreFormats, Add more Format(s)
	Gui %guiCount%:Show,, Please list all output formats: ;%guiCount%
Return


FormatsDone:
	Gui %guiCount%:Submit
	if (CurrFormat = "")
	{
		formatCount -= 1
		if (formatCount = 0)
		{
			Gui %guiCount%:Destroy
			Gui %guiCount%:Add, Text,, You need to define at least one output format
			Gui %guiCount%:Add, Button, gFormatsCancelOK, OK
			Gui %guiCount%:Add, Button, gFormatsCancelExit, Leave
			Gui %guiCount%:Show,, ;%guiCount%
		}
		else
		{
			Gui %guiCount%:Destroy
			GoSub ReadParamFile
		}
	}
	else
	{
		Format%formatCount% := CurrFormat
		IfNotExist %WorkingDirectory%\%CurrFormat%
			FileCreateDir %WorkingDirectory%\%CurrFormat%
		Gui %guiCount%:Destroy
		GoSub ReadParamFile
	}
Return


ReadParamFile:

	FileRead, ParamList, %ParamFilePath%
	StringReplace, ParamList, ParamList, `r`n, `n, All
	ParamList := RegExReplace(ParamList, "^(\s)+")
	ParamList := RegExReplace(ParamList, "`n(\s)+", "`n")
	ParamList := RegExReplace(ParamList, "(\s)+`n", "`n")
	ParamList := RegExReplace(ParamList, "(\s)+$")
	ParamList := RegExReplace(ParamList, "`n+", "`n")
	StringReplace, ParamList, ParamList, `n, |, All
	StringRight, Char, ParamList, 1
	if (Char!="|")
		ParamList := ParamList . "|"

	Loop %formatCount%
	{
		FormatParams%A_Index% := ParamList
	}

	AllParams := ParamList
	StringSplit, AllParams, ParamList , |
	AllParamsNum := AllParams0 - 1

	GoSub WorkingSet
Return




FormatsCancelOK:
	Gui %guiCount%:Destroy
	GoSub AddFormats
Return

AddMoreFormats:
	Gui %guiCount%:Submit
	Format%formatCount%:=CurrFormat
	IfNotExist %WorkingDirectory%\%CurrFormat%
		FileCreateDir %WorkingDirectory%\%CurrFormat%
	;Gui %guiCount%:Destroy
	GoSub AddFormats
Return




;----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
;-------------------------------------------- GENERAL STUFF -----------------------------------------------------------------------------------------------------------------------
;----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
FillWin:
	win := "win """
	if (CurrentWindowTitle != "")
		win := win . CurrentWindowTitle
	if (CurrentWindowClass != "")
		win := win . " ahk_class " . CurrentWindowClass
	win := win . """"
Return

WriteToFile:
	this_suffix := ""
	suf_count := guiCount - 2
	if (suf_count < 0) ;shouldn't happen, but if there is a small hidden bug we don't want the program to crash
		suf_count := 0
	Loop %suf_count%
	{
		this_suffix := "`t" . this_suffix 
	}
	Loop %formatCount%
	{
		if (Active%A_Index% = 1)
		{
			f := File%A_Index%
			;for obligatory steps:
			;ff := f . fileCount . "_" . Always  . ".txt"
			ff := f . fileCount . ".txt"
			StringSplit lines, roadlines, `n
			Loop %lines0%
			{
				this_line := lines%A_Index%
				this_line := this_suffix . this_line . "`n"
				FileAppend %this_line%, %ff%
			}
		}
	}
Return


StartLines:
	start_line := "run " . Program . " """ . ProgramPath . """`n"
	domain_line := "domain """ . ProgramDomain . """`n"
	input_format_line := "inputs """ . ProgramInputFormats . """`n"
	
	
	Loop %formatCount%
	{
		f:= File%A_Index%
		;for obligatory steps:
		;ff := f . "0_1.txt"
		ff := f . "0.txt"
		FileAppend %start_line%, %ff%
		FileAppend %domain_line%, %ff%
		FileAppend %input_format_line%, %ff%
	}
Return


WrongControl:
	Gui %guiCount%: Destroy
Return

CancelExit:
WorkingSetFinished:
FormatsCancelExit:
	ExitApp
Return


;----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
;----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
;**********************************************************************************************************************************************************************************
;**********************************************************************************************************************************************************************************
;                                         CONTROL OPTIONS
;**********************************************************************************************************************************************************************************
;**********************************************************************************************************************************************************************************
;----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
;----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------


;----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
;-------------------------------------------- MENU --------------------------------------------------------------------------------------------------------------------------------
;----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

MenuFeedback:
	Gui %guiCount%:Submit, NoHide
	feedbackWin := ""
	if (CurrentWindowTitle != "")
		feedbackWin := feedbackWin . CurrentWindowTitle
	if (CurrentWindowClass != "")
		feedbackWin := feedbackWin . " ahk_class " . CurrentWindowClass
	m_count := 0
	Loop 6
	{
		if (SubMenu%A_Index%Num != "")
		{
			m_count := A_Index
		}
	}

	WinActivate %feedbackWin%
	if (m_count=0)
		WinMenuSelectItem %feedbackWin%,, %Menu1Num%&
	else if (m_count=1)
		WinMenuSelectItem %feedbackWin%,, %Menu1Num%&, %SubMenu1Num%&
	else if (m_count=2)
		WinMenuSelectItem %feedbackWin%,, %Menu1Num%&, %SubMenu1Num%&, %SubMenu2Num%&
	else if (m_count=3)
		WinMenuSelectItem %feedbackWin%,, %Menu1Num%&, %SubMenu1Num%&, %SubMenu2Num%&, %SubMenu3Num%&
	else if (m_count=4)
		WinMenuSelectItem %feedbackWin%,, %Menu1Num%&, %SubMenu1Num%&, %SubMenu2Num%&, %SubMenu3Num%&, %SubMenu4Num%&
	else if (m_count=5)
		WinMenuSelectItem %feedbackWin%,, %Menu1Num%&, %SubMenu1Num%&, %SubMenu2Num%&, %SubMenu3Num%&, %SubMenu4Num%&, %SubMenu5Num%&
	else if (m_count=6)
		WinMenuSelectItem %feedbackWin%,, %Menu1Num%&, %SubMenu1Num%&, %SubMenu2Num%&, %SubMenu3Num%&, %SubMenu4Num%&, %SubMenu5Num%&, %SubMenu6Num%&	
		
	guiCount += 1
	Gui %guiCount%:Add, Text, w300, If it worked please press OK
	Gui %guiCount%:Add, Button, gMenuFeedbackOK, OK
	Gui %guiCount%:Add, Text, w300, If it did not work please double check that all text and numbers entered are correct.
	Gui %guiCount%:Add, Text, w300, If it wasn't correct please press OK to return to previous screen, fix the text and try it again.
	Gui %guiCount%:Add, Text, w300, If it WAS correct and it DIDN'T work anyway please press method 2 to try the second method	
	Gui %guiCount%:Add, Button, gMenuFeedbackTry2, method 2
	Gui %guiCount%:Show
Return


MenuFeedbackTry2:
	Gui %guiCount%: Destroy
	WinActivate %feedbackWin%
	if (m_count=0)
		WinMenuSelectItem %feedbackWin%,, %Menu1%
	else if (m_count=1)
		WinMenuSelectItem %feedbackWin%,, %Menu1%, %SubMenu1%
	else if (m_count=2)
		WinMenuSelectItem %feedbackWin%,, %Menu1%, %SubMenu1%, %SubMenu2%
	else if (m_count=3)
		WinMenuSelectItem %feedbackWin%,, %Menu1%, %SubMenu1%, %SubMenu2%, %SubMenu3%
	else if (m_count=4)
		WinMenuSelectItem %feedbackWin%,, %Menu1%, %SubMenu1%, %SubMenu2%, %SubMenu3%, %SubMenu4%
	else if (m_count=5)
		WinMenuSelectItem %feedbackWin%,, %Menu1%, %SubMenu1%, %SubMenu2%, %SubMenu3%, %SubMenu4%, %SubMenu5%
	else if (m_count=6)
		WinMenuSelectItem %feedbackWin%,, %Menu1%, %SubMenu1%, %SubMenu2%, %SubMenu3%, %SubMenu4%, %SubMenu5%, %SubMenu6%	
	
	Gui %guiCount%:Add, Text, w300, If it worked please press OK so your script will use this method
	Gui %guiCount%:Add, Button, gMenuFeedbackOK2, OK
	Gui %guiCount%:Add, Text, w300, If it did not work please double check that all text and numbers entered are correct. If it is all correct, you may have to fix your script manually - please take note of the menu so it will be easier to locate it in the final script.
	Gui %guiCount%:Add, Button, gMenuFeedbackCancel, Cancel
	Gui %guiCount%:Show
Return

MenuFeedbackOK2:
	method:=2
	Gui %guiCount%: Destroy
	guiCount -= 1
Return

MenuFeedbackCancel:
MenuFeedbackOK:
	method:=1
	Gui %guiCount%: Destroy
	guiCount -= 1
Return


FillMenu:
	Gui %guiCount%:Submit
	GoSub FillWin
	m_count := 0
	comment :=""
	if (method =1)
	{
		command := "menu1 """ . Menu1Num . "&"""
		comment := "comment " . Menu1Num . "&=" . Menu1 . " "
		Loop 6
		{
			if (SubMenu%A_Index%Num != "")
			{
				command := command . " """ . SubMenu%A_Index%Num . "&"""
				comment := comment . SubMenu%A_Index%Num . "&=" . SubMenu%A_Index% . " "
				m_count := A_Index
			}
		}
	}
	else
	{
		command := "menu2 """ . Menu1 . """"
		Loop 6
		{
			if (SubMenu%A_Index% != "")
			{
				command := command . " """ . SubMenu%A_Index% . """"
				m_count := A_Index
			}
		}
	}
	roadlines := win . "`n" . command . "`n" . comment
	if (IsParam = 1)
	{
		if (m_count = 0)
			roadlines := "param ""Menu"" """ . ParamName . """ """ . Menu1 . """`n" . roadlines . "`n"
		else
			roadlines := "param ""Menu"" """ . ParamName . """ """ . SubMenu%m_count% . """`n" . roadlines . "`n"
	}
	GoSub WriteToFile
	
	Gui %guiCount%:Destroy
	if (IsParam = 1)
	{
		GoSub RemoveParam
		GoSub MenuParam
	}
	
Return


MenuParam:
	Gui %guiCount%:Add, Text, w300 , Please press Start to record any other steps/params that can be reached ONLY as a consequence of parameter %ParamName% being chosen or not. When finished press Stop. If there is nothing to be done just press Start and then Stop. You have to record it to both checked/unchecked options.
	Gui %guiCount%:Add, Radio, Checked vMenuParam%guiCount%, Chosen
	Gui %guiCount%:Add, Radio,, Not Chosen
	Gui %guiCount%:Add, Button, gMenuParamStartRecording, Start
	Gui %guiCount%:Add, Button, gMenuParamStopRecording, Stop
	Gui %guiCount%:Add, Button, gMenuParamFinishRecording, Finish
	
	GuiControl Disable, Stop
	GuiControl Disable, Finish
	
	MenuParamCount%guiCount% := 0
	
	Gui %guiCount%:Show, xCenter y0, Recording after Menu parameter %ParamName% ;%guiCount%
Return


MenuParamStartRecording:
	Gui %guiCount%:Submit, NoHide
	mp := MenuParam%guiCount%
	if (mp = 1)
		roadlines := "#`n""true""`n" ;Clicked
	else
		roadlines := "#`n""false""`n" ;Not Clicked
	GoSub WriteToFile
	guiCount += 1
	GuiControl Disable, Start
	GuiControl Disable, Chosen
	GuiControl Disable, Not Chosen
	GuiControl Enable, Stop
Return

MenuParamStopRecording:
	guiCount -= 1
	mp := MenuParam%guiCount%
	GuiControl Disable, Stop
	if (mp = 1) ;Clicked was chosen
	{
		GuiControl,, Chosen, 0
		GuiControl Disable, Chosen
		GuiControl,, Not Chosen, 1
		GuiControl Enable, Not Chosen
	}
	else
	{
		GuiControl,, Not Chosen, 0
		GuiControl Disable, Not Chosen
		GuiControl,, Chosen, 1
		GuiControl Enable, Chosen
	}
	mpc := MenuParamCount%guiCount%
	mpc += 1
	MenuParamCount%guiCount% := mpc
	
	if (mpc = 2)
	{
		GuiControl Disable, Not Chosed
		GuiControl Disable, Chosen
		GuiControl Enable, Finish
	}
	else
	{
		GuiControl Enable, Start
	}
Return

MenuParamFinishRecording:
	Gui %guiCount%:Destroy
	roadlines := "#`nEND`n"
	GoSub WriteToFile
Return


;----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
;-------------------------------------------- BUTTON ------------------------------------------------------------------------------------------------------------------------------
;----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

ButtonFeedback:
	Gui %guiCount%:Submit, NoHide
	feedbackWin := ""
	if (CurrentWindowTitle != "")
		feedbackWin := feedbackWin . CurrentWindowTitle
	if (CurrentWindowClass != "")
		feedbackWin := feedbackWin . " ahk_class " . CurrentWindowClass
	
	WinActivate %feedbackWin%
	ControlClick %CurrentControl%, %feedbackWin%

	guiCount += 1
	Gui %guiCount%:Add, Text, w300, If it worked please press OK
	Gui %guiCount%:Add, Button, gButtonFeedbackOK, OK
	Gui %guiCount%:Add, Text, w300, If it did not work please double check that all text and numbers entered are correct.
	Gui %guiCount%:Add, Text, w300, If it wasn't correct please press OK to return to previous screen, fix the text and try it again.
	Gui %guiCount%:Add, Text, w300, If it WAS correct and it DIDN'T work anyway please press method 2 to try the second method	
	Gui %guiCount%:Add, Button, gButtonFeedbackTry2, method 2
	Gui %guiCount%:Show
Return


ButtonFeedbackTry2:
	Gui %guiCount%: Destroy
	WinActivate %feedbackWin%
	ControlClick %ButtonText%, %feedbackWin%
	Gui %guiCount%:Add, Text, w300, If it worked please press OK so your script will use this method
	Gui %guiCount%:Add, Button, gButtonFeedbackOK2, OK
	Gui %guiCount%:Add, Text, w300, If it did not work please double check that all text and numbers entered are correct.
	Gui %guiCount%:Add, Text, w300, If it wasn't correct please press OK to return to previous screen, fix the text and try it again.
	Gui %guiCount%:Add, Text, w300, If it WAS correct and it DIDN'T work anyway please press method 2 to try the second method	
	Gui %guiCount%:Add, Button, gButtonFeedbackTry3, method 3
	Gui %guiCount%:Show
Return


ButtonFeedbackTry3:
	Gui %guiCount%: Destroy
	WinActivate %feedbackWin%
	ControlClick, x%CurrentControlX% y%CurrentControlY%, %feedbackWin%
	Gui %guiCount%:Add, Text, w300, If it worked please press OK so your script will use this method
	Gui %guiCount%:Add, Button, gButtonFeedbackOK3, OK
	Gui %guiCount%:Add, Text, w300, If it did not work please double check that all text and numbers entered are correct. If it is all correct, you may have to fix your script manually - please take note of the button so it will be easier to locate it in the final script.
	Gui %guiCount%:Add, Button, gButtonFeedbackCancel, Cancel
	Gui %guiCount%:Show
Return

ButtonFeedbackOK2:
	method:=2
	Gui %guiCount%: Destroy
	guiCount -= 1
Return

ButtonFeedbackOK3:
	method:=3
	Gui %guiCount%: Destroy
	guiCount -= 1
Return

ButtonFeedbackCancel:
ButtonFeedbackOK:
	method:=1
	Gui %guiCount%: Destroy
	guiCount -= 1
Return



FillButton:
	Gui %guiCount%:Submit
	GoSub FillWin
	comment:=""
	if (method=1)
	{
		command := "click " . """" . CurrentControl . """"
		if (ButtonText != "")
			comment := "comment " . CurrentControl . "=" . ButtonText
	}
	else if (method=2)
	{
		command := "click " . """" . ButtonText . """"
	}
	else
	{
		command := "click " . """x" . CurrentControlX . " y" . CurrentControlY . """"
		if (ButtonText != "")
			comment := "comment " . CurrentControlX . ", " . CurrentControlY . "=" . ButtonText
	}
	roadlines := win . "`n" . command . "`n" . comment
	if (IsParam = 1)
	{
		roadlines := "param ""Button"" """ . ParamName . """ """ . ButtonText . """`n" . roadlines . "`n" 
	}
	GoSub WriteToFile
	Gui %guiCount%:Destroy
	if (IsParam = 1)
	{
		GoSub RemoveParam
		GoSub ButtonParam
	}
Return

ButtonParam:
	Gui %guiCount%:Add, Text, w300 , Please press Start to record any other steps/params that can be reached ONLY as a consequence of parameter %ParamName% being clicked or not. When finished press Stop. If there is nothing to be done just press Start and then Stop. You have to record it to both checked/unchecked options.
	Gui %guiCount%:Add, Radio, Checked vButtonParam%guiCount%, Clicked
	Gui %guiCount%:Add, Radio,, Not Clicked
	Gui %guiCount%:Add, Button, gButtonParamStartRecording, Start
	Gui %guiCount%:Add, Button, gButtonParamStopRecording, Stop
	Gui %guiCount%:Add, Button, gButtonParamFinishRecording, Finish
	
	GuiControl Disable, Stop
	GuiControl Disable, Finish
	
	ButtonParamCount%guiCount% := 0
	
	Gui %guiCount%:Show, xCenter y0, Recording after Button parameter %ParamName% ;%guiCount%
Return


ButtonParamStartRecording:
	Gui %guiCount%:Submit, NoHide
	bp := ButtonParam%guiCount%
	if (bp = 1)
		roadlines := "#`n""true""`n" ;Clicked
	else
		roadlines := "#`n""false""`n" ;Not Clicked
	GoSub WriteToFile
	guiCount += 1
	GuiControl Disable, Start
	GuiControl Disable, Clicked
	GuiControl Disable, Not Clicked
	GuiControl Enable, Stop
Return

ButtonParamStopRecording:
	guiCount -= 1
	bp := ButtonParam%guiCount%
	GuiControl Disable, Stop
	if (bp = 1) ;Clicked was chosen
	{
		GuiControl,, Clicked, 0
		GuiControl Disable, Clicked
		GuiControl,, Not Clicked, 1
		GuiControl Enable, Not Clicked
	}
	else
	{
		GuiControl,, Not Clicked, 0
		GuiControl Disable, Not Clicked
		GuiControl,, Clicked, 1
		GuiControl Enable, Clicked
	}
	bpc := ButtonParamCount%guiCount%
	bpc += 1
	ButtonParamCount%guiCount% := bpc
	
	if (bpc = 2)
	{
		GuiControl Disable, Not Clicked
		GuiControl Disable, Clicked
		GuiControl Enable, Finish
	}
	else
	{
		GuiControl Enable, Start
	}
Return

ButtonParamFinishRecording:
	Gui %guiCount%:Destroy
	roadlines := "#`nEND`n"
	GoSub WriteToFile
Return










;----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
;-------------------------------------------- CHECKBOX ----------------------------------------------------------------------------------------------------------------------------
;----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

;Control, Check,, %CurrentControl%

CheckboxFeedback:
	Gui %guiCount%:Submit, NoHide
	feedbackWin := ""
	if (CurrentWindowTitle != "")
		feedbackWin := feedbackWin . CurrentWindowTitle
	if (CurrentWindowClass != "")
		feedbackWin := feedbackWin . " ahk_class " . CurrentWindowClass
	

	WinActivate %feedbackWin%
	MsgBox Trying to Check Checkbox
	Control, Check,, %CurrentControl%, %feedbackWin%
	MsgBox Trying to Uncheck Checkbox
	Control, Uncheck,, %CurrentControl%, %feedbackWin%
	
	guiCount += 1
	Gui %guiCount%:Add, Text, w300, If it worked please press OK
	Gui %guiCount%:Add, Button, gCheckboxFeedbackOK, OK
	Gui %guiCount%:Add, Text, w300, If it did not work please double check that all text and numbers entered are correct.
	Gui %guiCount%:Add, Text, w300, If it wasn't correct please press OK to return to previous screen, fix the text and try it again.
	Gui %guiCount%:Add, Text, w300, If it WAS correct and it DIDN'T work anyway please press method 2 to try the second method	
	Gui %guiCount%:Add, Button, gCheckboxFeedbackTry2, method 2
	Gui %guiCount%:Show
Return


CheckboxFeedbackTry2:
	Gui %guiCount%: Destroy
	WinActivate %feedbackWin%
		
	MsgBox Trying to Check Checkbox
	Control, Check,, %CheckboxText%, %feedbackWin%
	MsgBox Trying to Uncheck Checkbox
	Control, Uncheck,, %CheckboxText%, %feedbackWin%	
		
	Gui %guiCount%:Add, Text, w300, If it worked please press OK so your script will use this method
	Gui %guiCount%:Add, Button, gCheckboxFeedbackOK2, OK
	Gui %guiCount%:Add, Text, w300, If it did not work please double check that all text and numbers entered are correct. If it is all correct, you may have to fix your script manually - please take note of the checkbox so it will be easier to locate it in the final script.
	Gui %guiCount%:Add, Button, gCheckboxFeedbackCancel, Cancel
	Gui %guiCount%:Show
Return

CheckboxFeedbackOK2:
	method:=2
	Gui %guiCount%: Destroy
	guiCount -= 1
Return

CheckboxFeedbackCancel:
CheckboxFeedbackOK:
	method:=1
	Gui %guiCount%: Destroy
	guiCount -= 1
Return



FillCheckbox:
	Gui %guiCount%:Submit
	GoSub FillWin
	comment :=""
	if (CheckboxText != "")
	{	
		if (method=1)
			comment := "comment " . CurrentControl . "=" . CheckboxText
	}
	if (IsParam = 1)
	{
		if (method=1)
			command := "checkbox " . """" . CurrentControl . """"
		else
			command := "checkbox " . """" . CheckboxText . """"
			
		roadlines := "param ""Checkbox"" """ . ParamName . """ """ . CheckboxText . """`n" . win . "`n" . command . "`n" . comment
	}
	else
	{	
		if (method=1)
			command := "select " . """" . CurrentControl . """ """ . Value . """"  
		else
			command := "select " . """" . CheckboxText . """ """ . Value . """"  

		roadlines := win . "`n" . command . "`n" . comment
	}
	Gui %guiCount%:Destroy
	GoSub WriteToFile
	if (IsParam = 1)
	{
		GoSub RemoveParam
		GoSub CheckboxParam
	}
Return

CheckboxParam:
	Gui %guiCount%:Add, Text, w300 , Please press Start to record any other steps/params that can be reached ONLY after the parameter %ParamName% is selected. When finished press Stop. If there is nothing to be done just press Start and then Stop. You have to record it to both checked/unchecked options.
	Gui %guiCount%:Add, Radio, Checked vCheckboxParam%guiCount%, Checked
	Gui %guiCount%:Add, Radio,, Unchecked
	Gui %guiCount%:Add, Button, gCheckboxParamStartRecording, Start
	Gui %guiCount%:Add, Button, gCheckboxParamStopRecording, Stop
	Gui %guiCount%:Add, Button, gCheckboxParamFinishRecording, Finish
	
	GuiControl Disable, Stop
	GuiControl Disable, Finish
	
	CheckboxParamCount%guiCount% := 0
	
	Gui %guiCount%:Show, xCenter y0, Recording after Checkbox parameter %ParamName% ;%guiCount%
Return


CheckboxParamStartRecording:
	Gui %guiCount%:Submit, NoHide
	cp := CheckboxParam%guiCount%
	if (cp = 1)
		roadlines := "#`n""true""`n" ;checked
	else
		roadlines := "#`n""false""`n" ;unchecked
	GoSub WriteToFile
	guiCount += 1
	GuiControl Disable, Start
	GuiControl Disable, Checked
	GuiControl Disable, Unchecked
	GuiControl Enable, Stop
Return

CheckboxParamStopRecording:
	guiCount -= 1
	cp := CheckboxParam%guiCount%
	GuiControl Disable, Stop
	if (cp = 1) ;checked was chosen
	{
		GuiControl,, Checked, 0
		GuiControl Disable, Checked
		GuiControl,, Unchecked, 1
		GuiControl Enable, Unchecked
	}
	else
	{
		GuiControl,, Unchecked, 0
		GuiControl Disable, Unchecked
		GuiControl,, Checked, 1
		GuiControl Enable, Checked
	}
	cpc := CheckboxParamCount%guiCount%
	cpc += 1
	CheckboxParamCount%guiCount% := cpc
	
	if (cpc = 2)
	{
		GuiControl Disable, Unchecked
		GuiControl Disable, Checked
		GuiControl Enable, Finish
	}
	else
	{
		GuiControl Enable, Start
	}
Return

CheckboxParamFinishRecording:
	Gui %guiCount%:Destroy
	roadlines := "#`nEND`n"
	GoSub WriteToFile
Return

;----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
;-------------------------------------------- TEXT --------------------------------------------------------------------------------------------------------------------------------
;----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
;(1)ControlSetText, %CurrentControl%, %Value%, %feedbackWin%
;(2)Control, EditPaste, %Value%, %CurrentControl%, %feedbackWin%
;(3)ControlSendRaw , %CurrentControl%, %Value%, %feedbackWin%
;better not to use
;(4) Activate %feedbackWin%
;    SendInput %Value%


TextFeedback:
	Gui %guiCount%:Submit, NoHide
	feedbackWin := ""
	if (CurrentWindowTitle != "")
		feedbackWin := feedbackWin . CurrentWindowTitle
	if (CurrentWindowClass != "")
		feedbackWin := feedbackWin . " ahk_class " . CurrentWindowClass
	
	WinActivate %feedbackWin%
	ControlSetText, %CurrentControl%, %Value%, %feedbackWin%

	guiCount += 1
	Gui %guiCount%:Add, Text, w300, If it worked please press OK
	Gui %guiCount%:Add, Button, gTextFeedbackOK, OK
	Gui %guiCount%:Add, Text, w300, If it did not work please double check that all text and numbers entered are correct.
	Gui %guiCount%:Add, Text, w300, If it wasn't correct please press OK to return to previous screen, fix the text and try it again.
	Gui %guiCount%:Add, Text, w300, If it WAS correct and it DIDN'T work anyway please press method 2 to try the second method	
	Gui %guiCount%:Add, Button, gTextFeedbackTry2, method 2
	Gui %guiCount%:Show
Return


TextFeedbackTry2:
	Gui %guiCount%: Destroy
	WinActivate %feedbackWin%
	Control, EditPaste, %Value%, %CurrentControl%, %feedbackWin%
	Gui %guiCount%:Add, Text, w300, If it worked please press OK so your script will use this method
	Gui %guiCount%:Add, Button, gTextFeedbackOK2, OK
	Gui %guiCount%:Add, Text, w300, If it did not work please double check that all text and numbers entered are correct.
	Gui %guiCount%:Add, Text, w300, If it wasn't correct please press OK to return to previous screen, fix the text and try it again.
	Gui %guiCount%:Add, Text, w300, If it WAS correct and it DIDN'T work anyway please press method 2 to try the second method	
	Gui %guiCount%:Add, Button, gTextFeedbackTry3, method 3
	Gui %guiCount%:Show
Return

TextFeedbackTry3:
	Gui %guiCount%: Destroy
	WinActivate %feedbackWin%
	ControlSendRaw, %CurrentControl%, %Value%, %feedbackWin%
	Gui %guiCount%:Add, Text, w300, If it worked please press OK so your script will use this method
	Gui %guiCount%:Add, Button, gTextFeedbackOK3, OK
	Gui %guiCount%:Add, Text, w300, If it did not work please double check that all text and numbers entered are correct.
	Gui %guiCount%:Add, Text, w300, If it wasn't correct please press OK to return to previous screen, fix the text and try it again.
	Gui %guiCount%:Add, Text, w300, If it WAS correct and it DIDN'T work anyway please press method 2 to try the second method	
	Gui %guiCount%:Add, Button, gTextFeedbackTry4, method 4
	Gui %guiCount%:Show
Return

TextFeedbackTry4:
	Gui %guiCount%: Destroy
	WinActivate %feedbackWin%
	SendInput {Raw}%Value%
	Gui %guiCount%:Add, Text, w300, If it worked please press OK so your script will use this method
	Gui %guiCount%:Add, Button, gTextFeedbackOK4, OK
	Gui %guiCount%:Add, Text, w300, If it did not work please double check that all text and numbers entered are correct. If it is all correct, you may have to fix your script manually - please take note of the text so it will be easier to locate it in the final script.
	Gui %guiCount%:Add, Button, gTextFeedbackCancel, Cancel
	Gui %guiCount%:Show
Return

TextFeedbackOK2:
	method:=2
	Gui %guiCount%: Destroy
	guiCount -= 1
Return

TextFeedbackOK3:
	method:=3
	Gui %guiCount%: Destroy
	guiCount -= 1
Return

TextFeedbackOK4:
	method:=4
	Gui %guiCount%: Destroy
	guiCount -= 1
Return


TextFeedbackCancel:
TextFeedbackOK:
	method:=1
	Gui %guiCount%: Destroy
	guiCount -= 1
Return




FillInserText:
	Gui %guiCount%:Submit
	GoSub FillWin
	comment :=""
	if (TextTitle != "")
		comment := "comment " . CurrentControl . "=" . TextTitle
	if (IsParam = 1)
	{
		command := "userinput method " . method . " """ . CurrentControl . """"
		if (ParamRange=1)
			command := command . " range"
		if (minParam != "")
			command := command . " minVal " . minParam
		if (maxParam != "")
			command := command . " maxVal " . maxParam
		roadlines := "param ""UserInput"" """ . ParamName . """ """ . TextTitle . """`n" . win . "`n" . command . "`n" . comment . "`n#`nany`n"
	}
	else
	{	
		command := "insert " . """" . Value . """ method " . method . " """ . CurrentControl . """"  
		extra := ""
		if (inputFile = 1)
		{
			extra := "`ninputfile """ . FileUsed . """ `n"
			extra := extra . "comment the input file used during script recording"
		}
		if (outputFile = 1)
		{
			extra := extra . "`noutputfile """ . FileUsed . """ `n"
			extra := extra . "comment the output file used during script recording"
		}
		roadlines := win . "`n" . command . "`n" . comment . extra

	}
	
	Gui %guiCount%:Destroy
	GoSub WriteToFile
	if (IsParam = 1)
	{
		GoSub RemoveParam
		GoSub UserInputParam
	}
Return


UserInputParam:
	Gui %guiCount%:Add, Text, w300 , Please press Start to record any other steps/params that can be reached ONLY after the parameter %ParamName% is selected. When finished press Stop. If there is nothing to be done just press Start and then Stop.
	Gui %guiCount%:Add, Button, gUserInputParamStartRecording, Start
	Gui %guiCount%:Add, Button, gUserInputParamStopRecording, Stop
	GuiControl Disable, Stop
	Gui %guiCount%:Show, xCenter y0, Recording after UserInput parameter %ParamName% ;%guiCount%
Return

UserInputParamStartRecording:
	guiCount += 1
	GuiControl Disable, Start
	GuiControl Enable, Stop
Return

UserInputParamStopRecording:
	guiCount -= 1
	Gui %guiCount%:Destroy
	roadlines := "#`nEND`n"
	GoSub WriteToFile
Return



;----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
;-------------------------------------------- DROPDOWN MENU -----------------------------------------------------------------------------------------------------------------------
;----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
;Control, Choose
;ChooseString, String
;controlsend

DropDownFeedback:
	Gui %guiCount%:Submit, NoHide
	feedbackWin := ""
	if (CurrentWindowTitle != "")
		feedbackWin := feedbackWin . CurrentWindowTitle
	if (CurrentWindowClass != "")
		feedbackWin := feedbackWin . " ahk_class " . CurrentWindowClass
	
	WinActivate %feedbackWin%
	Control, Choose, %ItemNum%, %CurrentControl%, %feedbackWin% 

	guiCount += 1
	Gui %guiCount%:Add, Text, w300, If it worked please press OK
	Gui %guiCount%:Add, Button, gDropDownFeedbackOK, OK
	Gui %guiCount%:Add, Text, w300, If it did not work please double check that all text and numbers entered are correct.
	Gui %guiCount%:Add, Text, w300, If it wasn't correct please press OK to return to previous screen, fix the text and try it again.
	Gui %guiCount%:Add, Text, w300, If it WAS correct and it DIDN'T work anyway please press method 2 to try the second method	
	Gui %guiCount%:Add, Button, gDropDownFeedbackTry2, method 2
	Gui %guiCount%:Show
Return


DropDownFeedbackTry2:
	Gui %guiCount%: Destroy
	WinActivate %feedbackWin%
	Control, ChooseString, %ItemText%, %CurrentControl%, %feedbackWin% 
	
	Gui %guiCount%:Add, Text, w300, If it worked please press OK so your script will use this method
	Gui %guiCount%:Add, Button, gDropDownFeedbackOK2, OK
	Gui %guiCount%:Add, Text, w300, If it did not work please double check that all text and numbers entered are correct.
	Gui %guiCount%:Add, Text, w300, If it wasn't correct please press OK to return to previous screen, fix the text and try it again.
	Gui %guiCount%:Add, Text, w300, If it WAS correct and it DIDN'T work anyway please press method 2 to try the second method	
	Gui %guiCount%:Add, Button, gDropDownFeedbackTry3, method 3
	Gui %guiCount%:Show
Return

DropDownFeedbackTry3:
	Gui %guiCount%: Destroy
	WinActivate %feedbackWin%
	ControlSendRaw, %CurrentControl%, %ItemText%, %feedbackWin%
	Gui %guiCount%:Add, Text, w300, If it worked please press OK so your script will use this method
	Gui %guiCount%:Add, Button, gDropDownFeedbackOK3, OK
	Gui %guiCount%:Add, Text, w300, If it did not work please double check that all text and numbers entered are correct. If it is all correct, you may have to fix your script manually - please take note of the text so it will be easier to locate it in the final script.
	Gui %guiCount%:Add, Button, gDropDownFeedbackCancel, Cancel
	Gui %guiCount%:Show
Return

DropDownFeedbackOK2:
	method:=2
	Gui %guiCount%: Destroy
	guiCount -= 1
Return

DropDownFeedbackOK3:
	method:=3
	Gui %guiCount%: Destroy
	guiCount -= 1
Return

DropDownFeedbackCancel:
DropDownFeedbackOK:
	method:=1
	Gui %guiCount%: Destroy
	guiCount -= 1
Return




SimpleDropdown:
	Gui %guiCount%:Add, Text,, Dropdown Text
	Gui %guiCount%:Add, Edit, vDropdownText			
	Gui %guiCount%:Add, Text,, Text of item to choose
	Gui %guiCount%:Add, Edit, vItemText
	Gui %guiCount%:Add, Text,, Number of item to choose
	Gui %guiCount%:Add, Edit, vItemNum
	Gui %guiCount%:Add, Button, gDropDownFeedback, Try it
	Gui %guiCount%:Add, Button, gFillDropdown, Done
	Gui %guiCount%:Add, Button, gWrongControl, Ooops Wrong Choice
	Gui %guiCount%:Show, xCenter y0, Dropdown Menu Info ;%guiCount%
Return

FillDropdown:
	Gui %guiCount%:Submit
	GoSub FillWin
	if (method=1)
	{
		command := "choose method " . method . " """ . CurrentControl . """ """ . ItemNum . """"     
		if (DropdownText != "")
				comment := "comment " . CurrentControl . "=" . DropdownText . " " . ItemNum . "=" . ItemText
	}
	else
	{
		command := "choose method " . method . " """ . CurrentControl . """ """ . ItemText . """"     
	}
	roadlines := win . "`n" . command . "`n" . comment
	Gui %guiCount%:Destroy
	GoSub WriteToFile
Return


ParamDropdown:
	dropdownCount%guiCount% := 0
	SingleParamRecordCount%guiCount% := 0
	Gui %guiCount%:Add, Text,, Dropdown Text
	Gui %guiCount%:Add, Edit, vDropdownText			
	Gui %guiCount%:Add, Text,, Param Name
	Gui %guiCount%:Add, DropDownList, Sort vParamName, %ParamsToPresent%
	Gui %guiCount%:Add, Button, gParamDropdown2, Done
	Gui %guiCount%:Add, Button, gWrongControl, Ooops Wrong Choice
	
	Gui %guiCount%:Add, Text,, Text of item to choose for trying
	Gui %guiCount%:Add, Edit, vItemText
	Gui %guiCount%:Add, Text,, Number of item to choose for trying
	Gui %guiCount%:Add, Edit, vItemNum
	Gui %guiCount%:Add, Button, gDropDownFeedback, Try it
	Gui %guiCount%:Show, xCenter y0, Dropdown Menu Info ;%guiCount%
Return	


ParamDropdown2:
	Gui %guiCount%:Submit
	GoSub FillWin
	command := "userchoose method " . method . " """ . CurrentControl . """"
	if (DropdownText != "")
		comment := "comment " . CurrentControl . "=" . DropdownText
	roadlines :=  "param ""Dropdown"" """ . ParamName . """ """ . DropdownText . """`n" . win . "`n" . command . "`n" . comment
	GoSub WriteToFile
	Gui %guiCount%:Destroy
	GoSub RemoveParam
	GoSub AddDDParams
Return


RecordDDParams:
	Gui %guiCount%: Destroy
	c := dropdownCount%guiCount%
	Loop %c%
	{
		el := DDParams%guiCount%_%A_Index%
		if (A_Index = 1)
			Gui %guiCount%:Add, Radio, Checked vChosenParam%guiCount%, %el%
		else
			Gui %guiCount%:Add, Radio,, %el%
	}
	Gui %guiCount%:Add, Button, gStartRecordSingleParam, Start
	Gui %guiCount%:Add, Button, Disable gStopRecordSingleParam, Stop
	Gui %guiCount%:Add, Button, Disable gFinishedRecording, Finished All
	GuiControl Disable, Stop
	GuiControl Disable, Finished All
	Gui %guiCount%:Show, xCenter y0, ;%guiCount%
Return

StartRecordSingleParam:
	Gui %guiCount%:Submit, NoHide
	pnum := ChosenParam%guiCount%
	p := DDParams%guiCount%_%pnum%
	pn := DDParamsNum%guiCount%_%pnum%
	
	if (p = "")
		return
	GuiControl Disable, Start
	GuiControl Enable, Stop
	roadlines := "#`n""" . p . """ """ . pn . """`n"
	GoSub WriteToFile
	
	c := dropdownCount%guiCount%
	Loop %c%
	{
		p := DDParams%guiCount%_%A_Index%
		GuiControl Disable, %p%					
	}
	guiCount += 1	
Return

StopRecordSingleParam:
	guiCount -= 1 
	
	GuiControl Disable, Stop
	pnum := ChosenParam%guiCount%
	p := DDParams%guiCount%_%pnum%
	DDDisab%guiCount%_%pnum% := 1
	GuiControl,, %p%, 0
	
	sprc := SingleParamRecordCount%guiCount% 
	sprc += 1
	SingleParamRecordCount%guiCount% := sprc
	
	c := dropdownCount%guiCount%
	Loop %c%
	{
		if (DDDisab%guiCount%_%A_Index% != 1)
		{
			p := DDParams%guiCount%_%A_Index%
			GuiControl Enable, %p%
		}			
	}
	if (c = sprc)
	{
		GuiControl Enable, Finished All
	}
	else
	{
		GuiControl Enable, Start
	}
	
	
Return

FinishedRecording:
	Gui %guiCount%: Destroy
	roadlines := "#`nEND`n"
	GoSub WriteToFile
Return

AddDDParams:
	new_col := 0
	c := dropdownCount%guiCount%
	Loop %c%
	{
		Gui %guiCount%:Add, Text,, option %A_Index%:
	}
	Gui %guiCount%:Add, Text,, add option text:
	Gui %guiCount%:Add, Text,, add option number:
	Loop %c%
	{
		el := DDParams%guiCount%_%A_Index%
		if (A_Index=1)
		{
			new_col := 1
			Gui %guiCount%:Add, Text,ym , %el%
		}
		else
			Gui %guiCount%:Add, Text,, %el%
	}
	c += 1
	dropdownCount%guiCount% := c
	if (new_col = 0)
		Gui %guiCount%:Add, Edit, ym vCurrParamName
	else
		Gui %guiCount%:Add, Edit, vCurrParamName	
	Gui %guiCount%:Add, Edit, vCurrParamNum
	Gui %guiCount%:Add, Button, ym gParamsDone, Done
	Gui %guiCount%:Add, Button, gAddMoreParams, Add more option(s)
	Gui %guiCount%:Show, xCenter y0, Please list all dropdown options: ;%guiCount%
Return

ParamsDone:
	Gui %guiCount%:Submit
	if (CurrParamName = "")
	{
		c := dropdownCount%guiCount%
		c -= 1
		dropdownCount%guiCount% := c
		if (c = 0)
		{
			Gui %guiCount%:Destroy
			pd_form_gui_error := guiCount + 1
			Gui %pd_form_gui_error%:Add, Text,, You need to define at least one option
			Gui %pd_form_gui_error%:Add, Button, gParamsCancelOK, OK
			Gui %pd_form_gui_error%:Show,, ;%guiCount%
		}
		else
		{
			Gui %guiCount%:Destroy
			GoSub RecordDDParams
		}
	}
	else
	{
		c := dropdownCount%guiCount%
		DDParams%guiCount%_%c% := CurrParamName
		DDParamsNum%guiCount%_%c% := CurrParamNum
		Gui %guiCount%:Destroy
		GoSub RecordDDParams
	}
Return

ParamsCancelOK:
	Gui %pd_form_gui_error%:Destroy
	Gui %guiCount%:Destroy
	GoSub AddDDParams
Return

AddMoreParams:
	Gui %guiCount%:Submit
	c := dropdownCount%guiCount%
	DDParams%guiCount%_%c% := CurrParamName
	DDParamsNum%guiCount%_%c% := CurrParamNum
	Gui %guiCount%:Destroy
	GoSub AddDDParams
Return



;----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
;-------------------------------------------- SCROLLBAR ---------------------------------------------------------------------------------------------------------------------------
;----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------



ScrollbarFeedback:
	Gui %guiCount%:Submit, NoHide
	feedbackWin := ""
	if (CurrentWindowTitle != "")
		feedbackWin := feedbackWin . CurrentWindowTitle
	if (CurrentWindowClass != "")
		feedbackWin := feedbackWin . " ahk_class " . CurrentWindowClass
		
	if (ScrollbarMax="")
		ScrollbarMax:=100
	if (ScrollbarMin="")
		ScrollbarMin:=0
	if (Value="")
		Value:=33

	left_clicks := ScrollbarMax - ScrollbarMin
	right_clicks := Value - ScrollbarMin
	
	WinActivate %feedbackWin%	
	Loop, %left_clicks%
		ControlSend, %CurrentControl%, {Left}, %feedbackWin%
	Loop, %right_clicks%
		ControlSend, %CurrentControl%,{Right}, %feedbackWin%
	
	guiCount += 1
	Gui %guiCount%:Add, Text, w300, If it worked please press OK
	Gui %guiCount%:Add, Button, gScrollbarFeedbackOK, OK
	Gui %guiCount%:Add, Text, w300, If it did not work please double check that all text and numbers entered are correct.
	Gui %guiCount%:Add, Text, w300, If it wasn't correct please press OK to return to previous screen, fix the text and try it again.
	Gui %guiCount%:Add, Text, w300, If it WAS correct and it DIDN'T work anyway please press OK to return to the previous screen - be aware that you may have to fix your script manually	
	Gui %guiCount%:Show
Return


ScrollbarFeedbackOK:
	method:=1
	Gui %guiCount%: Destroy
	guiCount -= 1
Return



FillScrollbar:
	Gui %guiCount%:Submit
	GoSub FillWin
	comment :=""
	if (ScrollbarText != "")
		comment := "comment " . CurrentControl . "=" . ScrollbarText
	if (IsParam = 1)
	{
		command := "userscrollbar " . """" . CurrentControl . """ minRange " . """" . ScrollbarMin . """ maxRange " . """" . ScrollbarMax . """"     
		roadlines := "param ""Scrollbar"" """ . ParamName . """ """ . ScrollbarText . """`n" . win . "`n" . command . "`n" . comment . "`n#`nany`n"		
	}
	else
	{
		command := "scrollbar " . """" . Value . """ " . """" . CurrentControl . """ minRange " . """" . ScrollbarMin . """ maxRange " . """" . ScrollbarMax . """"     
		roadlines := win . "`n" . command . "`n" . comment		
	}
	Gui %guiCount%:Destroy
	GoSub WriteToFile
	if (IsParam = 1)
	{
		GoSub RemoveParam
		GoSub ScrollParam
	}
Return


ScrollParam:
	Gui %guiCount%:Add, Text, w300 , Please press Start to record any other steps/params that can be reached ONLY if/after the parameter %ParamName% is selected. When finished press Stop. If there is nothing to be done just press Start and then Stop.
	Gui %guiCount%:Add, Button, gScrollParamStartRecording, Start
	Gui %guiCount%:Add, Button, gScrollParamStopRecording, Stop
	GuiControl Disable, Stop
	Gui %guiCount%:Show, xCenter y0, Recording after Scroll parameter %ParamName% ;%guiCount%
Return


ScrollParamStartRecording:
	guiCount += 1
	GuiControl Disable, Start
	GuiControl Enable, Stop
Return

ScrollParamStopRecording:
	guiCount -= 1
	Gui %guiCount%:Destroy
	roadlines := "#`nEND`n"
	GoSub WriteToFile
Return


;----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
;-------------------------------------------- WAIT --------------------------------------------------------------------------------------------------------------------------------
;----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

;FillWait:
;	Gui %guiCount%:Add, Text, w300 , Please select the type of wait desired
;	Gui %guiCount%:Add, DropDownList, vWaitType, Until text is present on status bar|Until text is present on clicked Window|Until image is present on clicked Window (not recommended)|Until clicked Window appears|For pre-defined amount of Time
;	Gui %guiCount%:Add, Button, gWaitOK, OK
;	Gui %guiCount%:Add, Button, gWrongControl, Ooops Wrong Choice
;Return

WaitOK:
	Gui %guiCount%:Submit
	Gui %guiCount%:Destroy
	if (WaitType = "Until text is present on status bar")
	{
		Gui %guiCount%:Add, Text, w300 , Text to Wait:
		Gui %guiCount%:Add, Edit, vTextToWait
		Gui %guiCount%:Add, Button, gFillWaitStatus, OK
		Gui %guiCount%:Add, Button, gWrongControl, Ooops Wrong Choice
		Gui %guiCount%:Show, xCenter y0, Wait until text is present on statusbar ;%guiCount%
	}
	if (WaitType = "Until text is present on clicked Window")
	{
		Gui %guiCount%:Add, Text, w300 , Text to Wait:
		Gui %guiCount%:Add, Edit, vTextToWait
		Gui %guiCount%:Add, Button, gFillWaitText, OK
		Gui %guiCount%:Add, Button, gWrongControl, Ooops Wrong Choice
		Gui %guiCount%:Show, xCenter y0, Wait until text appears on window ;%guiCount%
	}
	else if (WaitType = "Until image is present on clicked Window (not recommended)")
	{
		Gui %guiCount%:Add, Text, w300 , Image File Name:
		Gui %guiCount%:Add, Edit, vImageToWait
		Gui %guiCount%:Add, Button, gFillWaitImage, OK	
		Gui %guiCount%:Add, Button, gWrongControl, Ooops Wrong Choice
		Gui %guiCount%:Show, xCenter y0, Wait until image appears on window ;%guiCount%
	}
	else if (WaitType = "Until clicked Window appears")
	{
		Gui %guiCount%:Add, Text, w300 , Please click OK if you want the program to wait for the Window to appear 
		Gui %guiCount%:Add, Button, gFillWaitWindow, OK	
		Gui %guiCount%:Add, Button, gWrongControl, Ooops Wrong Choice
		Gui %guiCount%:Show, xCenter y0, Wait until clicked window appears ;%guiCount%
		;AQUI
	}
	else ;For pre-defined amount of Time
	{
		Gui %guiCount%:Add, Text, w300 , Amount of time:
		Gui %guiCount%:Add, Edit, vTimeToWait
		Gui %guiCount%:Add, Button, gFillWaitTime, OK		
		Gui %guiCount%:Add, Button, gWrongControl, Ooops Wrong Choice
		Gui %guiCount%:Show
	}
Return

FillWaitStatus:
	Gui %guiCount%:Submit
	GoSub FillWin
	comment:="comment wait for status"
	command := "wait status """ . TextToWait . """" 
	roadlines := win . "`n" . command . "`n" . comment
	Gui %guiCount%:Destroy
	GoSub WriteToFile
Return

FillWaitText:
	Gui %guiCount%:Submit
	GoSub FillWin
	comment:="comment wait for text"
	command := "wait text """ . TextToWait . """" 
	roadlines := win . "`n" . command . "`n" . comment
	Gui %guiCount%:Destroy
	GoSub WriteToFile
Return

FillWaitImage:
	Gui %guiCount%:Submit
	GoSub FillWin
	comment:="comment wait for image"
	command := "wait image """ . ImageToWait . """"
	roadlines := win . "`n" . command . "`n" . comment
	Gui %guiCount%:Destroy
	GoSub WriteToFile
Return

FillWaitWindow:
	Gui %guiCount%:Submit
	GoSub FillWin
	comment:="comment wait for window"
	command := ""
	roadlines := win . "`n" . command . "`n" . comment
	Gui %guiCount%:Destroy
	GoSub WriteToFile
Return

FillWaitTime:
	Gui %guiCount%:Submit
	comment:="comment wait for time"
	command := "wait time """ . TimeToWait . """"	
	roadlines := win . "`n" . command . "`n" . comment
	Gui %guiCount%:Destroy
	GoSub WriteToFile
Return


;----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
;-------------------------------------------- SAVE TEXT ---------------------------------------------------------------------------------------------------------------------------
;----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

SavetextFeedback:

	Gui %guiCount%:Submit, NoHide
	feedbackWin := ""
	if (CurrentWindowTitle != "")
		feedbackWin := feedbackWin . CurrentWindowTitle
	if (CurrentWindowClass != "")
		feedbackWin := feedbackWin . " ahk_class " . CurrentWindowClass
	
	WinActivate %feedbackWin%
	ControlGetText, tmp_res, %CurrentControl%, %feedbackWin%
	FileAppend, %tmp_res%, %FileName%

	guiCount += 1
	Gui %guiCount%:Add, Text, w300, If it worked please press OK
	Gui %guiCount%:Add, Button, gSaveTextFeedbackOK, OK
	Gui %guiCount%:Add, Text, w300, If it did not work please double check that all text and numbers entered are correct.
	Gui %guiCount%:Add, Text, w300, If it wasn't correct please press OK to return to previous screen, fix the text and try it again.
	Gui %guiCount%:Add, Text, w300, If it WAS correct and it DIDN'T work anyway please press OK to return to the previous screen - be aware that you may have to fix your script manually	
	Gui %guiCount%:Show
Return


SaveTextFeedbackOK:
	method:=1
	Gui %guiCount%: Destroy
	guiCount -= 1
Return



SaveTextOK:
	Gui %guiCount%:Submit
	GoSub FillWin
	comment :=""
	if (TextTitle != "")
		comment := "comment " . CurrentControl . "=" . TextTitle
	
	command := "savetext " . """" . CurrentControl . """ """ . FileName . """"  
	
	if (IsParam = 1)
	{
		roadlines := "param ""SaveText"" """ . ParamName . """ """ . TextTitle . """`n" . win . "`n" . command . "`n" . comment . "`n#`n"
	}
	else
	{	
		roadlines := win . "`n" . command . "`n" . comment
	}
	
	Gui %guiCount%:Destroy
	GoSub WriteToFile
	if (IsParam = 1)
	{
		GoSub RemoveParam
		GoSub SaveTextParam
	}
Return


SaveTextParam:
	Gui %guiCount%:Add, Text, w300 , Please press Start to record any other steps/params that can be reached ONLY after the parameter %ParamName% is selected. When finished press Stop. If there is nothing to be done just press Start and then Stop.
	Gui %guiCount%:Add, Button, gSaveTextParamStartRecording, Start
	Gui %guiCount%:Add, Button, gSaveTextParamStopRecording, Stop
	GuiControl Disable, Stop
	Gui %guiCount%:Show, xCenter y0, Recording after Save Text parameter %ParamName% ;%guiCount%
Return

SaveTextParamStartRecording:
	guiCount += 1
	GuiControl Disable, Start
	GuiControl Enable, Stop
Return

SaveTextParamStopRecording:
	guiCount -= 1
	Gui %guiCount%:Destroy
	roadlines := "#`nEND`n"
	GoSub WriteToFile
Return



RemoveParam:
	Loop %formatCount%
	{
		if (Active%A_Index% = 1)
		{
			StringReplace, FormatParams%A_Index%, FormatParams%A_Index%, %ParamName%| 

		}
	}
	StringReplace, ParamsToPresent, ParamsToPresent, %ParamName%| 
Return

PrepareParams:
	ParamsToPresent :=""
	Loop %AllParamsNum%
	{
		choose:=1
		param := AllParams%A_Index%
		Loop %formatCount%
			{
				if (Active%A_Index% = 1)
				{
					p := FormatParams%A_Index%
					IfNotInString, p, %param%
					{
						choose:=0
						break
					}	
				}
		}
		if (choose=1)
			ParamsToPresent := param . "|" . ParamsToPresent 
	}
Return



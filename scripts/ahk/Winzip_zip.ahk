;Winzip (v8.1)
;takes any single file and zips it

;parse input file extension
inputfile=%1%
SplitPath, inputfile,,,input_extension

if(input_extension=="xps"){
	;winzip has a strange interaction with xps files. It opens them as an archive.
	ExitApp
}

;parse output file extension
outputfile=%2%
SplitPath, outputfile,,,output_extension

if(output_extension!="zip"){
	ExitApp
}

;run program
Run, "C:\Program Files (x86)\WinZip\WINZIP32.EXE" "%inputfile%"

;wait for window
WinWait, WinZip
WinWaitActive, Add

;create archive
ControlSetText, Edit1, %outputfile%
ControlSend, Edit1, {Enter}

;wait for main window and exit
WinWaitActive, WinZip
WinClose
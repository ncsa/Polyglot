;Microsoft Office Word (2010)

;Activate the window
SetTitleMatchMode, 2
WinActivate, Microsoft Word
WinWaitActive, Microsoft Word

Send, ^a

Send, +^f

WinActivate, Font
WinWaitActive, Font

ControlSetText, RichEdit20W5,
ControlFocus, RichEdit20W5
Sleep, 300
Send, Regular

ControlSend, RichEdit20W5, {Enter}
;Microsoft Word (2010)

;Activate the window
SetTitleMatchMode, 2
WinActivate, Microsoft Word
WinWaitActive, Microsoft Word

Send, ^a

Send, +^F

WinWaitActive, Font

ControlSetText, RichEdit20W6, 8

ControlSend, RichEdit20W6, {Enter}

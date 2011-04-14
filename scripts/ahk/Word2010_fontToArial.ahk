;Microsoft Office Word (2010)

;Activate the window
SetTitleMatchMode, 2
WinActivate, Microsoft Word
WinWaitActive, Microsoft Word

Send, ^a

Send, +^F

WinWaitActive, Font

ControlSetText, RICHEDIT20W1, Arial

ControlSend, RICHEDIT20W1, {Enter}

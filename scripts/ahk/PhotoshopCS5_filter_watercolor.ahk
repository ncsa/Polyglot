;Adobe Photoshop CS5

wintitle=ahk_class Photoshop

;check if application is running
IfWinNotExist, %wintitle%
{
	ExitApp
}

;activate window
WinActivate, %wintitle%
WinWaitActive

;filter menu
Send, !t

;move down one to bypass the dynamic option
Send, {down}

;select filter
Send, a{right}w

;manage dialog
WinWaitActive, Watercolor
ControlSend, Button1, {Enter}

;wait for main window before exiting
WinWaitActive, %wintitle%
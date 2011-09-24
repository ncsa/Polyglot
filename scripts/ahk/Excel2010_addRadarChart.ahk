;Microsoft Excel (2010)

;Activate the window
SetTitleMatchMode, 2
WinActivate, Microsoft Excel
WinWaitActive, Microsoft Excel

;move cursor to upper left cell
Send ^{up 2}^{left 2}

;navigate gui
Send, !no{down 4}{Enter}
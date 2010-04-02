;IrfanView (v4.25)

;Kill any scripts that could be using this application first
RunWait, taskkill /f /im IrfanView_convert.exe

;Kill the application
RunWait, taskkill /f /im i_view32.exe
;Adobe Acrobat (v9.3.0 Pro Extended)

;Kill any scripts that could be using this application first
RunWait, taskkill /f /im Acrobat_open.exe
RunWait, taskkill /f /im Acrobat_save.exe

;Kill the application
RunWait, taskkill /f /im Acrobat.exe
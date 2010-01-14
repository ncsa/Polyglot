;OpenOffice (v3.1.0)

;Kill any scripts that could be using this application first
RunWait, taskkill /f /im OpenOffice_convert.exe

;Kill the application
RunWait, taskkill /f /im soffice.exe
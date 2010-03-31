;3Ds Max (v11.0, 2009 Educational)

;Kill any scripts that could be using this application first
RunWait, taskkill /f /im 3DS_open.exe
RunWait, taskkill /f /im 3DS_import.exe
RunWait, taskkill /f /im 3DS_save.exe
RunWait, taskkill /f /im 3DS_export.exe

;Kill the application
RunWait, taskkill /f /im 3dsmax.exe
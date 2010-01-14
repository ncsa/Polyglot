;ABBYY FineReader (v9.0 Professional Edition)

;Kill any scripts that could be using this application first
RunWait, taskkill /f /im ABBYY_open.exe
RunWait, taskkill /f /im ABBYY_save.exe

;Kill the application
RunWait, taskkill /f /im FineReader.exe
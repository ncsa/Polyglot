;Google SketchUp (v7.0)

;Kill any scripts that could be using this application first
RunWait, taskkill /f /im GSkUp_import.exe
RunWait, taskkill /f /im GSkUp_save.exe
RunWait, taskkill /f /im GSkUp_export.exe

;Kill the application
RunWait, taskkill /f /im SketchUp.exe
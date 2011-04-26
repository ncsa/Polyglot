;VTK (v5.2.1)

;Kill any scripts that could be using this application first
RunWait, taskkill /f /im VTK_convert.ahk

;Kill the application
RunWait, taskkill /f /im vtk.exe

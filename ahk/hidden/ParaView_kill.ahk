;ParaView (v3.4.0)

;Kill any scripts that could be using this application first
RunWait, taskkill /f /im ParaView_open.exe
RunWait, taskkill /f /im ParaView_save.exe
RunWait, taskkill /f /im ParaView_export.exe

;Kill the application
RunWait, taskkill /f /im paraview.exe
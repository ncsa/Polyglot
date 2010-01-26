;Cyberware PlyTool (v1.7)

;Kill any scripts that could be using this application first
RunWait, taskkill /f /im CyW-c_convert_stl_ply.exe

;Kill the application
RunWait, taskkill /f /im stl2ply.exe
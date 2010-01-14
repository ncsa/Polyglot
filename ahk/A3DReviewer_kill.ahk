;Adobe 3D Reviewer (v9)

;Kill any scripts that could be using this application first
RunWait, taskkill /f /im A3DReviewer_open.exe
RunWait, taskkill /f /im A3DReviewer_export.exe

;Kill the application
RunWait, taskkill /f /im A3DReviewer.exe
;ImageMagick (v6.5.2)

;Kill any scripts that could be using this application first
;RunWait, taskkill /f /im ImgMgk_convert.ahk

;Kill the application
RunWait, taskkill /f /im convert.exe

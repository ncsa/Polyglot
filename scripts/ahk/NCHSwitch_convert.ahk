;NCH Switch
;audio
;wav, mp3, mp2, mpga, au, aif, aiff, gsm, dct, vox, rarw, ogg, flac, amr, wma, wmv, aac, m4a, mid, rm, ra, ram, sri, shn, cda, avi, mpg, mpeg, m3u, pls
;wav, mp3, au, aiff, gsm, vox, raw, rss, m3u, pls, wpl, amr, aac, wma

arg1 = %1%
arg2 = %2%

;Parse output folder path
StringGetPos, path_index, arg1, \, R
path_index += 1
tmp_path := SubStr(arg1, 1, path_index)

;Parse output format
StringGetPos, index, arg2, ., R
ifLess, index, 0, ExitApp
index += 2
out := SubStr(arg2, index)

;Run Program
RunWait, "C:\Program Files\NCH Swift Sound\Switch\switch.exe" -hide -convert "%arg1%" -format ".%out%" -exit -outfolder "%tmp_path%

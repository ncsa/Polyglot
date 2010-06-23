;FFMPEG (SVN-r18709)
;video
;avi, flv, mpeg (flv, h263, h264, mpeg1video, mpeg2video, mpeg4, wmv1, wmv2, wmv3)
;avi, flv, mov, mp4, mpeg (flv, h263, mpeg1video, mpeg2video, mpeg4, wmv1, wmv2)

;Get arguments and correct slashes in path
arg1 = %1%
StringReplace arg1, arg1, \, /, All
arg2 = %2%
StringReplace arg2, arg2, \, /, All

;Remove option information from input
StringGetPos, index, arg1, `;, R

if(index > 0){
	arg1 := SubStr(arg1, 1, index)
}

;Remove option information from output
StringGetPos, index, arg2, `;, R
codec := ""

if(index > 0){
	codec := SubStr(arg2, index+2)
	arg2 := SubStr(arg2, 1, index)
}

;Run program
if(codec = ""){
	RunWait, "C:\Program Files\WinFF\ffmpeg.exe" -y -i "%arg1%" "%arg2%"
}else{
	RunWait, "C:\Program Files\WinFF\ffmpeg.exe" -y -i "%arg1%" -vcodec %codec% "%arg2%"
}

;VideoLAN (v0.9.9)
;video
;asf, avi, divs, dv, flv, mov, mpeg1, mpeg2, mpeg4, ogg, vob, wmv
;

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
	RunWait, "C:\Program Files\VideoLAN\VLC\vlc.exe" -y -i "%arg1%" "%arg2%"
}else{
	RunWait, "C:\Program Files\VideoLAN\VLC\vlc.exe" -y -i "%arg1%" -vcodec %codec% "%arg2%"
}

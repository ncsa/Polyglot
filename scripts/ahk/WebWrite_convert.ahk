;Script to use Web Write tool to convert handwritten text. 
;Victoria Winner

Run http://webdemo.visionobjects.com/write.html?locale=default
sleep, 2000
Click 200, 400 down 
Loop, read, %1%
{
  X=-1
  Loop, parse, A_LoopReadLine, %A_Space%
  {
     if(X=-1){
       X= %A_LoopField%
       X+=200
     }else{
       Y= %A_LoopField%
       Y+=400
;      MsgBox, X is %X%, Y is %Y% 
       MouseMove %X%,%Y%
       X=-1
     }      
  }

}
Click up 
sleep, 1000
MouseClickDrag, L, 200,600, 900,700
Send, {CTRLDOWN}c{CTRLUP}
;{ALTDOWN}{TAB}{ALTUP}{CTRLDOWN}v{CTRLUP}{ENTER}{ALTDOWN}{TAB}{ALTUP}
FileAppend, %ClipBoard%, %2%
WinClose, Web Write

return
; Note: From now on whenever you run AutoHotkey directly, this script
; will be loaded.  So feel free to customize it to suit your needs.

; Please read the QUICK-START TUTORIAL near the top of the help file.
; It explains how to perform common automation tasks such as sending
; keystrokes and mouse clicks.  It also explains more about hotkeys.

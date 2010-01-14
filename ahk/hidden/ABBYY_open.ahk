;ABBYY FineReader (v9.0 Professional Edition)
;image
;jpg

;Run program if not already running
IfWinNotExist, ABBYY FineReader 9.0 Professional Edition
{
  Run, C:\Program Files\ABBYY FineReader 9.0\FineReader.exe
  WinWait, Untitled document - ABBYY FineReader 9.0 Professional Edition

  ;Activate the window
  WinActivate, Untitled document - ABBYY FineReader 9.0 Professional Edition
  WinWaitActive, Untitled document - ABBYY FineReader 9.0 Professional Edition

  ;Close the default document
  PostMessage, 0x111, 40444, 0,, Untitled document - ABBYY FineReader 9.0 Professional Edition
}

;Activate the window
WinActivate, ABBYY FineReader 9.0 Professional Edition
WinWaitActive, ABBYY FineReader 9.0 Professional Edition

;Open model
PostMessage, 0x111, 40215, 0,, ABBYY FineReader 9.0 Professional Edition
WinWait, Open Image
ControlSetText, Edit1, %1%
ControlSend, Edit1, {Enter}

;Wait for scan to complete
WinWait, Adding pages to the document...

Loop
{
  ;Click "Close" when scanning is finished
  ControlGetText, tmp, Button1, Adding pages to the document...
  
  if(tmp = "Close")
  {
    ControlClick, Button1, Adding pages to the document...
    break
  }

  ;Don't spin!
  Sleep, 500
}
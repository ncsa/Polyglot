;Microsoft PowerPoint (2010)
; save power point file
;pptx, pptm, ppt, pdf, xps, potx, potm, pot, thmx, ppsx, ppsm, pps, ppam, ppa, xml, wmv, gif, jpg,png, tif, bmp,wmf, emf, rtf, pptx, odp


;Parse output filename
arg1 = %1%
StringGetPos, index, arg1, \, R
ifLess, index, 0, ExitApp
index += 2
output_filename := SubStr(arg1, index)


;Parse output format
StringGetPos, index, arg1, ., R
ifLess, index, 0, ExitApp
index += 2
out := SubStr(arg1, index)


;allow partial match of the title
SetTitleMatchMode, 2
IfWinNotActive, Microsoft PowerPoint, MsoDockBottom, WinActivate,  Microsoft PowerPoint, MsoDockBottom
WinWaitActive,  Microsoft PowerPoint, MsoDockBottom

Send, !f
;Send, {ALT}+{f}
Send, a


;Save the model
WinWait, Save As
ControlSetText, Edit1, %1%

ControlClick, ComboBox2, Save As
Send {Up 27}

if(out = "pptx"){
}else if(out = "pptm"){
  Send, {p}
  }else if(out = "ppt"){
  Send, {p 2}
}else if(out = "pdf"){
  Send, {p 3}
  }else if(out = "xps"){
  Send, {x}
  }else if(out = "potx"){
  Send, {p 4}
  }else if(out = "potm"){
  Send, {p 5}
  }else if(out = "pot"){
  Send, {p 6}
  }else if(out = "thmx"){
  Send, {o}
  }else if(out = "ppsx"){
  Send, {p 7}
  }else if(out = "ppsm"){
  Send, {p 8}
  }else if(out = "pps"){
  Send, {p 9}
}else if(out = "ppam"){
  Send, {p 10}
}else if(out = "ppa"){
  Send, {p 11}
  }else if(out = "xml"){
  Send, {p 12}
  }else if(out = "wmv"){
  Send, {w}
  }else if(out = "gif"){
  Send, {g}
  }else if(out = "jpg"){
  Send, {j}
  }else if(out = "png"){
  Send, {p 13}
  }else if(out = "tif"){
  Send, {t}
  }else if(out = "bmp"){
  Send, {b}
  }else if(out = "wmf"){
  Send, {w 2}
  }else if(out = "emf"){
  Send, {e}
  }else if(out = "rtf"){
  Send, {o 2}
  }else if(out = "odp"){
  Send, {o}
}
; this is the order of selections: pptx, pptm, ppt, pdf, xps, potx, potm, pot, thmx, ppsx, ppsm, pps, ppam, ppa, xml, wmv, gif, jpg,png, tif, bmp,wmf, emf, rtf, pptx, odp

Send, {Enter}

;ControlClick, Button6, Save

ControlSend, Edit1, {Enter}



;Return to main window before exiting
Loop
{
  ;Continue on if main window is active
  IfWinActive, %output_filename% - Microsoft PowerPoint
  { 
    break
  }

  Sleep, 500
}

Send, !f
Send, x


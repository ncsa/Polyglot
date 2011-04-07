;Microsoft Office Power Point (2010)
;power point
;pptx, ppt, pptm, ppsx, pps, ppsm, potx, pot, xml, html, htm, mht,mhtml, thmx, txt, rtf, doc, docx, odt, wpd, wps, ppam, ppams, odp

; open power point file
;Parse input filename
arg1 = %1%
StringGetPos, index, arg1, \, R
ifLess, index, 0, ExitApp
index += 2
input_filename := SubStr(arg1, index)


;Run program if not already running
IfWinNotExist, Presentation1 - Microsoft PowerPoint
{
  Run, "C:\Program Files (x86)\Microsoft Office\Office14\POWERPNT.EXE" "%1%"
  ;WinWait, Presentation1 - Microsoft PowerPoint
}

;Make sure image is loaded before continuing
Loop
{
  IfWinExist, %input_filename% - Microsoft PowerPoint
  {
    break
  }

  Sleep, 500
}







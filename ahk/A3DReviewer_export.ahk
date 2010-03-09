;Adobe 3D Reviewer (v9)
;model
;igs, pdf, stl, stp, u3d, wrl, x_t

;Parse input format
arg1 = %1%
StringGetPos, index, arg1, ., R
ifLess, index, 0, ExitApp
index += 2
out := SubStr(arg1, index)

;Parse filename root
StringGetPos, index, arg1, \, R
ifLess, index, 0, ExitApp
index += 2
name := SubStr(arg1, index)
StringGetPos, index, name, ., R
ifLess, index, 0, ExitApp
name := SubStr(name, 1, index)

;Check for opened model before continuing
WinWait, Adobe 3D Reviewer - [%name%]

;Active the window (import if we want the active windows title)
WinActivate, Adobe 3D Reviewer
WinWaitActive, Adobe 3D Reviewer

;Export model
PostMessage, 0x111, 32804, 0,, Adobe 3D Reviewer - [%name%]
WinWait, Export

if(out = "igs"){
  ControlSend, ComboBox3, i
}else if(out = "pdf"){
  ControlSend, ComboBox3, i
  ControlSend, ComboBox3, p
  ControlSend, ComboBox3, p
}else if(out = "stl"){
  ControlSend, ComboBox3, i
  ControlSend, ComboBox3, s
  ControlSend, ComboBox3, s
}else if(out = "stp"){
  ControlSend, ComboBox3, i
  ControlSend, ComboBox3, s
}else if(out = "u3d"){
  ControlSend, ComboBox3, u
}else if(out = "wrl"){
  ControlSend, ComboBox3, V
}else if(out = "x_t"){
  ControlSend, ComboBox3, i
  ControlSend, ComboBox3, p
}

ControlSetText, Edit1, %1%
ControlSend, Edit1, {Enter}

;Return to main window before exiting
Loop
{
  ;Continue on if main window is active
  IfWinActive, Adobe 3D Reviewer - [%name%]
  { 
    break
  }

  ;Click "Yes" if asked to overwrite files
  IfWinExist, Confirm
  {
    ControlGetText, tmp, Button1, Confirm

    if(tmp = "&Yes")
    {
      ControlClick, Button1, Confirm
    }
  }

  Sleep, 500
}

;Wait for export to finish
StatusBarWait, For Help, press F1

;Wait a lit bit more just in case
Sleep, 1000

;Close whatever model is currently open
PostMessage, 0x111, 57602, 0,, Adobe 3D Reviewer

;Make sure it actually closed before exiting
Loop
{
  ;Exit if back at empty main window
  WinGetActiveTitle, title

  if(title = "Adobe 3D Reviewer")
  {
    ;Click "OK" if we still tried to close too quickly
    ControlGetText, tmp, Static2, Adobe 3D Reviewer
  
    if(tmp = "You cannot close Adobe 3D Reviewer while an export is in progress.")
    {
      ControlClick, Button1, Adobe 3D Reviewer
    }else{
      break
    }
  }

  Sleep, 500
}
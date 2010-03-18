;Acrobat 3D Reviewer

;Check for opened model before continuing
WinWait, Adobe 3D Reviewer - [%2%]

;Active the window (import if we want the active windows title)
WinActivate, Adobe 3D Reviewer
WinWaitActive, Adobe 3D Reviewer

;Export model
PostMessage, 0x111, 32804, 0,, Adobe 3D Reviewer - [%2%]
WinWait, Export
ControlSend, ComboBox3, u
ControlSetText, Edit1, %1%
ControlSend, Edit1, {Enter}

;Return to main window before exiting
WinWaitActive, Adobe 3D Reviewer - [%2%]

;Wait for export to finish
StatusBarWait, For Help, press F1

;Wait a lit bit more just in case
Sleep 200

;Close whatever model is currently open
PostMessage, 0x111, 57602, 0,, Adobe 3D Reviewer

;Make sure it actually closed before exiting
Loop
{
  WinGetActiveTitle, title
  ;MsgBox, Title: "%title%".

  if(title = "Adobe 3D Reviewer")
  {
    break
  }
}
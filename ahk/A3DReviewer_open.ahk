;Adobe 3D Reviewer (v9)
;3d
;3ds, 3dxml, arc, asm, bdl, catdrawing, catpart, catproduct, catshape, cgr, dae, dlv, exp, hgl, hp, hpgl, hpl, iam, ifc, igs, iges, ipt, jt, kmz, mf1, model, neu, obj, _pd, par, pdf, pkg, plt, prc, prt, prw, psm, pwd, sab, sat, sda, sdac, sdp, sdpc, sds, sdsc, sdw, sdwc, ses, session, sldasm, sldlfp, sldprt, stl, step, stp, u3d, unv, wrl, vrml, x_b, x_t, xas, xpr, xmt, xmt_txt, xv0, xv3

;Run program if not already running
IfWinNotExist, Adobe 3D Reviewer
{
  Run, C:\Program Files\Adobe\Acrobat 9.0\Acrobat\plug_ins3d\prc\A3DReviewer.exe
  WinWait, Adobe 3D Reviewer
}

;Activate the window
WinActivate, Adobe 3D Reviewer
WinWaitActive, Adobe 3D Reviewer

;Parse filename root
arg1 = %1%
StringGetPos, index, arg1, \, R
ifLess, index, 0, ExitApp
index += 2
name := SubStr(arg1, index)
StringGetPos, index, name, ., R
ifLess, index, 0, ExitApp
name := SubStr(name, 1, index)

;Open model
PostMessage, 0x111, 57601, 0,, Adobe 3D Reviewer
WinWait, Open
ControlSetText, Edit1, %1%
ControlSend, Edit1, {Enter}

;Make sure model is loaded before exiting
Loop
{
  IfWinExist, Adobe 3D Reviewer - [%name%]
  {
    break
  }

  ;Click "OK" if this was an unknown format
  ControlGetText, tmp, Static2, Adobe 3D Reviewer

  if(tmp = "Unknown Format")
  {
    ControlClick, Button1, Adobe 3D Reviewer
  }

  Sleep, 500
}
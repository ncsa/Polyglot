;ParaView (v3.4.0)
;3d
;cube, dem, facet, g, ply, pvd, raw, stl, vtk, xyz

;Run program if not already running
IfWinNotExist, Kitware ParaView 3.4.0
{
  Run, C:\Program Files\ParaView 3.4.0\bin\paraview.exe
  WinWait, Kitware ParaView 3.4.0
}

;Activate the window
WinActivate, Kitware ParaView 3.4.0
WinWaitActive, Kitware ParaView 3.4.0

;Open model
Send, ^o
WinWait, Open File:
SetKeyDelay, 0
Send, %1%
Send, {Enter}
WinWaitActive, Kitware ParaView 3.4.0
Send, !a
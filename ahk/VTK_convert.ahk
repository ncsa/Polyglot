;VTK (v5.2.1)
;model
;3ds, obj, pdb, ply, stl
;iv, ply, rib, stl, wrl

;Get arguments and correct slashes in path to unix style
arg1 = %1%
StringReplace arg1, arg1, \, /, All
arg2 = %2%
StringReplace arg2, arg2, \, /, All
tmp_path = %3%

;Parse input format
StringGetPos, index, arg1, ., R
ifLess, index, 0, ExitApp
index += 2
in := SubStr(arg1, index)

;Parse filename root
StringGetPos, index, arg1, /, R
ifLess, index, 0, ExitApp
folder := SubStr(arg1, 1, index)
index += 2
name := SubStr(arg1, index)
StringGetPos, index, name, ., R
ifLess, index, 0, ExitApp
name := SubStr(name, 1, index)
name = %folder%/%name%

;Parse output format
StringGetPos, index, arg2, ., R
ifLess, index, 0, ExitApp
index += 2
out := SubStr(arg2, index)

;Set VTK plugins
plugin_i = vtkPolyDataReader
plugin_o = vtkPolyDataWriter
plugin_o_setInput = SetInputConnection [input GetOutputPort]
plugin_o_setFile = SetFileName "%arg2%"

if(in = "3ds"){
  plugin_i = vtk3DSImporter
}else if(in = "obj"){
  plugin_i = vtkOBJReader
}else if(in = "pdb"){
  plugin_i = vtkPDBReader
}else if(in = "ply"){
  plugin_i = vtkPLYReader
}else if(in = "stl"){
  plugin_i = vtkSTLReader
}

if(out = "iv"){
  plugin_o = vtkIVWriter
}else if(out = "obj"){	;Buggy, won't export to non-current directory!
  plugin_o = vtkOBJExporter
  plugin_o_setInput = SetInput renWin
  plugin_o_setFile = SetFilePrefix "%name%"
}else if(out = "ply"){
  plugin_o = vtkPLYWriter
}else if(out = "rib"){
  plugin_o = vtkRIBExporter
  plugin_o_setInput = SetInput renWin
  plugin_o_setFile = SetFilePrefix "%name%"
}else if(out = "stl"){
  plugin_o = vtkSTLWriter
}else if(out = "wrl"){
  plugin_o = vtkVRMLExporter
  plugin_o_setInput = SetInput renWin
}

;Write Tcl/TK script
FileDelete, %3%_VTK_convert.tcl
FileAppend,
(
package require vtk
package require vtkinteraction

#Load model
%plugin_i% input
input SetFileName "%arg1%"

#Create viewer, needed by exporters
vtkPolyDataMapper map;
map SetInput [input GetOutput]
vtkActor objectActor
objectActor SetMapper map
[objectActor GetProperty] SetColor 0.5 0.5 0.5;
vtkRenderWindow renWin
vtkRenderer ren1
renWin AddRenderer ren1
vtkRenderWindowInteractor iren
iren SetRenderWindow renWin
ren1 AddActor objectActor
ren1 SetBackground 1 1 1;
renWin Render
wm withdraw .

#Save model
%plugin_o% output
output %plugin_o_setInput%
output %plugin_o_setFile%
output Write

exit
), %tmp_path%_VTK_convert.tcl

;Run program
RunWait, "C:\Program Files\VTK 5.2\bin\vtk.exe" "%tmp_path%_VTK_convert.tcl"

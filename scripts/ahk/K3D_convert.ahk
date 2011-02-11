;K-3D (v0.6.7)
;model
;k3d, obj
;k3d, obj

;Get arguments and correct slashes in path for k3d
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

;Parse output format
StringGetPos, index, arg2, ., R
ifLess, index, 0, ExitApp
index += 2
out := SubStr(arg2, index)

;Set K3D plugins
plugin_i = K3DMeshInput
plugin_o = K3DMeshOutput

if(in = "obj"){
  plugin_i = OBJMeshInput
}

if(out = "obj"){
  plugin_o = OBJMeshOutput
}

;Write Python script
FileDelete, %3%_K3D_convert.py
FileAppend,
(
#python

import k3d

doc = k3d.application.new_document();
source = doc.new_node("%plugin_i%");
source.file = "%arg1%";
target = doc.new_node("%plugin_o%");
target.file = "%arg2%";

doc.set_dependency(target.get_property("input_mesh"), source.get_property("output_mesh"));
), %tmp_path%_K3D_convert.py

;Run program
RunWait, "C:\k3d\k3d.exe" --script "%tmp_path%_K3D_convert.py" --ui none --exit

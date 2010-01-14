;Cyberware PlyTool (v1.7)
;3d
;stl
;obj

;Get arguments and correct slashes in path for k3d
arg1 = %1%
StringReplace arg1, arg1, \, /, All
arg2 = %2%
StringReplace arg2, arg2, \, /, All
tmp_path = %3%
StringReplace tmp_path, tmp_path, \, /, All

;Run program
RunWait, "C:\Program Files\headus PlyTool\stl2ply.exe" "%arg1%" "%tmp_path%_tmp.ply"
RunWait, "C:\Program Files\headus PlyTool\ply2obj.exe" "%tmp_path%_tmp.ply" "%arg2%"

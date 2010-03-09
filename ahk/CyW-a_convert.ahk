;Cyberware PlyTool (v1.7)
;model
;ply
;3ds, asc, dxf, iges, iv, obj, rag, stl, wrl

;Get arguments and correct slashes in path for k3d
arg1 = %1%
StringReplace arg1, arg1, \, /, All
arg2 = %2%
StringReplace arg2, arg2, \, /, All

;Parse output format
StringGetPos, index, arg2, ., R
ifLess, index, 0, ExitApp
index += 2
out := SubStr(arg2, index)

;Run program
if(out = "3ds"){
  RunWait, "C:\Program Files\headus PlyTool\ply23ds.exe" "%arg1%" "%arg2%"
}else if(out = "asc"){
  RunWait, "C:\Program Files\headus PlyTool\ply2asc.exe" "%arg1%" "%arg2%"
}else if(out = "dxf"){
  RunWait, "C:\Program Files\headus PlyTool\ply2dxf.exe" "%arg1%" "%arg2%"
}else if(out = "iges"){
  RunWait, "C:\Program Files\headus PlyTool\ply2iges.exe" "%arg1%" "%arg2%"
}else if(out = "iv"){
  RunWait, "C:\Program Files\headus PlyTool\ply2iv.exe" "%arg1%" "%arg2%"
}else if(out = "obj"){
  RunWait, "C:\Program Files\headus PlyTool\ply2obj.exe" "%arg1%" "%arg2%"
}else if(out = "rag"){
  RunWait, "C:\Program Files\headus PlyTool\ply2rag.exe" "%arg1%" "%arg2%"
}else if(out = "stl"){
  RunWait, "C:\Program Files\headus PlyTool\ply2stl.exe" "%arg1%" "%arg2%"
}else if(out = "wrl"){
  RunWait, "C:\Program Files\headus PlyTool\ply2wrl.exe" "%arg1%" "%arg2%"
}

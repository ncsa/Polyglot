;Cyberware PlyTool (v1.7)
;model
;obj
;ply

;Get arguments and correct slashes in path for k3d
arg1 = %1%
StringReplace arg1, arg1, \, /, All
arg2 = %2%
StringReplace arg2, arg2, \, /, All

;Run program
RunWait, "C:\Program Files\headus PlyTool\obj2ply.exe" "%arg1%" "%arg2%"

;NIST VRML<->X3D (v1.0)
;3d
;wrl, x3d
;wrl, x3d

;Get arguments and correct slashes in path for k3d
arg1 = %1%
StringReplace arg1, arg1, \, /, All
arg2 = %2%
StringReplace arg2, arg2, \, /, All

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

;Run program
if(in = "wrl" and out = "x3d"){
  RunWait, java -cp C:/Converters/Vrml97ToX3dNist_v1.0/Vrml97ToX3dNist.jar iicm.vrml.vrml2x3d.vrml2x3d "%arg1%" "%arg2%"
}else if(in = "x3d" and out = "wrl"){
  RunWait, java -cp C:/Converters/x3dv2/x3dv2.jar org.apache.xalan.xslt.Process -IN "%arg1%" -XSL C:\Converters\x3dv2\test\X3dToVrml97.xsl -OUT "%arg2%"
}
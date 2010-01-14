;Blender (v2.46)
;3d
;blend, stl, wrl
;blend, dxf, stl, wrl

;Get arguments and correct slashes in path for k3d
arg1 = %1%
StringReplace arg1, arg1, \, /, All
arg2 = %2%
StringReplace arg2, arg2, \, /, All
tmp_path = %3%
StringReplace tmp_path, tmp_path, \, /, All

;Write Python script
FileDelete, %3%_Blender_convert.py
FileAppend,
(
import Blender
Blender.Load('%arg1%');

import Blender
Blender.Save('%arg2%', 1);
Blender.Quit();
), %tmp_path%_Blender_convert.py

;Run program
RunWait, "C:\Program Files\Blender Foundation\Blender\blender.exe" -P "%tmp_path%_Blender_convert.py"

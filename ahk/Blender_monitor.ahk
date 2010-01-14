;Blender (v2.46)

Loop
{
  ;Check if parent process is still running
  Process, Exist, javaw.exe
  JAVAW_EXISTS = %ErrorLevel%
  Process, Exist, java.exe
  JAVA_EXISTS = %ErrorLevel%

  if(JAVAW_EXISTS = 0 and JAVA_EXISTS = 0)
  {
    break
  }

  ;Move mouse away from popup to close it (in case an error occurs)
  ifWinExist, Blender
  {
    MouseMove, 400, 400, 50

    ifWinExist, Blender
    {
      MouseMove, 800, 800, 50
    }
  }

  ;Don't spin!
  Sleep, 500
}
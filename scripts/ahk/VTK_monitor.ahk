;VTK (v5.2.1)

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

  ;Click "OK" if conversion fails.
  ifWinExist, Error in startup script
  {
    ControlClick, Button1, Error in startup script
  }

  ;Don't spin!
  Sleep, 500
}
;Adobe 3D Reviewer (v9)

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

  ;Click "Don't Send" if application crashed.
  ifWinExist, Adobe® 3D Reviewer Application
  {
    ControlGetText, tmp, Button1, Adobe® 3D Reviewer Application
  
    if(tmp = "&Don't Send")
    {
      ControlClick, Button1, Adobe® 3D Reviewer Application
    }
  }

  ;Don't spin!
  Sleep, 500
}
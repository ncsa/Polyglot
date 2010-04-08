;IrfanView (v4.25)

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
  ifWinExist, IrfanView
  {
    ControlGetText, tmp, Button1, IrfanView
  
    if(tmp = "OK")
    {
      ControlClick, Button1, IrfanView
    }
  }

  ;Don't spin!
  Sleep, 500
}
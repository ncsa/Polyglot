;Microsoft Office Word (2007)

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

  ;Click "Yes" if warned about saving to a macro-free format.
  ifWinExist, Microsoft Office Word
  {
    ControlGetText, tmp, Button1, Microsoft Office Word
  
    if(tmp = "&Yes")
    {
      ControlClick, Button1, Microsoft Office Word
    }
  }

  ;Don't spin!
  Sleep, 500
}

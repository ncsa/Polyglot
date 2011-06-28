Google SketchUp (v7.0)

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
  ifWinExist, SketchUp Error Report
  {
    ControlGetText, tmp, Button2, SketchUp Error Report

    if(tmp = "&Don't Send")
    {
      ControlClick, Button2, SketchUp Error Report
    }
  }

  ;Don't spin!
  Sleep, 500
}

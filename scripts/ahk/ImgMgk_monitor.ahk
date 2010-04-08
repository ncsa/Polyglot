;ImageMagick (v6.5.2)

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
  ifWinExist, ImageMagick Studio library and utility programs
  {
    ControlGetText, tmp, Button1, ImageMagick Studio library and utility programs
  
    if(tmp = "&Close program")
    {
      ControlClick, Button1, ImageMagick Studio library and utility programs
    }

    ControlGetText, tmp, Button2, ImageMagick Studio library and utility programs
  
    if(tmp = "Cancel")
    {
      ControlClick, Button2, ImageMagick Studio library and utility programs
    }
  }

  ;Don't spin!
  Sleep, 500
}
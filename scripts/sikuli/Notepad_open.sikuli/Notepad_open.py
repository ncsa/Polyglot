#Notepad
#document
#txt
import sys

setAutoWaitTimeout(10000)
openApp("C:/windows/system32/notepad.exe")

click("File.png")
click("CtrOOpen.png")
wait("1296061487207.png")
type(sys.argv[0] + Key.ENTER);
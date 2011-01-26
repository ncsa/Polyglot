#Notepad
#document
#txt
import sys

setAutoWaitTimeout(10000)

click("File.png")
click("SaveAs.png")
wait("SaveAs-1.png")
type(sys.argv[0] + Key.ENTER)

closeApp("notepad")

#Wordpad
#document
#doc, rtf, txt, wri
#rtf, txt
import sys
import os.path

out = os.path.splitext(sys.argv[1])[1][1:]

setAutoWaitTimeout(10000)
openApp("C:/Program Files/Windows NT/Accessories/wordpad.exe")

#Open
click("File-1.png")
click("Open-2.png")
wait("1295906488629.png")
type(sys.argv[0] + Key.ENTER)
wait("urHeDDressF1.png")

#Save
click("File-2.png")
click("SaveAs-2.png")
wait("SaveAs.png")
type(sys.argv[1])
type(Key.TAB)
type(Key.DOWN + Key.UP + Key.UP + Key.UP + Key.UP)

if out == "txt":
	type(Key.DOWN)

type(Key.ENTER + Key.ENTER)
wait("DrHelDDressF.png")
click("X.png")
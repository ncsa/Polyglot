#Adobe 3D Reviewer (v9)
#model
#igs, pdf, stl, stp, u3d, wrl, x_t
import sys
import os.path

out = os.path.splitext(sys.argv[0])[1][1:]

setAutoWaitTimeout(10000)
switchApp("adobe 3d reviewer")

click("File.png")
click("Qxpurt.png")
wait("1296063699598.png")
type(sys.argv[0])
type(Key.TAB)
type(Key.DOWN + Key.UP + Key.UP + Key.UP + Key.UP + Key.UP + Key.UP + Key.UP)

if out == "igs":
	type(Key.DOWN)
elif out == "x_t":
	type(Key.DOWN + Key.DOWN)
elif out == "stp":
	type(Key.DOWN + Key.DOWN + Key.DOWN)
elif out == "stl":
	type(Key.DOWN + Key.DOWN + Key.DOWN + Key.DOWN)
elif out == "u3d":
	type(Key.DOWN + Key.DOWN + Key.DOWN + Key.DOWN + Key.DOWN)
elif out == "wrl":
	type(Key.DOWN + Key.DOWN + Key.DOWN + Key.DOWN + Key.DOWN + Key.DOWN)

type(Key.ENTER + Key.ENTER)

wait("ZnrHelnnress.png")
click("File-1.png")
click("Cluse.png")
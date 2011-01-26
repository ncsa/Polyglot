#Adobe 3D Reviewer (v9)
#model
#3ds, 3dxml, arc, asm, bdl, catdrawing, catpart, catproduct, catshape, cgr, dae, dlv, exp, hgl, hp, hpgl, hpl, iam, ifc, igs, iges, ipt, jt, kmz, mf1, model, neu, obj, _pd, par, pdf, pkg, plt, prc, prt, prw, psm, pwd, sab, sat, sda, sdac, sdp, sdpc, sds, sdsc, sdw, sdwc, ses, session, sldasm, sldlfp, sldprt, stl, step, stp, u3d, unv, wrl, vrml, x_b, x_t, xas, xpr, xmt, xmt_txt, xv0, xv3
import sys

setAutoWaitTimeout(10000)
openApp("C:\Program Files\Adobe\Acrobat 9.0\Acrobat\plug_ins3d\prc\A3DReviewer.exe")
switchApp("adobe 3d reviewer")

click("File.png")
click("Qcn.png")
wait("1296062416239.png")
type(sys.argv[0] + Key.ENTER)
wait("0rHelmuressF.png")
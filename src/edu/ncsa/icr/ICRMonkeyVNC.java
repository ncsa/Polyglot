package edu.ncsa.icr;
import com.tightvnc.vncviewer.*;

public class ICRMonkeyVNC extends VncViewer
{
	public static void main(String[] args)
	{
		args = new String[]{"HOST", "starbuck.ncsa.uiuc.edu"};
		
	  ICRMonkeyVNC v = new ICRMonkeyVNC();
	  v.mainArgs = args;
	  v.inAnApplet = false;
	  v.inSeparateFrame = true;
	
	  v.init();
	  v.start();
	}
}
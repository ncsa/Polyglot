package edu.ncsa.icr;
import com.tightvnc.vncviewer.*;

public class ICRMonkey extends VncViewer
{
	public static void main(String[] args)
	{
		args = new String[]{"HOST", "starbuck.ncsa.uiuc.edu"};
		
	  ICRMonkey v = new ICRMonkey();
	  v.mainArgs = args;
	  v.inAnApplet = false;
	  v.inSeparateFrame = true;
	
	  v.init();
	  v.start();
	}
}
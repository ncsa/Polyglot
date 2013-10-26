package edu.illinois.ncsa.isda.icr.polyglot;
import javax.swing.*;

/**
 * An applet to hold a PolyglotMonitor.
 * @author Kenton McHenry
 */
public class PolyglotMonitorApplet extends JApplet
{
	private PolyglotMonitor monitor;
	
  /**
   * Initialize the applet.
   */
  public void init()
  {
  	String server = getParameter("server");
  	String host;
  	int port;
  	int tmpi;
  	
  	if(true){		//Debug
  		server = "polyglot.ncsa.illinois.edu:50002";
  		this.setSize(900, 650);
  	}
  	
  	if(server != null){
  		tmpi = server.lastIndexOf(":");	
			host = server.substring(0, tmpi);
			port = Integer.valueOf(server.substring(tmpi+1)); 
			monitor = new PolyglotMonitor(host, port);
  	}else{
  		monitor = new PolyglotMonitor("localhost", 50002);
  	}
  	
  	add(monitor);
  }
}
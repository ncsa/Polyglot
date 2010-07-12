package edu.ncsa.icr.polyglot;
import javax.swing.*;

/**
 * An applet to simply hold an IOGraphPanel.
 * @author Kenton McHenry
 */
public class IOGraphApplet extends JApplet
{
	private IOGraphPanel<String,String> iograph_panel;
	
  /**
   * Initialize the applet.
   */
  public void init()
  {
  	Polyglot polyglot = null;
  	String url = getParameter("url");
  	String side_pane_width = getParameter("side_pane_width");
  	String ouput_panel_height = getParameter("output_panel_height");
  	String server;
  	int port;
  	int tmpi;
  	
  	if(url != null){
  		tmpi = url.lastIndexOf(":");
  		
  		if(tmpi != -1){
  			server = url.substring(0, tmpi);
  			port = Integer.valueOf(url.substring(tmpi+1)); 
  			polyglot = new PolyglotClient(server, port);
  		}
  	}else{
  		polyglot = new PolyglotClient("localhost", 31);
  	}
  	
  	if(polyglot != null){
	  	iograph_panel = new IOGraphPanel<String,String>(polyglot.getInputOutputGraph(), getWidth(), getHeight(), 2);
	  	polyglot.close();
	  	if(side_pane_width != null) iograph_panel.setSidePaneWidth(Integer.valueOf(side_pane_width));
		  if(ouput_panel_height != null) iograph_panel.setOutputPanelHeight(Integer.valueOf(ouput_panel_height));
		  add(iograph_panel.getAuxiliaryInterfacePane());
  	}
  }
}
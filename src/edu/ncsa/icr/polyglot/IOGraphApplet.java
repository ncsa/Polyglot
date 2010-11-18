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
  	IOGraph iograph = null;
  	String url = getParameter("url");
  	String side_pane_width = getParameter("side_pane_width");
  	String ouput_panel_height = getParameter("output_panel_height");
  	String vertex_color = getParameter("vertex_color");
  	String edge_color = getParameter("edge_color");
  	String server;
  	int port;
  	int tmpi;
  	
  	if(url != null){
  		if(url.contains(":") && !url.startsWith("http:")){
	  		tmpi = url.lastIndexOf(":");	
  			server = url.substring(0, tmpi);
  			port = Integer.valueOf(url.substring(tmpi+1)); 
  			polyglot = new PolyglotClient(server, port);
  			iograph = polyglot.getInputOutputGraph();
  			polyglot.close();
  		}else{
  			if(url.startsWith("http:")){
  				iograph = new IOGraph(url);
  			}else{
  				iograph = new IOGraph(getCodeBase() + url);
  			}
  		}
  	}else{
  		polyglot = new PolyglotClient("localhost", 50002);
			iograph = polyglot.getInputOutputGraph();
			polyglot.close();
  	}
  	
  	if(iograph != null){
	  	iograph_panel = new IOGraphPanel<String,String>(iograph, getWidth(), getHeight(), 2);
	  	if(side_pane_width != null) iograph_panel.setSidePaneWidth(Integer.valueOf(side_pane_width));
		  if(ouput_panel_height != null) iograph_panel.setOutputPanelHeight(Integer.valueOf(ouput_panel_height));
		  if(vertex_color != null) iograph_panel.setVertexColor(Integer.valueOf(vertex_color, 16).intValue());
		  if(edge_color != null) iograph_panel.setEdgeColor(Integer.valueOf(edge_color, 16).intValue());
		  add(iograph_panel.getAuxiliaryInterfacePane());
  	}
  }
}
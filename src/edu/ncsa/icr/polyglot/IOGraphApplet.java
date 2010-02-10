package edu.ncsa.icr.polyglot;
import javax.swing.*;

/**
 * An applet to simply hold an IOGraphPanel.
 * @author Kenton McHenry
 */
public class IOGraphApplet extends JApplet
{
  /**
   * Initialize the applet.
   */
  public void init()
  {
  	String url = getParameter("url");
  	String user = getParameter("user");
  	String password = getParameter("password");
  	String graph_width = getParameter("graph_width");
  	String graph_height = getParameter("graph_height");
  	String graph_rings = getParameter("graph_rings");
  	
  	if(url == null) url = "jdbc:mysql://isda.ncsa.uiuc.edu/csr";
  	if(user == null) user = "demo";
  	if(password == null) password = "demo";
  	if(graph_width == null) graph_width = "600";
  	if(graph_height == null) graph_height = "600";
  	if(graph_rings == null) graph_rings = "2";
    
    IOGraph iograph = new IOGraph(url, user, password);
    IOGraphPanel iograph_panel = new IOGraphPanel(iograph, Integer.valueOf(graph_width), Integer.valueOf(graph_height), Integer.valueOf(graph_rings));
    setSize(Integer.valueOf(graph_width) + 250, Integer.valueOf(graph_height) + 100);
    add(iograph_panel.getAuxiliaryInterfacePane());
  }
}
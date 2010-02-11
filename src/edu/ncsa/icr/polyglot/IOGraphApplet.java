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
  	String side_pane_width = getParameter("side_pane_width");
  	String ouput_panel_height = getParameter("output_panel_height");
  	
  	if(url != null){
	    IOGraph iograph = new IOGraph(url);
	    IOGraphPanel iograph_panel = new IOGraphPanel(iograph, getWidth(), getHeight(), 2);
	    if(side_pane_width != null) iograph_panel.setSidePaneWidth(Integer.valueOf(side_pane_width));
	    if(ouput_panel_height != null) iograph_panel.setOutputPanelHeight(Integer.valueOf(ouput_panel_height));
	    add(iograph_panel.getAuxiliaryInterfacePane());
  	}
  }
}
package edu.ncsa.icr.polyglot;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import javax.swing.*;

/**
 * An applet to simply hold an IOGraphPanel.
 * @author Kenton McHenry
 */
public class IOGraphApplet extends JApplet
{
	private IOGraphPanel<String,String> iograph_panel;
	private IOGraph iograph = null;
  private URL url;
  
  /**
   * Initialize the applet.
   */
  public void init()
  {
  	Polyglot polyglot = null;
  	url = getDocumentBase();  	
  	String conversions = getParameter("convversions");
  	String measures = getParameter("measures");
  	String side_pane_width = getParameter("side_pane_width");
  	String output_panel_height = getParameter("output_panel_height");
  	String vertex_color = getParameter("vertex_color");
  	String edge_color = getParameter("edge_color");
  	
  	// parse the url
  	if (getParameter("url") != null) {
  		try {
  			url = new URL(getParameter("url"));
  		} catch (MalformedURLException e) {
  			e.printStackTrace();
  		}
  	}
  	
  	// load conversions
  	if(conversions == null){
  		conversions = "get_conversions.php";
  	}
  	try {
	  	URL c_url = new URL(url, conversions);
	  	if (c_url.getProtocol().equals("polyglot")) {
  			polyglot = new PolyglotClient(c_url.getFile(), c_url.getPort());
  			iograph = polyglot.getInputOutputGraph();
  			polyglot.close();
	  	} else {
	  		iograph = new IOGraph(c_url.toString());
	  	}
  	} catch (MalformedURLException e) {
  		e.printStackTrace();
  	}
  	if (iograph == null) {
  		return;
  	}
  	
		iograph_panel = new IOGraphPanel<String,String>(iograph, getWidth(), getHeight(), 2);
  	if(side_pane_width != null) iograph_panel.setSidePaneWidth(Integer.valueOf(side_pane_width));
	  if(output_panel_height != null) iograph_panel.setOutputPanelHeight(Integer.valueOf(output_panel_height));
	  if(vertex_color != null) iograph_panel.setVertexColor(Integer.valueOf(vertex_color, 16).intValue());
	  if(edge_color != null) iograph_panel.setEdgeColor(Integer.valueOf(edge_color, 16).intValue());
	  add(iograph_panel.getAuxiliaryInterfacePane());
  	
	  // load measures
	  if (measures == null) {
	  	measures = "get_measures.php";
	  }
	  try {
	  	URL m_url = new URL(url, measures);
	  	BufferedReader br = new BufferedReader(new InputStreamReader(m_url.openStream()));
	  	String line;

	  	JMenu menu = new JMenu("Weights");
		  iograph_panel.addPopupMenu(menu); 

		  ButtonGroup group = new ButtonGroup();
	    JRadioButtonMenuItem mi = new JRadioButtonMenuItem(new AbstractAction("No weights") {					
				public void actionPerformed(ActionEvent e) {
					iograph_panel.setEnableWeightedPaths(false);
					iograph_panel.setViewEdgeQuality(false);
					iograph_panel.computePath();
				}
			});
	    mi.setSelected(true);
	    menu.add(mi);
	    group.add(mi);

	  	while((line = br.readLine()) != null) {
	  		String[] pieces = line.split("\t");
	  		if (pieces.length != 5) {
	  			continue;
	  		}
	  		final String menuitem = pieces[1];
		    mi = new JRadioButtonMenuItem(new AbstractAction(menuitem) {					
					public void actionPerformed(ActionEvent e) {
						iograph_panel.setEnableWeightedPaths(true);
						iograph_panel.setViewEdgeQuality(true);
						iograph.setEdgeWeight(-1.0);
						iograph.setMinimumWeight(0.0);
						
						try {
					  	URL m_url = new URL(url, "get_weights_average.php?measure=" + URLEncoder.encode(menuitem, "UTF8"));
					  	BufferedReader br = new BufferedReader(new InputStreamReader(m_url.openStream()));
					  	String line;
					  	while((line = br.readLine()) != null) {
					  		if (line.toLowerCase().endsWith("<br>")) {
					  			line = line.substring(0, line.length() - 4);
					  		}
					  		String[] parts = line.split("\t");
					  		if (parts.length != 4) {
					  			System.err.println("Bad line : " + line);
					  		} else {
					  			try {
						  			Double w = Math.abs(Double.parseDouble(parts[3]));
						  			iograph.setEdgeWeight(parts[1], parts[2], parts[0], w);
					  			} catch (NumberFormatException exc) {
						  			System.err.println("Bad line : " + line);
					  				exc.printStackTrace();
					  			}
					  		}
					  	}
					  	br.close();
							iograph_panel.computePath();
						} catch (Exception exc) {
							exc.printStackTrace();
						}
					}
				});
		    mi.setSelected(false);
		    menu.add(mi);
		    group.add(mi);
	  	}
	  	br.close();
	  } catch (Exception e) {
	  	e.printStackTrace();
  	}
  }
}
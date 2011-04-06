package edu.ncsa.icr.polyglot;
import java.awt.*;
import javax.swing.*;
import java.util.*;

/**
 * An IOGraph Panel which accepts only vertices and edges of type String which have information specific
 * to software that is distributed among multiple machines.
 * @author Kenton McHenry
 */
public class DistributedSoftwareIOGraphPanel extends IOGraphPanel<String,String>
{	
  /**
   * Class constructor.
   * @param iograph the I/O-Graph
   * @param rings the number of rings used when displaying the graph
   */
  public DistributedSoftwareIOGraphPanel(IOGraph<String,String> iograph, int rings)
  {  	
  	super(iograph, rings, "software");
  	colorGraph();
  }
  
  /**
   * Color the graph according to software servers.
   */
  public void colorGraph()
  {  	
  	//Build color map
  	TreeMap<String,Color> color_map = new TreeMap<String,Color>();
  	TreeSet<String> servers = new TreeSet<String>();
  	Random random = new Random();
  	String server;
  	int rgb, r, g, b, tmpi;
  	
  	for(int i=0; i<edge_strings.size(); i++){
  		for(int j=0; j<edge_strings.get(i).size(); j++){
  			server = edge_strings.get(i).get(j);
  			tmpi = server.indexOf('[');
  			if(tmpi != -1) server = server.substring(tmpi+1, server.length()-1);
  			servers.add(server);
  		}
  	}
  	
  	for(Iterator<String> itr = servers.iterator(); itr.hasNext();){
      r = random.nextInt() % 55 + 200;
      g = random.nextInt() % 55 + 200;
      b = random.nextInt() % 55 + 200;
      rgb = (r<<16) | (g<<8) | b;
      
  		color_map.put(itr.next(), new Color(rgb));
  	}
  	
  	//Assign colors
  	edge_colors = new Vector<Vector<Color>>();
  	
  	for(int i=0; i<edge_strings.size(); i++){
  		edge_colors.add(new Vector<Color>());
  		
  		for(int j=0; j<edge_strings.get(i).size(); j++){
  			server = edge_strings.get(i).get(j);
  			tmpi = server.indexOf('[');
  			server = server.substring(tmpi+1, server.length()-1);
  			edge_colors.get(i).add(color_map.get(server));
  		}
  	}
  }
  
	/**
	 * Add this panel to a frame.
	 * @return the created frame
	 */
	public JFrame createFrame()
	{
    JFrame frame = new JFrame("IOGraph Viewer");
    frame.add(getAuxiliaryInterfacePane());
    frame.pack();
    frame.setVisible(true);
    
    return frame;
	}
	
	/**
   * The main starting point for this program.
   * @param args command line arguments
   */
  public static void main(String args[])
  {
  	DistributedSoftwareIOGraphPanel iograph_panel = null;
		String server = "localhost";
		int port = 50002;
		int tmpi;  
		
  	if(args.length > 0){
  		tmpi = args[0].lastIndexOf(':');
  		
  		if(tmpi != -1){
  			server = args[0].substring(0, tmpi);
  			port = Integer.valueOf(args[0].substring(tmpi+1));
  		}
  	}
  		
		PolyglotClient polyglot = new PolyglotClient(server, port);
		iograph_panel = new DistributedSoftwareIOGraphPanel(polyglot.getDistributedInputOutputGraph(), 2);
		polyglot.close();
		
		JFrame frame = iograph_panel.createFrame();
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
  }
}
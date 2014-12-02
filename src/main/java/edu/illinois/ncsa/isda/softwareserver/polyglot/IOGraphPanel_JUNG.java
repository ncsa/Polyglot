package edu.illinois.ncsa.isda.softwareserver.polyglot;
import edu.illinois.ncsa.isda.softwareserver.*;
import edu.illinois.ncsa.isda.softwareserver.SoftwareServerAuxiliary.*;
import java.awt.*;
import java.awt.geom.*;
import javax.swing.*;
import java.util.*;
import edu.uci.ics.jung.graph.*;
import edu.uci.ics.jung.algorithms.layout.*;
import edu.uci.ics.jung.visualization.*;
import edu.uci.ics.jung.visualization.control.*;
import edu.uci.ics.jung.visualization.decorators.*;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;
import edu.uci.ics.jung.visualization.subLayout.*;
import edu.uci.ics.jung.visualization.renderers.Renderer;
import edu.uci.ics.jung.visualization.picking.*;
import org.apache.commons.collections15.*;
import org.apache.commons.collections15.functors.*;

/*
//3D
import edu.uci.ics.jung.algorithms.layout3d.Layout;
import edu.uci.ics.jung.algorithms.layout3d.SpringLayout;
import edu.uci.ics.jung.visualization3d.VisualizationViewer;
*/

public class IOGraphPanel_JUNG<V extends Comparable, E extends Comparable> extends JPanel
{
	private IOGraph<V,E> iograph;
	private Graph<String,UniqueString> graph;
	
	/**
	 * A class that simply wraps a string so that as a comparable it is never equal to another string.
	 */
	public static class UniqueString implements Comparable
	{
		private String string;
		
		/**
		 * Class constructor.
		 * @param string the string to set this to
		 */
		public UniqueString(String string)
		{
			this.string = string;
		}
		
		/**
		 * Set the string.
		 * @param string the string to set this to
		 */
		public void set(String string)
		{
			this.string = string;
		}
		
		/**
		 * Get the string.
		 * @return the associated string
		 */
		public String get()
		{
			return string;
		}
		
		/**
		 * Return the string representation of this instance.
		 * @return the associated string
		 */
		public String toString()
		{
			return string;
		}
		
		/**
		 * Compare this string to another object
		 * @param object the object to compare to
		 * @return -1 if less, 1 otherwise (never equal!)
		 */
		public int compareTo(Object object)
		{
			if(object instanceof String){
				if(string.compareTo((String)object) > 1){
					return 1;
				}else{
					return -1;
				}
			}else{
				return -1;
			}
		}
	}
	
	/**
	 * A class which abbreviates vertex strings during label rendering.
	 * @param <V> the vertex type
	 */
	public static class ToAbbreviatedStringLabeller<V> implements Transformer<V,String>
	{
		/**
		 * Transform a vertex to a string.
		 * @param v a vertex
		 */
		public String transform(V v)
		{
			TreeSet<String> set = new TreeSet<String>();
			String[] strings;
			String string;
			int tmpi;
			
			string = v.toString();
			tmpi = string.indexOf("Edges:");
			if(tmpi >= 0) string = string.substring(0, tmpi);
			tmpi = string.indexOf(':');
			if(tmpi >= 0) string = string.substring(tmpi+1);
			strings = string.split(",");
			
			if(strings.length > 1){
				for(int i=0; i<strings.length; i++){
					set.add(strings[i]);
				}
				
				string = set.first() + "-" + set.last();
			}
			
			return string;
		}
	}
	
  /**
   * Class constructor.
   * @param iograph the I/O-Graph
   * @param rings the number of rings used when displaying the graph
   */
	public IOGraphPanel_JUNG(IOGraph<V,E> iograph, int rings)
  {
  	this(iograph, 600, 600, rings);
	}
	
  /**
   * Class constructor.
   * @param iograph the I/O-Graph
   * @param width the width of the panel
   * @param height the height of the panel
   * @param rings the number of rings used when displaying the graph
   */
  public IOGraphPanel_JUNG(IOGraph<V,E> iograph, int width, int height, int rings)
  {
  	this.iograph = iograph;
  	
  	//Build the JUNG graph
  	graph = new DirectedSparseGraph<String,UniqueString>();
  	Vector<String> vertices = iograph.getVertexStrings();
  	Vector<Vector<Integer>> edges = iograph.getAdjacencyList();
  	Vector<Vector<String>> edge_strings = iograph.getEdgeStrings();
  	String v0, v1;
  	
  	for(int i=0; i<vertices.size(); i++){
  		graph.addVertex((String)vertices.get(i));
  	}
  	
  	for(int i=0; i<edges.size(); i++){
  		for(int j=0; j<edges.get(i).size(); j++){
  			v0 = vertices.get(i);
  			v1 = vertices.get(edges.get(i).get(j));
  			graph.addEdge(new UniqueString(edge_strings.get(i).get(j)), v0, v1);
  		}
  	}
  	
  	//Collapse vertices alphabetically
  	if(true){
  		TreeSet<Character> set = new TreeSet<Character>();
	  	Vector<String> cluster_vertices = new Vector<String>();
	  	GraphCollapser collapser = new GraphCollapser(graph);
	  	Graph cluster;
	  	char c;
	  	
	  	//Find all first letters
	  	for(int i=0; i<vertices.size(); i++){
	  		if(vertices.get(i).length() > 0){
	  			set.add(vertices.get(i).charAt(0));
	  		}
	  	}
	  	
	  	//Clusters vertices with the same first letter
	  	for(Iterator<Character> itr=set.iterator(); itr.hasNext();){
	  		c = itr.next();
	  		cluster_vertices.clear();
	  		
	  		for(int i=0; i<vertices.size(); i++){
		  		if(vertices.get(i).length() > 0 && vertices.get(i).charAt(0) == c){
		  			cluster_vertices.add(vertices.get(i));
		  		}
		  	}	
	  		
	  		if(!cluster_vertices.isEmpty()){
		  		cluster = collapser.getClusterGraph(graph, cluster_vertices);
		  		graph = collapser.collapse(graph, cluster);
	  		}
	  	}
  	}
  	
  	//2D
  	Layout<String,UniqueString> layout = new CircleLayout(graph); 
  	layout.setSize(new Dimension(600,600));
  	DefaultModalGraphMouse gm = new DefaultModalGraphMouse();
  	gm.setMode(ModalGraphMouse.Mode.TRANSFORMING);
  	
  	VisualizationViewer<String,UniqueString> vv = new VisualizationViewer<String,UniqueString>(layout);
  	vv.setPreferredSize(new Dimension(600,600));
  	vv.setBackground(Color.white);
  	vv.setGraphMouse(gm);
  	//vv.getRenderContext().setVertexShapeTransformer(new ConstantTransformer(new Rectangle2D.Float(-6,-6,12,12)));
  	vv.getRenderContext().setVertexFillPaintTransformer(new PickableVertexPaintTransformer<String>(new MultiPickedState<String>(), Color.white, Color.blue));
  	vv.getRenderContext().setVertexLabelTransformer(new ToAbbreviatedStringLabeller());  	
  	vv.getRenderer().getVertexLabelRenderer().setPosition(Renderer.VertexLabel.Position.CNTR);
  	//vv.getRenderContext().setEdgeLabelTransformer(new ToStringLabeller());
  	//vv.getRenderContext().setEdgeShapeTransformer(new EdgeShape.QuadCurve<String,UniqueString>());
  	//vv.getRenderContext().setEdgeDrawPaintTransformer(new PickableEdgePaintTransformer<UniqueString>(new MultiPickedState<UniqueString>(), Color.black, Color.blue));
  	
  	/*
  	//3D
  	VisualizationViewer<String,UniqueString> vv = new VisualizationViewer<String,UniqueString>();	
  	vv.setGraphLayout(new SpringLayout(graph));
		*/
  	
  	add(vv);
  }
  
	/**
   * The main starting point for this program.
   * @param args command line arguments
   */
	public static void main(String args[])
	{
  	IOGraph iograph = null;
  	IOGraphPanel_JUNG iograph_panel = null;
  	
  	if(true){
	  	SoftwareServerClient icr = new SoftwareServerClient("localhost", 50000);
	  	iograph = new IOGraph<Data,Application>(icr);
	  	icr.close();
	  	
    	iograph_panel = new IOGraphPanel_JUNG<Data,Application>(iograph, 2);
  	}else if(false){
    	iograph = new IOGraph<String,String>("jdbc:mysql://isda.ncsa.uiuc.edu/csr", "demo", "demo");
    	iograph_panel = new IOGraphPanel_JUNG<String,String>(iograph, 2);
  	}else{
    	iograph = new IOGraph<String,String>("http://isda.ncsa.uiuc.edu/NARA/CSR_test/get_conversions.php");
    	iograph_panel = new IOGraphPanel_JUNG<String,String>(iograph, 2);
  	}
 
    JFrame frame = new JFrame("IOGraph Viewer");
    frame.add(iograph_panel);
    frame.pack();
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 
    frame.setVisible(true);
	}
}
package edu.ncsa.polyglot2;
import edu.ncsa.polyglot2.IOGraphViewerAuxiliary.*;
import edu.ncsa.icr.*;
import edu.ncsa.icr.ICRAuxiliary.*;
import edu.ncsa.utility.*;
import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.event.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

/**
 * A program that displays a graph of input/output operations and allows for the exploration of paths
 * among them from a source to a target.
 * @author Kenton McHenry
 */
public class IOGraphViewer<V extends Comparable,E> extends JPanel implements TreeSelectionListener, MouseListener, MouseMotionListener, ActionListener
{
  private static int graph_panel_width;
  private static int graph_panel_height;
  private static int top_pane_width;
  private static int top_pane_height;
  private Graphics bg;
  private Image offscreen;
  private int clicked_button = 0;
  private int clicked_x = -1;
  private int clicked_y = -1;
  private double theta = 0;
  private double theta_offset = 0;
  
	private IOGraph<V,E> iograph;  
  private Vector<Vertex2D> vertices = new Vector<Vertex2D>();
  private Vector<Vector<Integer>> edges = new Vector<Vector<Integer>>();
  private LinkedList<String> SelectedConverters = new LinkedList<String>();
  private int v0 = -1;
  private int v1 = -1;
  private Vector<Integer> v0_paths;
  private Vector<Integer> v1_domain;
  private Vector<Integer> v01 = new Vector<Integer>();
  private Vector<Double> v01_quality = new Vector<Double>();
  private int e0 = -1;
  private int e1 = -1;  
  private Set<Integer> WS = new TreeSet<Integer>();
  private Set<Integer> WS_outputs = new TreeSet<Integer>();
  private String output_string = "";
  
  private JPanel converter_panel; 
  private TextPanel output_panel;
  private JTree converter_tree;
  private JSplitPane splitpane1;  
  private JSplitPane splitpane2;
  private JPopupMenu popup_menu;
  private JMenuItem item_SET_v0_pm;
  private JMenuItem item_SET_v1_pm;
  private JCheckBoxMenuItem item_VIEW_ST_pm;
  private JCheckBoxMenuItem item_VIEW_VD_pm;
  private JMenuItem item_ADD_V_pm;
  private JMenuItem item_RMV_V_pm;
  private Stroke thinStroke = new BasicStroke(1);
  private Stroke wideStroke = new BasicStroke(4);
  
  private boolean SPANNING_TREE = false;
  private boolean VIEW_DOMAIN = false;
  private boolean VIEW_EDGE_QUALITY = false;
  private boolean ENABLE_WEIGHTED_PATHS = false;
  
  /**
   * Class constructor.
   */
  public IOGraphViewer(IOGraph<V,E> iograph)
  {
  	this(iograph, 600, 600);
  }
  
  /**
   * Class constructor.
   * @param iograph the IO-Graph
   * @param w the width of the panel
   * @param h the height of the panel
   */
  public IOGraphViewer(IOGraph<V,E> iograph, int w, int h)
  {
    graph_panel_width = w;
    graph_panel_height = h;
    top_pane_width = graph_panel_width + (int)Math.round(0.4*graph_panel_width);
    top_pane_height = graph_panel_height + (int)Math.round(0.175*graph_panel_height);
    
    setPreferredSize(new Dimension(graph_panel_width, graph_panel_height));
    setFont(new Font("Times New Roman", Font.BOLD, 12));
    addMouseListener(this);
    addMouseMotionListener(this);
    
    converter_panel = new JPanel();
    converter_panel.setBackground(Color.white);
    
    output_panel = new TextPanel();
    
    splitpane1 = new JSplitPane();  
    splitpane1.setOrientation(JSplitPane.VERTICAL_SPLIT);
    splitpane1.setDividerSize(0);
    splitpane1.setBorder(new EmptyBorder(0, 0, 0, 0));
    splitpane2 = new JSplitPane();
    splitpane2.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
    splitpane2.setDividerSize(0);
    splitpane2.setBorder(new EmptyBorder(0, 0, 0, 0));
    splitpane2.setSize(new Dimension(top_pane_width, top_pane_height));
    splitpane1.setTopComponent(this);
    splitpane1.setBottomComponent(output_panel);
    splitpane2.setLeftComponent(converter_panel);
    splitpane2.setRightComponent(splitpane1);
    
    //Setup menus
    JPopupMenu.setDefaultLightWeightPopupEnabled(false);
    
    popup_menu = new JPopupMenu(); 
    item_SET_v0_pm = new JMenuItem("Source"); item_SET_v0_pm.addActionListener(this); popup_menu.add(item_SET_v0_pm);
    item_SET_v1_pm = new JMenuItem("Target"); item_SET_v1_pm.addActionListener(this); popup_menu.add(item_SET_v1_pm);
    popup_menu.addSeparator();
    item_VIEW_ST_pm = new JCheckBoxMenuItem("Spanning Tree"); item_VIEW_ST_pm.addActionListener(this); popup_menu.add(item_VIEW_ST_pm); item_VIEW_ST_pm.setState(SPANNING_TREE);
    item_VIEW_VD_pm = new JCheckBoxMenuItem("Domain"); item_VIEW_VD_pm.addActionListener(this); popup_menu.add(item_VIEW_VD_pm); item_VIEW_VD_pm.setState(VIEW_DOMAIN);
    popup_menu.addSeparator();
    item_ADD_V_pm = new JMenuItem("Add to Working Set"); item_ADD_V_pm.addActionListener(this); popup_menu.add(item_ADD_V_pm);
    item_RMV_V_pm = new JMenuItem("Remove from Working Set"); item_RMV_V_pm.addActionListener(this); popup_menu.add(item_RMV_V_pm);
    
  	setGraph(iograph);
  }
  
  /**
   * Set up the GUI and graph based on the loaded IOGraph data.
   * @param iograph the IO-Graph to use
   */
  private void setGraph(IOGraph<V,E> iograph)
  {
  	this.iograph = iograph;
    vertices = Vertex2D.getVertices(iograph.getVertexStrings());
    edges = iograph.getAdjacencyList();
    Vertex2D.setCircularGraph(vertices, graph_panel_width+50, graph_panel_height, theta);

    //Build converter JTree    
    Vector<Vector<String>> converters = iograph.getEdgeStrings();
    TreeSet<String> set = new TreeSet<String>();
    DefaultMutableTreeNode root = new DefaultMutableTreeNode("Applications");
    DefaultMutableTreeNode child;
    
    child = new DefaultMutableTreeNode("ALL");
    root.add(child);
    
    for(int i=0; i<converters.size(); i++){
    	for(int j=0; j<converters.get(i).size(); j++){
	    	if(!set.contains(converters.get(i).get(j).toString())){
	    		set.add(converters.get(i).get(j).toString());
		      child = new DefaultMutableTreeNode(converters.get(i).get(j).toString());
		      root.add(child);
	    	}
    	}
    }
    
    converter_tree = new JTree(root);
    converter_tree.addTreeSelectionListener(this);
    converter_panel.add(converter_tree, BorderLayout.WEST);
  }
  
  /**
   * Enable/disable the display of edge quality information in the graph.
   * @param b enable/disable edge quality values
   */
  public void viewEdgeQuality(boolean b)
  {
  	VIEW_EDGE_QUALITY = b;
  }
  
  /**
   * Enable/disable the use of weighted paths when searching for conversions in the graph.
   * @param b enable/disable weighted paths
   */
  public void enableWeightedPaths(boolean b)
  {
  	ENABLE_WEIGHTED_PATHS = b;
  }
  
  /**
   * Return an auxiliary interface pane.
   * @return the top most pane
   */
  public JSplitPane getAuxInterfacePane()
  {
    return splitpane2;
  }
  
  /**
   * Draw an arrow to the given graphics context.
   *  @param g the graphics context to draw to
   *  @param x0 the starting x coordinate
   *  @param y0 the starting y coordinate
   *  @param x1 the ending x coordinate
   *  @param y1 the ending y coordinate
   *  @param DRAW_ARROWHEAD true if the arrow head should be drawn
   */
  private void drawArrow(Graphics g, int x0, int y0, int x1,int y1, boolean DRAW_ARROWHEAD)
  {
    double arrow_h = 5;
    double arrow_w = 5;
    double dx, dy, length;
    double alpha, beta;
    
    dx = x1 - x0;
    dy = y1 - y0;
    length = Math.sqrt(dx*dx+dy*dy);
    dx /= length;
    dy /= length;

    g.drawLine(x0, y0, x1, y1);
    
    if(DRAW_ARROWHEAD){
    	alpha = (1-arrow_h/length)*length;
    	beta = arrow_w;
    	
	    g.drawLine(x0 + (int)(alpha*dx + beta*dy), y0 + (int)(alpha*dy - beta*dx), x1, y1);
	    g.drawLine(x0 + (int)(alpha*dx - beta*dy), y0 + (int)(alpha*dy + beta*dx), x1, y1);
    }
  }
  
  /**
   * Draw an arrow to the given graphics context.
   *  @param g the graphics context to draw to
   *  @param x0 the starting x coordinate
   *  @param y0 the starting y coordinate
   *  @param x1 the ending x coordinate
   *  @param y1 the ending y coordinate
   *  @param label a label to be drawn over the arrow
   *  @param DRAW_ARROWHEAD true if the arrow head should be drawn
   */
  private void drawArrow(Graphics g, int x0, int y0, int x1,int y1, String label, boolean DRAW_ARROWHEAD)
  {
    drawArrow(g, x0, y0, x1, y1, DRAW_ARROWHEAD);
    
    FontMetrics fm = g.getFontMetrics();
    int ascent = fm.getMaxAscent();
    int descent= fm.getMaxDescent();
    int msg_width = fm.stringWidth(label);
    int x = (x1 + x0) / 2;
    int y = (y1 + y0) / 2;
    int w = (int)Math.round(1.5* msg_width);
    int h = (int)Math.round(1.25 * (descent + ascent));
    Color color = g.getColor();
        
    g.setColor(Color.white);
    g.fillRect(x-w/2, y-h/2, w, h);
    g.setColor(color);
    g.drawRect(x-w/2, y-h/2, w, h);
    g.setColor(Color.black);
    g.drawString(label, x-msg_width/2, y-descent/2+ascent/2);
    g.setColor(color);
  }
  
  /**
   * Calculate the distance between the given points
   *  @param x0 the x-coordinate of the first point
   *  @param y0 the y-coordinate of the first point
   *  @param x1 the x-coordinate of the second point
   *  @param y1 the y-coordinate of the second point
   */
  public double distance(int x0, int y0, int x1, int y1)
  {
    double dx = x1 - x0;
    double dy = y1 - y0;
    
    return Math.sqrt(dx*dx + dy*dy);
  }
  
  /**
   * Calculate the distance of the given point from the given line segment.
   * Note: Uses perpendicular distances to line segment.
   *  @param x the x-coordinate of the point
   *  @param y the y-coordinate of the point
   *  @param x0 the x-coordinate of the line segments starting point
   *  @param y0 the y-coordinate of the line segments starting point
   *  @param x1 the x-coordinate of the line segments ending point
   *  @param y1 the y-coordinate of the line segments ending point
   *  @return the distance of the point from the line segment
   */
  public double distance(int x, int y, int x0, int y0, int x1, int y1)
  {
  	double dx, dy, dx0, dx1, dy0, dy1, length, t;
  	
  	dx0 = x - x0;
  	dy0 = y - y0;
  	dx1 = x1 - x0;
  	dy1 = y1 - y0;
  	length = distance(x0, y0, x1, y1);
  	t = (dx0*dx1 + dy0*dy1) / (length*length);
  	
  	if(t <= 0){
  		return distance(x, y, x0, y0);
  	}else if(t >= 1){
  		return distance(x, y, x1, y1);
  	}else{
    	dx = x1 - x0;
    	dy = y1 - y0;
    	
  		return distance(x, y, (int)Math.round(x0+t*dx), (int)Math.round(y0+t*dy));
  	}
  }
  
  /**
	 * Highlight the adjacent edge of v0 that is nearest to the given location.
	 *  @param x the x-coordinate of the clicked point
	 *  @param y the y-coordinate of the clicked point
	 */
	public void highlightAdjacentEdge(int x, int y)
	{
		if(v0 >= 0 && SPANNING_TREE){
	  	int mini = -1;
	  	double mind = Double.MAX_VALUE;
	  	double tmpd;
	  	
	  	for(int i=0; i<v0_paths.size(); i++){
	  		if(v0_paths.get(i) == v0 && i != v0){
	    		tmpd = distance(x, y, vertices.get(i).x, vertices.get(i).y, vertices.get(v0).x, vertices.get(v0).y);
	    		
	    		if(tmpd < mind){
	    			mind = tmpd;
	    			mini = i;
	    		}
	  		}
	  	}
	  	
	  	if(mini >= 0){
	    	e0 = v0;
	    	e1 = mini;
	    	repaint();
	  	}
	  }
	}

	/**
   * Draw the graph to the given graphics context.
   *  @param g the graphics context to draw to
   */
  public void paint(Graphics g)
  {
    int width = getSize().width;
    int height = getSize().height;
    FontMetrics fm;
    int ascent, descent, msg_width;
    int x, y, x0, y0, x1, y1;
    
    //Update background buffer if needed
    if(offscreen == null || width != offscreen.getWidth(null) || height != offscreen.getHeight(null)){ 
      offscreen = createImage(width, height);
      bg = offscreen.getGraphics();
	    splitpane1.setDividerLocation((float)graph_panel_height/(float)(top_pane_height-40));
    }
    
    super.paint(bg);
    bg.setColor(Color.white);
    bg.fillRect(0, 0, width, height);
    
    fm = bg.getFontMetrics();
    ascent = fm.getMaxAscent();
    descent= fm.getMaxDescent();
    
    //Draw edges
    bg.setColor(new Color(0x00cccccc));
    
    for(int i=0; i<edges.size(); i++){
      for(int j=0; j<edges.get(i).size(); j++){
        x0 = vertices.get(i).x;
        y0 = vertices.get(i).y;
        x1 = vertices.get(edges.get(i).get(j)).x;
        y1 = vertices.get(edges.get(i).get(j)).y;
        
        drawArrow(bg, x0, y0, x1, y1, true);
      }
    }
    
    //Draw spanning tree
    if(SPANNING_TREE && (v0 >= 0)){
    	((Graphics2D)bg).setStroke(wideStroke);
      bg.setColor(new Color(0x00c0c0c0));
      
      for(int i=0; i<v0_paths.size(); i++){
        if(v0_paths.get(i) >= 0){
          x0 = vertices.get(v0_paths.get(i)).x;
          y0 = vertices.get(v0_paths.get(i)).y;
          x1 = vertices.get(i).x;
          y1 = vertices.get(i).y;
          
          drawArrow(bg, x0, y0, x1, y1, true);
        }
      }
      
      //Indicate adjacent edges
      bg.setColor(new Color(0x00a0a0a0));
      
      for(int i=0; i<v0_paths.size(); i++){
        if(v0_paths.get(i) == v0 && i != v0){
          x0 = vertices.get(v0_paths.get(i)).x;
          y0 = vertices.get(v0_paths.get(i)).y;
          x1 = vertices.get(i).x;
          y1 = vertices.get(i).y;
          
          drawArrow(bg, x0, y0, x1, y1, true);
        }
      }
      
	    ((Graphics2D)bg).setStroke(thinStroke);
    }
    
    //Draw vertices
    for(int i=0; i<vertices.size(); i++){
      if(i == v0){
        bg.setColor(Color.red);
      }else if(i == v1){
        bg.setColor(Color.blue);
      }else if(WS.contains(i)){
        bg.setColor(new Color(0xa000a0));
      }else{
        bg.setColor(Color.black);
      }
      
      x = vertices.get(i).x;
      y = vertices.get(i).y;
      msg_width = fm.stringWidth(vertices.get(i).text);
      bg.drawString(vertices.get(i).text, x-msg_width/2, y-descent/2+ascent/2);
    }
    
    //Draw domain
    if(VIEW_DOMAIN && (v1 >= 0)){
      bg.setColor(Color.lightGray);
      
    	for(int i=0; i<vertices.size(); i++){
	      x = vertices.get(i).x;
	      y = vertices.get(i).y;
	      msg_width = fm.stringWidth(vertices.get(i).text);
	      bg.drawString(vertices.get(i).text, x-msg_width/2, y-descent/2+ascent/2);
	    }
    	
      bg.setColor(Color.black);
  	
      for(int i=0; i<v1_domain.size(); i++){
        x = vertices.get(v1_domain.get(i)).x;
        y = vertices.get(v1_domain.get(i)).y;
        msg_width = fm.stringWidth(vertices.get(v1_domain.get(i)).text);
        bg.drawString(vertices.get(v1_domain.get(i)).text, x-msg_width/2, y-descent/2+ascent/2);
      }
    }
    
    //Draw path
    ((Graphics2D)bg).setStroke(wideStroke);
    bg.setColor(new Color(0x00c3a3bd));
    
    for(int i=0; i<v01.size()-1; i++){
      x0 = vertices.get(v01.get(i)).x;
      y0 = vertices.get(v01.get(i)).y;
      x1 = vertices.get(v01.get(i+1)).x;
      y1 = vertices.get(v01.get(i+1)).y;
      
      if(VIEW_EDGE_QUALITY){
      	drawArrow(bg, x0, y0, x1, y1, Utility.round(v01_quality.get(i), 2), i==(v01.size()-2));
      }else{
      	drawArrow(bg, x0, y0, x1, y1, i==(v01.size()-2));
      }
    }
    
    ((Graphics2D)bg).setStroke(thinStroke);
    
    //Draw selected edges
    if(e0 >= 0 && e1 >= 0){
	    ((Graphics2D)bg).setStroke(wideStroke);
	    bg.setColor(new Color(0x00fff38f));
	    
	    x0 = vertices.get(e0).x;
	    y0 = vertices.get(e0).y;
	    x1 = vertices.get(e1).x;
	    y1 = vertices.get(e1).y;
	    
	    if(VIEW_EDGE_QUALITY){
	    	drawArrow(bg, x0, y0, x1, y1, Utility.round(iograph.getMaxEdgeWeight(e0, e1), 2), true);
	    }else{
	    	drawArrow(bg, x0, y0, x1, y1, true);
	    }
	
	    ((Graphics2D)bg).setStroke(thinStroke);
    }
    
    //Redraw coverted path points
    bg.setColor(Color.black); 
    
    for(int i=0; i<v01.size(); i++){
      x = vertices.get(v01.get(i)).x;
      y = vertices.get(v01.get(i)).y;
      msg_width = fm.stringWidth(vertices.get(v01.get(i)).text);
      bg.drawString(vertices.get(v01.get(i)).text, x-msg_width/2, y-descent/2+ascent/2);
    }
    
    //Redraw path covered endpoints
    if(v0 >= 0){
      x = vertices.get(v0).x;
      y = vertices.get(v0).y;
      msg_width = fm.stringWidth(vertices.get(v0).text);
      bg.setColor(Color.white);
      bg.drawString(vertices.get(v0).text, x-msg_width/2-1, y-descent/2+ascent/2+1);
      bg.setColor(new Color(0x00cf0000));
      bg.drawString(vertices.get(v0).text, x-msg_width/2, y-descent/2+ascent/2);
    }
    
    if(v1 >= 0){
      x = vertices.get(v1).x;
      y = vertices.get(v1).y;
      msg_width = fm.stringWidth(vertices.get(v1).text);
    	bg.setColor(Color.white);      
      bg.drawString(vertices.get(v1).text, x-msg_width/2-1, y-descent/2+ascent/2+1);
    	bg.setColor(new Color(0x00586cff));      
      bg.drawString(vertices.get(v1).text, x-msg_width/2, y-descent/2+ascent/2);
    }
    
    if(e1 >= 0){
      x = vertices.get(e1).x;
      y = vertices.get(e1).y;
      msg_width = fm.stringWidth(vertices.get(e1).text);
      bg.setColor(Color.black);      
      bg.drawString(vertices.get(e1).text, x-msg_width/2, y-descent/2+ascent/2);
    }
    
    output_panel.setText(output_string);
    
    //Draw background  buffer
    g.drawImage(offscreen, 0, 0, this);
  }
  
  /**
	 * The action listener used to handle menu events.
	 *  @param e the action event
	 */
	public void actionPerformed(ActionEvent e)
	{
	  JMenuItem source = (JMenuItem)e.getSource();
	  
	  if(source == item_SET_v0_pm || source == item_SET_v1_pm || source == item_ADD_V_pm || source == item_RMV_V_pm){
	    int tmpx, tmpy, tmpd;
	    int mini = -1;
	    int mind = Integer.MAX_VALUE;
	    
	    for(int i=0; i<vertices.size(); i++){
	      tmpx = clicked_x - vertices.get(i).x;
	      tmpy = clicked_y - vertices.get(i).y;
	      tmpd = tmpx*tmpx + tmpy*tmpy;
	      
	      if(tmpd < mind){
	        mini = i;
	        mind = tmpd;
	      }
	    }
	  
	    if(source == item_SET_v0_pm || source == item_SET_v1_pm){
	      if(source == item_SET_v0_pm){
	        v0 = mini;
	        
	        if(!ENABLE_WEIGHTED_PATHS){
	        	v0_paths = iograph.getShortestPaths(v0);
	        }else{
	        	v0_paths = iograph.getShortestWeightedPaths(v0).first;
	        }
	      }else if(source == item_SET_v1_pm){
	        v1 = mini;
	        v1_domain = iograph.getDomain(v1);
	      }
	    
	      if((v0 >= 0) && (v1 >= 0)){
	        v01 = IOGraph.getPath(v0, v1, v0_paths);
	        v01_quality = iograph.getPathQuality(v01);
	        output_string = iograph.toString(v01);
	          
	        if(!v01.isEmpty()){
	        	output_string = "\n" + output_string;
	        }else{
	          output_string = "\nNo path exists!";
	        }
	        
	        output_panel.alignCenter(true);
	      }
	    }else if(source == item_ADD_V_pm || source == item_RMV_V_pm){
	      if(source == item_ADD_V_pm){
	        WS.add(mini);
	        
	        if(WS.size() == 1){
	          WS_outputs = iograph.getSpanningTree(mini);
	        }else{
	          WS_outputs.retainAll(iograph.getSpanningTree(mini));
	        }
	      }else if(source == item_RMV_V_pm){
	        WS.remove(mini);
	        WS_outputs.clear();
	        Object[] arr = WS.toArray();
	        
	        for(int i=0; i<arr.length; i++){
	          if(i == 0){
	            WS_outputs = iograph.getSpanningTree((Integer)arr[i]);
	          }else{
	            WS_outputs.retainAll(iograph.getSpanningTree((Integer)arr[i]));
	          }
	        }
	      }
	      
	      //Print out output set
	      Object[] arr = (Object[])WS_outputs.toArray();
	      output_string = "Intersection: ";
	      
	      for(int i=0; i<arr.length; i++){
	        if(i > 0) output_string += ", ";
	        output_string += vertices.get((Integer)arr[i]).text;  
	      }
	      
	      output_panel.alignCenter(false);
	    }
	  }else if(source == item_VIEW_ST_pm){
	    SPANNING_TREE = !SPANNING_TREE;
	    item_VIEW_ST_pm.setState(SPANNING_TREE);
	  }else if(source == item_VIEW_VD_pm){
	    VIEW_DOMAIN = !VIEW_DOMAIN;
	    item_VIEW_VD_pm.setState(VIEW_DOMAIN);
	  }
	
	  repaint();
	}

	/**
   * The tree selection listener used to handle selections in the side tree.
   *  @param e the tree selection event
   */
  public void valueChanged(TreeSelectionEvent e)
  {
    String name;
    
    TreePath[] treepaths = e.getPaths();
    for(int i=0; i<treepaths.length; i++){
      if(treepaths[i].getPathCount() > 1){
        name = treepaths[i].getPathComponent(1).toString();
        
        if(SelectedConverters.contains(name)){
          SelectedConverters.remove(name);
        }else{
          SelectedConverters.add(name);
        }
      }
    }
    
    //Reset state
    v0 = -1;
    v1 = -1;
    v01.clear();
    if(v0_paths != null) v0_paths.clear();
    e0 = -1;
    e1 = -1;
    WS.clear();
    WS_outputs.clear();
    output_string = "";
    
    //Update edges
    if(SelectedConverters.contains("ALL")){	
  		//iograph.setEdges();
    }else{
    	//iograph.setEdges(new Vector<String>(SelectedConverters));
    }
    
    edges = iograph.getAdjacencyList();
    
    repaint();
  }
  
  /**
   * The mouse pressed listener used to open the popup menu.
   *  @param e the mouse event
   */
  public void mousePressed(MouseEvent e)
  {
  	clicked_x = e.getX();
  	clicked_y = e.getY();  	
  	
    if(e.getButton() == MouseEvent.BUTTON1){
    	clicked_button = 1;
    	
    	if(v0 >= 0 && SPANNING_TREE){
    		highlightAdjacentEdge(e.getX(), e.getY());
    	}
    }else if(e.getButton() == MouseEvent.BUTTON3){
    	clicked_button = 3;
      popup_menu.show(e.getComponent(), e.getX(), e.getY());
    }
  }
  
  /**
   * The mouse dragged listener used to select edges.
   *  @param e the mouse event
   */
  public void mouseDragged(MouseEvent e)
  {  	
  	if(clicked_button == 1){
	  	if(v0 >= 0 && SPANNING_TREE){
	  		highlightAdjacentEdge(e.getX(), e.getY());
	  	}else{
	  		theta_offset = 0.25 * 360.0 * (e.getY() - clicked_y) / (double)graph_panel_width;
	  		Vertex2D.setCircularGraph(vertices, getSize().width, getSize().height, theta + theta_offset);
	  		repaint();
	  	}
  	}
  }
  
  /**
   * The mouse released listener used to de-select edges.
   *  @param e the mouse event
   */
  public void mouseReleased(MouseEvent e)
  {
    if(e.getButton() == MouseEvent.BUTTON1){
    	if(v0 >= 0 && SPANNING_TREE){
	      e0 = -1;
	      e1 = -1;
	      repaint();
    	}else{
    		theta += theta_offset;
    		theta_offset = 0;
    		repaint();
    	}
    }
    
    clicked_button = 0;
  }

  public void mouseExited(MouseEvent e) {}
  public void mouseEntered(MouseEvent e) {}
  public void mouseClicked(MouseEvent e) {}
  public void mouseMoved(MouseEvent e) {}
	
  /**
   * The main starting point for this program.
   *  @param args optional, if present the GUI will not be displayed and text output indicated the conversion path between two types will be display instead
   */
  public static void main(String args[])
  {
  	final ICRClient icr = new ICRClient("ICRClient.ini");
  	IOGraph<Data,Application> iograph = new IOGraph<Data,Application>(icr); icr.close();
    IOGraphViewer<Data,Application> iograph_viewer = new IOGraphViewer<Data,Application>(iograph);
 
    if(args.length == 0){
      JFrame frame = new JFrame("IOGraph Viewer");
      frame.setSize(top_pane_width, top_pane_height);
      frame.add(iograph_viewer.getAuxInterfacePane());
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 
      frame.setVisible(true);
      
//      iograph.printQualityInfo();
//    }else{
//      Vector<String> tmpv;
//      boolean ALL = false;
//      boolean DOMAIN = false;
//      int count = 0;
//      
//      for(int i=0; i<args.length; i++){
//        if(args[i].charAt(0) == '-'){
//          if(args[i].equals("-all")){
//            ALL = true;
//          }else if(args[i].equals("-domain")){
//            DOMAIN = true;
//          }
//        }else{
//          count++;
//        }
//      }
//    
//      if(count == 1){
//      	if(DOMAIN){	//Domain
//      		tmpv = iograph.getDomain(args[0]);
//      	}else{			//Span/range
//	        tmpv = iograph.getSpanningTree(args[0]);
//      	}
//      	
//        for(int i=0; i<tmpv.size(); i++){
//          System.out.println(tmpv.get(i));
//        }
//      }else if(count == 2){
//        if(ALL){		//All parallel shortest paths
//          tmpv = iograph.getTasks(args[0], args[1]);
//          
//          for(int i=0; i<tmpv.size(); i++){
//            System.out.println(tmpv.get(i));
//          }
//        }else{			//Shortest path
//        	System.out.print(iograph.getTask(args[0], args[1], iograph_viewer.ENABLE_WEIGHTED_PATHS));
//        }
//      }
    }
  }
}

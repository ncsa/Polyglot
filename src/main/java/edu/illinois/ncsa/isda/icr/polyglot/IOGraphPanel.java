package edu.illinois.ncsa.isda.icr.polyglot;
import edu.illinois.ncsa.isda.icr.*;
import edu.illinois.ncsa.isda.icr.ICRAuxiliary.*;
import edu.illinois.ncsa.isda.icr.polyglot.PolyglotAuxiliary.*;
import kgm.image.*;
import kgm.utility.*;
import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.event.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

/**
 * A panel/program that displays a graph of input/output operations and allows for the exploration of paths
 * among them from a source to a target.
 * @author Kenton McHenry
 */
public class IOGraphPanel<V extends Comparable,E> extends JPanel implements TreeSelectionListener, MouseListener, MouseMotionListener, MouseWheelListener, ActionListener
{
  private Graphics bg;
  private Image offscreen;
  private int clicked_button = 0;
  private int clicked_x = -1;
  private int clicked_y = -1;
    
  private int rings = 1;
  private double theta = 0;
  private double theta_offset = 0;
  private String edges_alias = "edges";
  private int vertex_color = 0x000000;
  private int edge_color = 0xcccccc;
  
	private IOGraph<V,E> iograph;  
  private Vector<Point2D> vertices = new Vector<Point2D>();
  private Vector<Vector<Integer>> edges = new Vector<Vector<Integer>>();
  protected Vector<Vector<String>> edge_strings = null;
  protected Vector<Vector<Color>> edge_colors = null;
  private Vector<Boolean> active_vertices = new Vector<Boolean>();
  private Vector<Vector<Boolean>> active_edges = new Vector<Vector<Boolean>>();
  private TreeSet<String> selected_edges = new TreeSet<String>();
  private int source = -1;
  private int target = -1;
  private Vector<Integer> paths;
  private TreeSet<Integer> domain;
  private Vector<Integer> highlighted_path = new Vector<Integer>();
  private Vector<Double> highlighted_path_quality = new Vector<Double>();
  private int highlighted_edge_v0 = -1;
  private int highlighted_edge_v1 = -1;  
  private TreeSet<Integer> working_set = new TreeSet<Integer>();
  private TreeSet<Integer> working_set_range_intersection = new TreeSet<Integer>();
  private String output_string = "";
  
  private JTree edge_string_tree;
  private JScrollPane edge_string_pane;
  private TextPanel output_panel;
  private JSplitPane splitpane1;  
  private JSplitPane splitpane2;
  private JPopupMenu popup_menu;
  private JMenuItem menuitem_SET_SOURCE;
  private JMenuItem menuitem_SET_TARGET;
  private JCheckBoxMenuItem menuitem_VIEW_RANGE;
  private JCheckBoxMenuItem menuitem_VIEW_DOMAIN;
  private JMenuItem menuitem_WORKINGSET_ADD;
  private JMenuItem menuitem_WORKINGSET_REMOVE;
  private Stroke thickness1_stroke = new BasicStroke(1);
  private Stroke thickness2_stroke = new BasicStroke(2);
  private Stroke thickness4_stroke = new BasicStroke(4);
  
  private boolean SET_VERTEX_POSITIONS = false;
  private boolean VIEW_RANGE = false;
  private boolean VIEW_DOMAIN = false;
  private boolean VIEW_EDGE_QUALITY = false;
  private boolean ENABLE_WEIGHTED_PATHS = false;
  private boolean MENU_SEPERATOR = false;

  /**
   * Class constructor.
   */
  public IOGraphPanel()
  {
  	this(new IOGraph<V,E>(), 600, 600, 2, null);
  }
  
  /**
   * Class constructor.
   * @param iograph the I/O-Graph
   * @param rings the number of rings used when displaying the graph
   */
  public IOGraphPanel(IOGraph<V,E> iograph, int rings)
  {
  	this(iograph, 600, 600, rings, null);
  }
  
  /**
   * Class constructor.
   * @param iograph the I/O-Graph
   * @param rings the number of rings used when displaying the graph
   * @param edges_alias the alias to use for edges
   */
  public IOGraphPanel(IOGraph<V,E> iograph, int rings, String edges_alias)
  {
  	this(iograph, 600, 600, rings, edges_alias);
  }
  
  /**
   * Class constructor.
   * @param iograph the I/O-Graph
   * @param width the width of the panel
   * @param height the height of the panel
   * @param rings the number of rings used when displaying the graph
   */
  public IOGraphPanel(IOGraph<V,E> iograph, int width, int height, int rings)
  {
  	this(iograph, width, height, rings, null);
  }
  
  /**
   * Class constructor.
   * @param iograph the I/O-Graph
   * @param width the width of the panel
   * @param height the height of the panel
   * @param rings the number of rings used when displaying the graph
   * @param edges_alias the alias to use for edges
   */
  public IOGraphPanel(IOGraph<V,E> iograph, int width, int height, int rings, String edges_alias)
  {  
  	if(edges_alias != null){
  		this.edges_alias = edges_alias;
  	}
  	
    setGraph(iograph, false);
  	this.rings = rings;
    
    setPreferredSize(new Dimension(width, height));
    setFont(new Font("Times New Roman", Font.BOLD, 12));
    addMouseListener(this);
    addMouseMotionListener(this);
    addMouseWheelListener(this);
    
    edge_string_pane.setBorder(new EmptyBorder(0, 0, 0, 0));
    edge_string_pane.setMinimumSize(new Dimension(180, 0));
    edge_string_pane.getVerticalScrollBar().setOpaque(false);
    edge_string_pane.getVerticalScrollBar().setUI(new FadedScrollBarUI());
    output_panel = new TextPanel();
    output_panel.setMinimumSize(new Dimension(0, 60));

    splitpane1 = new JSplitPane();
    splitpane1.setBorder(new EmptyBorder(0, 0, 0, 0));
    splitpane1.setOrientation(JSplitPane.VERTICAL_SPLIT);
    splitpane1.setDividerSize(0);
    splitpane1.setResizeWeight(0.9);
    splitpane2 = new JSplitPane();
    splitpane2.setBorder(new EmptyBorder(0, 0, 0, 0));
    splitpane2.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
    splitpane2.setDividerSize(0);
    splitpane1.setTopComponent(this);
    splitpane1.setBottomComponent(output_panel);
    splitpane2.setLeftComponent(edge_string_pane);
    splitpane2.setRightComponent(splitpane1);
    
    //Setup menus
    JPopupMenu.setDefaultLightWeightPopupEnabled(false);
    
    popup_menu = new JPopupMenu(); 
    menuitem_SET_SOURCE = new JMenuItem("Source"); menuitem_SET_SOURCE.addActionListener(this); popup_menu.add(menuitem_SET_SOURCE);
    menuitem_SET_TARGET = new JMenuItem("Target"); menuitem_SET_TARGET.addActionListener(this); popup_menu.add(menuitem_SET_TARGET);
    popup_menu.addSeparator();
    menuitem_VIEW_RANGE = new JCheckBoxMenuItem("Range"); menuitem_VIEW_RANGE.addActionListener(this); popup_menu.add(menuitem_VIEW_RANGE); menuitem_VIEW_RANGE.setState(VIEW_RANGE);
    menuitem_VIEW_DOMAIN = new JCheckBoxMenuItem("Domain"); menuitem_VIEW_DOMAIN.addActionListener(this); popup_menu.add(menuitem_VIEW_DOMAIN); menuitem_VIEW_DOMAIN.setState(VIEW_DOMAIN);
    popup_menu.addSeparator();
    menuitem_WORKINGSET_ADD = new JMenuItem("Add to Working Set"); menuitem_WORKINGSET_ADD.addActionListener(this); popup_menu.add(menuitem_WORKINGSET_ADD);
    menuitem_WORKINGSET_REMOVE = new JMenuItem("Remove from Working Set"); menuitem_WORKINGSET_REMOVE.addActionListener(this); popup_menu.add(menuitem_WORKINGSET_REMOVE);
  }
  
  /**
   * Add a new item to the popup menu. If this is the first time called it will also add a seperator bar.
   * @param item the item to be added.
   */
  public void addPopupMenu(JMenuItem item) {
  	if (!MENU_SEPERATOR) {
  		MENU_SEPERATOR = true;
  		popup_menu.addSeparator();
  	}
  	popup_menu.add(item);
  }
  /**
   * Set up the GUI and graph based on the loaded IOGraph data.
   * @param iograph the IO-Graph to use
   * @param VIEW_ALL true if by default all data should be drawn
   */
  private void setGraph(IOGraph<V,E> iograph, boolean VIEW_ALL)
  {
  	this.iograph = iograph;

    //Build converter JTree    
    edge_strings = iograph.getEdgeStrings();
    TreeSet<String> set = new TreeSet<String>();
    DefaultMutableTreeNode root = new DefaultMutableTreeNode(Utility.capitalize(edges_alias));
    DefaultMutableTreeNode child;
    
    if(VIEW_ALL){
    	child = new DefaultMutableTreeNode("ALL");
    	root.add(child);
    }
    
    for(int i=0; i<edge_strings.size(); i++){
    	for(int j=0; j<edge_strings.get(i).size(); j++){
	    	if(!set.contains(edge_strings.get(i).get(j).toString())){
	    		set.add(edge_strings.get(i).get(j).toString());
	    	}
    	}
    }
    
    for(Iterator<String> itr=set.iterator(); itr.hasNext();){
      child = new DefaultMutableTreeNode(itr.next());
      root.add(child);
    }
    
    edge_string_tree = new JTree(root);
    edge_string_tree.addTreeSelectionListener(this);
    edge_string_pane = new JScrollPane(edge_string_tree);
    
    //Get required graph information
    vertices = Point2D.getVertices(iograph.getVertexStrings());
    edges = iograph.getAdjacencyList();
    active_edges = iograph.setActiveEdges(VIEW_ALL);
    active_vertices = iograph.getActiveVertices();
  	
    SET_VERTEX_POSITIONS = true;
  }
  
  /**
   * Set the width of the auxiliary side pane.
   * @param width the desired width of the auxiliary side pane
   */
  public void setSidePaneWidth(int width)
  {
  	edge_string_pane.setMinimumSize(new Dimension(width, 0));
  }
  
  /**
   * Set the height of the output panel.
   * @param height the desired height of the output panel
   */
  public void setOutputPanelHeight(int height)
  {
    output_panel.setMinimumSize(new Dimension(0, height));
  }
  
  /**
	 * Set the vertex color.
	 * @param color the color to use for vertices
	 */
	public void setVertexColor(int color)
	{
	  vertex_color = color;
	}

	/**
	 * Set the edge color.
	 * @param color the color to use for edges
	 */
	public void setEdgeColor(int color)
	{
	  edge_color = color;
	}

	/**
   * Enable/disable the display of edge quality information in the graph.
   * @param b enable/disable edge quality values
   */
  public void setViewEdgeQuality(boolean b)
  {
  	VIEW_EDGE_QUALITY = b;
  }
  
  /**
   * Enable/disable the use of weighted paths when searching for conversions in the graph.
   * @param b enable/disable weighted paths
   */
  public void setEnableWeightedPaths(boolean b)
  {
  	ENABLE_WEIGHTED_PATHS = b;
  }
  
  /**
   * Return an auxiliary interface pane.
   * @return the top most pane
   */
  public JSplitPane getAuxiliaryInterfacePane()
  {
    return splitpane2;
  }
  
  /**
	 * Highlight the adjacent source edge that is nearest to the given location.
	 * @param x the x-coordinate of the clicked point
	 * @param y the y-coordinate of the clicked point
	 */
	private void highlightAdjacentSourceEdge(int x, int y)
	{
		if(source >= 0 && VIEW_RANGE){
	  	int mini = -1;
	  	double mind = Double.MAX_VALUE;
	  	double tmpd;
	  	
	  	for(int i=0; i<paths.size(); i++){
	  		if(paths.get(i) == source && i != source){
	    		tmpd = ImageUtility.distance(x, y, vertices.get(i).x, vertices.get(i).y, vertices.get(source).x, vertices.get(source).y);
	    		
	    		if(tmpd < mind){
	    			mind = tmpd;
	    			mini = i;
	    		}
	  		}
	  	}
	  	
	  	if(mini >= 0){
	    	highlighted_edge_v0 = source;
	    	highlighted_edge_v1 = mini;
	    	repaint();
	  	}
	  }
	}

	/**
	 * Draw an arrow to the given graphics context.
	 * @param g the graphics context to draw to
	 * @param x0 the starting x coordinate
	 * @param y0 the starting y coordinate
	 * @param x1 the ending x coordinate
	 * @param y1 the ending y coordinate
	 * @param DRAW_ARROWHEAD true if the arrow head should be drawn
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
	 * @param g the graphics context to draw to
	 * @param x0 the starting x coordinate
	 * @param y0 the starting y coordinate
	 * @param x1 the ending x coordinate
	 * @param y1 the ending y coordinate
	 * @param DRAW_ARROWHEAD true if the arrow head should be drawn
	 * @param label a label to be drawn over the arrow
	 */
	private void drawArrow(Graphics g, int x0, int y0, int x1,int y1, boolean DRAW_ARROWHEAD, String label)
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
	 * Draw this component (called during resizing!)
	 * @param g the graphics context to draw to
	 */
	public void paintComponent(Graphics g)
	{		
		super.paintComponent(g);
	  SET_VERTEX_POSITIONS = true;
	}
	
	/**
   * Draw the graph to the given graphics context.
   * @param g the graphics context to draw to
   */
  public void paint(Graphics g)
  {
  	Iterator<Integer> itr;
    int width = getSize().width;
    int height = getSize().height;
    FontMetrics fm;
    String msg;
    int ascent, descent, msg_width;
    int x, y, x0, y0, x1, y1, index;
    boolean BLANK = true;
    Double tmpd;
        
    //Update background buffer if needed
    if(offscreen == null || width != offscreen.getWidth(null) || height != offscreen.getHeight(null)){ 
      offscreen = createImage(width, height);
      bg = offscreen.getGraphics();
    }
    
    super.paint(bg);
    bg.setColor(Color.white);
    bg.fillRect(0, 0, width, height);
    
    fm = bg.getFontMetrics();
    ascent = fm.getMaxAscent();
    descent= fm.getMaxDescent();
    
    // create list of active edges/vertices based on weights
    Vector<Boolean> weight_active_vertices = new Vector<Boolean>();
    weight_active_vertices.setSize(active_vertices.size());
    Vector<Vector<Boolean>> weight_active_edges = new Vector<Vector<Boolean>>();
    weight_active_edges.setSize(active_edges.size());
    
    for(int i=0; i<vertices.size(); i++) {
    	weight_active_vertices.set(i, false);
    }
    
    for(int i=0; i<edges.size(); i++){
    	Vector<Boolean> inner = new Vector<Boolean>();
    	inner.setSize(edges.get(i).size());
    	weight_active_edges.set(i, inner);
    	
      for(int j=0; j<edges.get(i).size(); j++){
      	if(active_edges.get(i).get(j) && (!ENABLE_WEIGHTED_PATHS || iograph.showEdge(i, j))){
      		inner.set(j, true);
      		weight_active_vertices.set(i, true);
      		weight_active_vertices.set(edges.get(i).get(j), true);
      	} else {
      		inner.set(j, false);
      	}
      }
    }
    
    if(SET_VERTEX_POSITIONS){
        //arrangePointsInRings(vertices, active_vertices, width, height, rings, theta + theta_offset);
    	arrangePointsInRings(vertices, weight_active_vertices, width, height, rings, theta + theta_offset);
    	SET_VERTEX_POSITIONS = false;
    }
    
    //Draw edges
    bg.setColor(new Color(edge_color));
    ((Graphics2D)bg).setStroke(thickness2_stroke);

    for(int i=0; i<edges.size(); i++){
      for(int j=0; j<edges.get(i).size(); j++){
      	if(edge_colors != null){
      		bg.setColor(edge_colors.get(i).get(j));
      	}
      	
      	//if(active_edges.get(i).get(j)){
      	if(weight_active_edges.get(i).get(j)){
	        x0 = vertices.get(i).x;
	        y0 = vertices.get(i).y;
	        x1 = vertices.get(edges.get(i).get(j)).x;
	        y1 = vertices.get(edges.get(i).get(j)).y;
	        
	        drawArrow(bg, x0, y0, x1, y1, true);
      	}
      }
    }
    
    //Draw range spanning tree
    if(VIEW_RANGE && (source >= 0)){
    	((Graphics2D)bg).setStroke(thickness4_stroke);
      bg.setColor(new Color(0x00c0c0c0));
      
      for(int i=0; i<paths.size(); i++){
        if(paths.get(i) >= 0){
          x0 = vertices.get(paths.get(i)).x;
          y0 = vertices.get(paths.get(i)).y;
          x1 = vertices.get(i).x;
          y1 = vertices.get(i).y;
          
          drawArrow(bg, x0, y0, x1, y1, true);
        }
      }
      
      //Indicate adjacent edges
      bg.setColor(new Color(0x00a0a0a0));
      
      for(int i=0; i<paths.size(); i++){
        if(paths.get(i) == source && i != source){
          x0 = vertices.get(paths.get(i)).x;
          y0 = vertices.get(paths.get(i)).y;
          x1 = vertices.get(i).x;
          y1 = vertices.get(i).y;
          
          drawArrow(bg, x0, y0, x1, y1, true);
        }
      }
    }
    
    //Draw vertices
    for(int i=0; i<vertices.size(); i++){
    	//if(active_vertices.get(i)){
    	if(weight_active_vertices.get(i)){
    		BLANK = false;
    		
	      if(i == source){
	        bg.setColor(Color.red);
	      }else if(i == target){
	        bg.setColor(Color.blue);
	      }else if(working_set.contains(i)){
	        bg.setColor(new Color(0xa000a0));
	      }else{
	        bg.setColor(new Color(vertex_color));
	      }
	      
	      x = vertices.get(i).x;
	      y = vertices.get(i).y;
	      msg_width = fm.stringWidth(vertices.get(i).text);
	      bg.drawString(vertices.get(i).text, x-msg_width/2, y-descent/2+ascent/2);
    	}
    }
    
    //Draw domain
    if(VIEW_DOMAIN && (target >= 0)){
      bg.setColor(Color.lightGray);
      
    	for(int i=0; i<vertices.size(); i++){
	      x = vertices.get(i).x;
	      y = vertices.get(i).y;
	      msg_width = fm.stringWidth(vertices.get(i).text);
	      bg.drawString(vertices.get(i).text, x-msg_width/2, y-descent/2+ascent/2);
	    }
    	
      bg.setColor(Color.black);
      itr = domain.iterator();
      
      while(itr.hasNext()){
      	index = itr.next();
        x = vertices.get(index).x;
        y = vertices.get(index).y;
        msg_width = fm.stringWidth(vertices.get(index).text);
        bg.drawString(vertices.get(index).text, x-msg_width/2, y-descent/2+ascent/2);
      }
    }
    
    //Draw path
    ((Graphics2D)bg).setStroke(thickness4_stroke);
    bg.setColor(new Color(0x00c3a3bd));
    
    for(int i=0; i<highlighted_path.size()-1; i++){
      x0 = vertices.get(highlighted_path.get(i)).x;
      y0 = vertices.get(highlighted_path.get(i)).y;
      x1 = vertices.get(highlighted_path.get(i+1)).x;
      y1 = vertices.get(highlighted_path.get(i+1)).y;
      
      if(VIEW_EDGE_QUALITY){
      	tmpd = highlighted_path_quality.get(i);
      	
      	if(tmpd != null){
      		drawArrow(bg, x0, y0, x1, y1, i==(highlighted_path.size()-2), Utility.round(tmpd, 2));
      	}else{
        	drawArrow(bg, x0, y0, x1, y1, i==(highlighted_path.size()-2));
      	}
      }else{
      	drawArrow(bg, x0, y0, x1, y1, i==(highlighted_path.size()-2));
      }
    }
    
    ((Graphics2D)bg).setStroke(thickness1_stroke);
    
    //Draw selected edges
    if(highlighted_edge_v0 >= 0 && highlighted_edge_v1 >= 0){
	    ((Graphics2D)bg).setStroke(thickness4_stroke);
	    bg.setColor(new Color(0x00fff38f));
	    
	    x0 = vertices.get(highlighted_edge_v0).x;
	    y0 = vertices.get(highlighted_edge_v0).y;
	    x1 = vertices.get(highlighted_edge_v1).x;
	    y1 = vertices.get(highlighted_edge_v1).y;
	    
	    if(VIEW_EDGE_QUALITY){
	    	tmpd = iograph.getMaxEdgeWeight(highlighted_edge_v0, highlighted_edge_v1);
	    	
	    	if(tmpd != null){
	    		drawArrow(bg, x0, y0, x1, y1, true, Utility.round(tmpd , 2));
	    	}else{
		    	drawArrow(bg, x0, y0, x1, y1, true);
	    	}
	    }else{
	    	drawArrow(bg, x0, y0, x1, y1, true);
	    }
	
	    ((Graphics2D)bg).setStroke(thickness1_stroke);
    }
    
    //Redraw converted path points
    bg.setColor(Color.black); 
    
    for(int i=0; i<highlighted_path.size(); i++){
      x = vertices.get(highlighted_path.get(i)).x;
      y = vertices.get(highlighted_path.get(i)).y;
      msg_width = fm.stringWidth(vertices.get(highlighted_path.get(i)).text);
      bg.drawString(vertices.get(highlighted_path.get(i)).text, x-msg_width/2, y-descent/2+ascent/2);
    }
    
    //Redraw path covered end points
    if(source >= 0){
      x = vertices.get(source).x;
      y = vertices.get(source).y;
      msg_width = fm.stringWidth(vertices.get(source).text);
      bg.setColor(Color.white);
      bg.drawString(vertices.get(source).text, x-msg_width/2-1, y-descent/2+ascent/2+1);
      bg.setColor(new Color(0x00cf0000));
      bg.drawString(vertices.get(source).text, x-msg_width/2, y-descent/2+ascent/2);
    }
    
    if(target >= 0){
      x = vertices.get(target).x;
      y = vertices.get(target).y;
      msg_width = fm.stringWidth(vertices.get(target).text);
    	bg.setColor(Color.white);      
      bg.drawString(vertices.get(target).text, x-msg_width/2-1, y-descent/2+ascent/2+1);
    	bg.setColor(new Color(0x00586cff));      
      bg.drawString(vertices.get(target).text, x-msg_width/2, y-descent/2+ascent/2);
    }
    
    if(highlighted_edge_v1 >= 0){
      x = vertices.get(highlighted_edge_v1).x;
      y = vertices.get(highlighted_edge_v1).y;
      msg_width = fm.stringWidth(vertices.get(highlighted_edge_v1).text);
      bg.setColor(Color.black);      
      bg.drawString(vertices.get(highlighted_edge_v1).text, x-msg_width/2, y-descent/2+ascent/2);
    }
    
    //Message if no active edges
    if(BLANK){
    	x = width/2;
    	y = height/2;
    	msg = "Select " + edges_alias + " to consider in the list to the left.";
      msg_width = fm.stringWidth(msg);
    	bg.setColor(Color.black);
      bg.drawString(msg, x-msg_width/2, y-descent/2+ascent/2);
    }
    
    //Set output message
    output_panel.setText(output_string);
    
    //Draw background  buffer
    g.drawImage(offscreen, 0, 0, this);
  }
  
  /**
	 * The action listener used to handle menu events.
	 * @param e the action event
	 */
	public void actionPerformed(ActionEvent e)
	{
	  JMenuItem event_source = (JMenuItem)e.getSource();
	  
	  if(event_source == menuitem_SET_SOURCE || event_source == menuitem_SET_TARGET || event_source == menuitem_WORKINGSET_ADD || event_source == menuitem_WORKINGSET_REMOVE){
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
	  
	    if(event_source == menuitem_SET_SOURCE || event_source == menuitem_SET_TARGET){
	      if(event_source == menuitem_SET_SOURCE){
	        source = mini;
	        
	        if(!ENABLE_WEIGHTED_PATHS){
	        	paths = iograph.getShortestPaths(source);
	        }else{
	        	//paths = iograph.getShortestWeightedPaths(source).first;
	        	paths = iograph.dijkstra(source).first;
	        }
	      }else if(event_source == menuitem_SET_TARGET){
	        target = mini;
	        //domain = iograph.getDomain(target);
	      }
	      
	      computePath();
	    }else if(event_source == menuitem_WORKINGSET_ADD || event_source == menuitem_WORKINGSET_REMOVE){
	      if(event_source == menuitem_WORKINGSET_ADD){
	        working_set.add(mini);
	        
	        if(working_set.size() == 1){
	          working_set_range_intersection = iograph.getRange(mini);
	        }else{
	          working_set_range_intersection.retainAll(iograph.getRange(mini));
	        }
	      }else if(event_source == menuitem_WORKINGSET_REMOVE){
	        working_set.remove(mini);
	        working_set_range_intersection = iograph.getRangeIntersection(working_set);
	      }
	      
	      //Print out output set
	      Object[] arr = (Object[])working_set_range_intersection.toArray();
	      output_string = "Intersection: ";
	      
	      for(int i=0; i<arr.length; i++){
	        if(i > 0) output_string += ", ";
	        output_string += vertices.get((Integer)arr[i]).text;  
	      }
	      
	      output_panel.alignCenter(false);
	    }
	  }else if(event_source == menuitem_VIEW_RANGE){
	    VIEW_RANGE = !VIEW_RANGE;
	    menuitem_VIEW_RANGE.setState(VIEW_RANGE);
	  }else if(event_source == menuitem_VIEW_DOMAIN){
	    VIEW_DOMAIN = !VIEW_DOMAIN;
	    menuitem_VIEW_DOMAIN.setState(VIEW_DOMAIN);
	  }
	
	  repaint();
	}
	
	public void computePath() {
    if((source >= 0) && (target >= 0)){
      highlighted_path = IOGraph.getPath(paths, source, target);
      highlighted_path_quality = iograph.getMaxPathWeights(highlighted_path);
      output_string = iograph.getPathString(highlighted_path);
        
      if(!highlighted_path.isEmpty()){
      	output_string = "\n" + output_string;
      }else{
        output_string = "\nNo path exists!";
      }
      
      output_panel.alignCenter(true);
    }
	  repaint();
	}

	/**
   * The tree selection listener used to handle selections in the side tree.
   * @param e the tree selection event
   */
  public void valueChanged(TreeSelectionEvent e)
  {
    TreePath[] tree_paths = e.getPaths();
    String name;

    for(int i=0; i<tree_paths.length; i++){
      if(tree_paths[i].getPathCount() > 1){
        name = tree_paths[i].getPathComponent(1).toString();
        
        if(selected_edges.contains(name)){
          selected_edges.remove(name);
        }else{
          selected_edges.add(name);
        }
      }
    }
    
    //Reset state
    source = -1;
    target = -1;
    highlighted_path.clear();
    if(paths != null) paths.clear();
    highlighted_edge_v0 = -1;
    highlighted_edge_v1 = -1;
    working_set.clear();
    working_set_range_intersection.clear();
    output_string = "";
    
    //Update edges
    if(selected_edges.contains("ALL")){	
  		active_edges = iograph.setActiveEdges(true);
    }else{
    	active_edges = iograph.setActiveEdges(selected_edges);
    }
    
    active_vertices = iograph.getActiveVertices();
    SET_VERTEX_POSITIONS = true;
    repaint();
  }
  
  /**
   * The mouse pressed listener used to open the popup menu.
   * @param e the mouse event
   */
  public void mousePressed(MouseEvent e)
  {
  	clicked_x = e.getX();
  	clicked_y = e.getY();  	
  	
    if(e.getButton() == MouseEvent.BUTTON1){
    	clicked_button = 1;
    	
    	if(source >= 0 && VIEW_RANGE){
    		highlightAdjacentSourceEdge(e.getX(), e.getY());
    	}
    }else if(e.getButton() == MouseEvent.BUTTON3){
    	clicked_button = 3;
      popup_menu.show(e.getComponent(), e.getX(), e.getY());
    }
  }
  
  /**
   * The mouse dragged listener used to select edges.
   * @param e the mouse event
   */
  public void mouseDragged(MouseEvent e)
  {  	
  	if(clicked_button == 1){
	  	if(source >= 0 && VIEW_RANGE){
	  		highlightAdjacentSourceEdge(e.getX(), e.getY());
	  	}else{
	  		theta_offset = 0.25 * 360.0 * (e.getY() - clicked_y) / (double)getHeight();
	  		SET_VERTEX_POSITIONS = true;
	  		repaint();
	  	}
  	}
  }
  
  /**
   * The mouse released listener used to de-select edges.
   * @param e the mouse event
   */
  public void mouseReleased(MouseEvent e)
  {
    if(e.getButton() == MouseEvent.BUTTON1){
    	if(source >= 0 && VIEW_RANGE){
	      highlighted_edge_v0 = -1;
	      highlighted_edge_v1 = -1;
	      repaint();
    	}else{
    		theta += theta_offset;
    		theta_offset = 0;
    		repaint();
    	}
    }
    
    clicked_button = 0;
  }

  /**
	 * Handle mouse wheel events.
	 * @param e a mouse wheel event
	 */
	public void mouseWheelMoved(MouseWheelEvent e)
	{		
		if(e.getWheelRotation() < 0){
			rings++;
		}else{
			if(rings > 1) rings--;
		}
		
		SET_VERTEX_POSITIONS = true;
		repaint();
	}

	public void mouseExited(MouseEvent e) {}
  public void mouseEntered(MouseEvent e) {}
  public void mouseClicked(MouseEvent e) {}
  public void mouseMoved(MouseEvent e) {}
	
  /**
	 * Set vertex positions to form a circular graph.
	 * @param vertices the vertices
	 * @param active_vertices a list of currently active vertices
	 * @param width the width of the panel graph will be displayed in
	 * @param height the height of the panel graph will be displayed in
	 * @param rings the number of rings
	 * @param theta0 the initial offset angle to start placing vertices (in degrees)
	 */
	public static void arrangePointsInRings(Vector<Point2D> vertices, Vector<Boolean> active_vertices, int width, int height, int rings, double theta0)
	{
		Vector<Point2D> sorted_vertices = new Vector<Point2D>();
		int half_width = width / 2;
		int half_height = height / 2;
		double radius;
		
		for(int i=0; i<active_vertices.size(); i++){
			//Set all vertices to an invalid position
			vertices.get(i).x = -1;
			vertices.get(i).y = -1;
			
			//Consider only active vertices
			if(active_vertices.get(i)){
				sorted_vertices.add(vertices.get(i));
			}
		}
		
		//Sort vertices and place on a circular graph
		Collections.sort(sorted_vertices);
		theta0 = Math.PI * theta0 / 180.0;
		
		for(int i=0; i<sorted_vertices.size(); i++){
			radius = Math.pow(0.9, (i%rings)+1);
			sorted_vertices.get(i).x = (int)Math.round(Math.cos((2.0*Math.PI*i)/sorted_vertices.size()+theta0)*radius*half_width + half_width);
			sorted_vertices.get(i).y = (int)Math.round(Math.sin((2.0*Math.PI*i)/sorted_vertices.size()+theta0)*radius*half_height + half_height);
		}
	}

	/**
   * The main starting point for this program.
   * @param args command line arguments
   */
  public static void main(String args[])
  {
  	IOGraph iograph = null;
  	IOGraphPanel iograph_panel = null;
		String server;
		int port, tmpi;  
		
		//args = new String[]{"-file", "data/weights.txt"};
		
  	if(args.length > 0){
  		if(args[0].equals("-file")){
  			iograph = new IOGraph<String,String>();
  			iograph.load(args[1]);
  			iograph.loadEdgeWeights(args[1], 0.0);
  			iograph.printEdgeInformation();
  			
  			iograph_panel = new IOGraphPanel<String,String>(iograph, 2);
      	iograph_panel.setViewEdgeQuality(true);
  		}else{
	  		tmpi = args[0].lastIndexOf(':');
	  		
	  		if(tmpi != -1){
	  			server = args[0].substring(0, tmpi);
	  			port = Integer.valueOf(args[0].substring(tmpi+1));
	  			
		  		PolyglotClient polyglot = new PolyglotClient(server, port);
		  		iograph_panel = new IOGraphPanel<String,String>(polyglot.getInputOutputGraph(), 2);
		  		polyglot.close();
	  		}
  		}
  	}else{		//Debugging
	  	if(true){
	  		PolyglotClient polyglot = new PolyglotClient("localhost", 32);
	  		iograph_panel = new IOGraphPanel<String,String>(polyglot.getInputOutputGraph(), 2);
	  		polyglot.close();
	  	}else if(false){
		  	SoftwareServerClient icr = new SoftwareServerClient("localhost", 30);
		  	iograph = new IOGraph<Data,Application>(icr);
		  	icr.close();
		  	
	    	iograph_panel = new IOGraphPanel<Data,Application>(iograph, 2);
	  	}else if(false){
	    	iograph = new IOGraph<String,String>("jdbc:mysql://isda.ncsa.uiuc.edu/csr", "demo", "demo");
	    	iograph_panel = new IOGraphPanel<String,String>(iograph, 2);
	  	}else{
	    	iograph = new IOGraph<String,String>("http://isda.ncsa.uiuc.edu/NARA/CSR/get_conversions.php");
	    	iograph_panel = new IOGraphPanel<String,String>(iograph, 2);
	  	}
  	}
 
    JFrame frame = new JFrame("IOGraph Viewer");
    frame.add(iograph_panel.getAuxiliaryInterfacePane());
    frame.pack();
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 
    frame.setVisible(true);
  }
}
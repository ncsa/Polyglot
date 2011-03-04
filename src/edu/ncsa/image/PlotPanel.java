package edu.ncsa.image;
import edu.ncsa.utility.*;
import java.awt.*;
import javax.swing.*;
import java.util.*;

/**
 * A panel to display a plot.
 * @author Kenton McHenry
 */
public class PlotPanel extends JPanel
{
	private double x_min = 0;
  private double x_max = 20;
  private double y_min = 0;
  private double y_max = 100;
  private int x_intervals = 20;
  private int y_intervals = 20;
  private int x_tick_intervals = 10;
  private int y_tick_intervals = 10;
  private Vector<Vector<Double>> x = new Vector<Vector<Double>>();
  private Vector<Vector<Double>> y = new Vector<Vector<Double>>();
  private Vector<Color> colors = new Vector<Color>();

  private JPanel draw_panel;
  private int draw_panel_offx = 45;
  private int draw_panel_offy = 20;
  private int draw_panel_width;
  private int draw_panel_height;

  private boolean DRAW_LINES = true;
  private boolean DRAW_POINTS = false;
  
  /**
   * Class constructor.
   * @param width the desired width of the panel
   * @param height the desired height of the panel
   */
  public PlotPanel(int width, int height)
  { 
  	//Allocate one plot line
  	x.add(new Vector<Double>());
  	y.add(new Vector<Double>());
  	colors.add(Color.blue);
  	
  	//Setup the panel
  	setBackground(Color.white);
    setPreferredSize(new Dimension(width, height));
    setLayout(null);

    draw_panel = new JPanel();
    draw_panel.setBackground(Color.white);
    draw_panel.setLocation(draw_panel_offx, draw_panel_offy);
    draw_panel_width = width - draw_panel_offx - 15;
    draw_panel_height = height - draw_panel_offy - 30;
    draw_panel.setSize(draw_panel_width, draw_panel_height);
    add(draw_panel);
  }
  
  /**
	 * Clear the plot.
	 */
	public void clear()
	{  	
		for(int i=0; i<x.size(); i++){
			x.get(i).clear();
			y.get(i).clear();
		}
	}

	/**
	 * Set axis limits.
	 * @param x_min x minimum
	 * @param x_max x maximum
	 * @param y_min y minimum
	 * @param y_max y maximum
	 */
	public void setAxis(double x_min, double x_max, double y_min, double y_max)
	{
		this.x_min = x_min;
		this.x_max = x_max;
		this.y_min = y_min;
		this.y_max = y_max;
	}

	/**
	 * Set whether or not to draw the plot lines.
	 * @param value true if plot lines should be drawn
	 */
	public void setDrawLines(boolean value)
	{
		DRAW_LINES = value;
		repaint();
	}
	
	/**
	 * Set whether or not to draw the plot points.
	 * @param value true if plot points should be drawn
	 */
	public void setDrawPoints(boolean value)
	{
		DRAW_POINTS = value;
		repaint();
	}
	
	/**
	 * Add a point to the plot.
	 * @param x the x-coordinate of the point to add
	 * @param y the y-coordinate of the point to add
	 */
	public void add(double x, double y)
	{
		this.x.get(0).add(x);
		this.y.get(0).add(y);
		repaint();
	}
	
	/**
	 * Add a point to the plot.
	 * @param i the plot line to add to
	 * @param x the x-coordinate of the point to add
	 * @param y the y-coordinate of the point to add
	 * @param color the color of this plot line
	 */
	public void add(int i, double x, double y, Color color)
	{
		//Ensure there is enough space
		while(this.x.size() <= i){
			this.x.add(new Vector<Double>());
			this.y.add(new Vector<Double>());
			colors.add(Color.gray);
		}
		
		this.x.get(i).add(x);
		this.y.get(i).add(y);
		colors.set(i, color);
		repaint();
	}

	/**
   * Draw the plot to the given graphics context.
   * @param g the graphics context to draw to
   */
  public void paint(Graphics g)
  {
    super.paint(g);
        
    String msg;
    FontMetrics fm = g.getFontMetrics();
    int ascent = fm.getMaxAscent();
    int descent= fm.getMaxDescent();
    int msg_width;
    int u1, v1, u2, v2;
    double value, delta, increment;
         
    //Draw horizontal grid lines
    value = y_min;
    delta = (y_max-y_min) / y_intervals;
    increment = ((double)draw_panel_height)/y_intervals;
    
    for(double v=draw_panel_height; v>=0; v-=increment){
      u1 = draw_panel_offx;
      v1 = draw_panel_offy + (int)Math.round(v);
      u2 = draw_panel_offx + draw_panel_width;
      v2 = v1;
      
      g.setColor(Color.lightGray);
      g.drawLine(u1, v1, u2, v2);
      
      value += delta;
    }
    
    //Draw horizontal ticks
    value = y_min;
    delta = (y_max-y_min) / y_tick_intervals;
    increment = ((double)draw_panel_height)/y_tick_intervals;

    for(double v=draw_panel_height; v>=0; v-=increment){
      u1 = draw_panel_offx;
      v1 = draw_panel_offy + (int)Math.round(v);
      u2 = draw_panel_offx + draw_panel_width;
      v2 = v1;
      
      msg = Utility.round(value, 2);
      msg_width = fm.stringWidth(msg);
      g.setColor(Color.black);
      g.drawString(msg, u1-msg_width/2-20, v1-descent/2+ascent/2);
      
      value += delta;
    }
    
    //Draw vertical grid lines
    value = x_min;
    delta = (x_max-x_min) / x_intervals;
    increment = ((double)draw_panel_width)/x_intervals;

    for(double u=0; u<=draw_panel_width; u+=increment){
      u1 = draw_panel_offx + (int)Math.abs(u);
      v1 = draw_panel_offy;
      u2 = u1;
      v2 = draw_panel_offy + draw_panel_height;
      
      g.setColor(Color.lightGray);
      g.drawLine(u1, v1, u2, v2);
      
      value += delta;
    }
    
    //Draw vertical ticks
    value = x_min;
    delta = (x_max-x_min) / x_tick_intervals;
    increment = ((double)draw_panel_width)/x_tick_intervals;

    for(double u=0; u<=draw_panel_width; u+=increment){
      u1 = draw_panel_offx + (int)Math.abs(u);
      v1 = draw_panel_offy;
      u2 = u1;
      v2 = draw_panel_offy + draw_panel_height;
      
      msg = Utility.round(value, 2);
      msg_width = fm.stringWidth(msg);
      g.setColor(Color.black);
      g.drawString(msg, u1-msg_width/2, v2-descent/2+ascent/2+10);
      
      value += delta;
    }
    
    //Draw x-axis
    u1 = draw_panel_offx;
    v1 = draw_panel_offy + draw_panel_height;
    if(y_min < 0) v1 += (int)Math.round(draw_panel_height * Math.abs(y_min)/(y_max-y_min));
    u2 = draw_panel_offx + draw_panel_width;
    v2 = v1;
    
    g.setColor(Color.black);
    g.drawLine(u1, v1, u2, v2);
    
    //Draw y-axis
    u1 = draw_panel_offx;
    if(x_min < 0) u1 += (int)Math.round(draw_panel_width * Math.abs(x_min)/(x_max-x_min));
    v1 = draw_panel_offy;
    u2 = u1;
    v2 = draw_panel_offy + draw_panel_height;
    
    g.setColor(Color.black);
    g.drawLine(u1, v1, u2, v2);
    
    //Draw lines 
    if(DRAW_LINES){
    	Graphics2D g2 = (Graphics2D)g;
    		    	
    	g2.setStroke(new BasicStroke(3));

	    for(int i=x.size()-1; i>=0; i--){
	    	g2.setColor(colors.get(i));
	
		    for(int j=1; j<x.get(i).size(); j++){
		      u1 = (int)Math.round(((x.get(i).get(j-1)-x_min)/(x_max-x_min)) * draw_panel_width) + draw_panel_offx;
		      v1 = (int)Math.round(((y.get(i).get(j-1)-y_min)/(y_max-y_min)) * draw_panel_height);
		      v1 = draw_panel_height - v1 + draw_panel_offy;
		      u2 = (int)Math.round(((x.get(i).get(j)-x_min)/(x_max-x_min)) * draw_panel_width) + draw_panel_offx;
		      v2 = (int)Math.round(((y.get(i).get(j)-y_min)/(y_max-y_min)) * draw_panel_height);
		      v2 = draw_panel_height - v2 + draw_panel_offy;
		      g2.drawLine(u1, v1, u2, v2);
		    }
	    }
    }
    
    //Draw points
    if(DRAW_POINTS){
    	Graphics2D g2 = (Graphics2D)g;
    	
    	g2.setStroke(new BasicStroke(5));

	    for(int i=x.size()-1; i>=0; i--){
	    	g2.setColor(colors.get(i));
		    
		    for(int j=0; j<x.get(i).size(); j++){
		      u1 = (int)Math.round(((x.get(i).get(j)-x_min)/(x_max-x_min)) * draw_panel_width) + draw_panel_offx;
		      v1 = (int)Math.round(((y.get(i).get(j)-y_min)/(y_max-y_min)) * draw_panel_height);
		      v1 = draw_panel_height - v1 + draw_panel_offy;
		      g2.drawLine(u1, v1, u1, v1);
		    }
	    }
    }
  }

  /**
   * Create and show a plot within a JFrame.
   * @param width the width of the plot panel
   * @param height the height of the plot panel
   * @param title the title of the frame
   * @return the created plot panel
   */
  public static PlotPanel framedPlotPanel(int width, int height, String title)
  {
		PlotPanel panel = new PlotPanel(width, height);
		
		JFrame frame = new JFrame();
		frame.add(panel); 
		frame.setTitle(title);
		frame.pack();
	  frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	  frame.setVisible(true); 
	  
	  return panel;
  }
  
	/**
	 * A main to run the panel by itself.
	 * @param args command line arguments
	 */
	public static void main(String args[])
	{
		PlotPanel panel = framedPlotPanel(600, 300, "Plot");
		panel.setAxis(1, 50, 0, 100);
		panel.setDrawPoints(true);
		
		for(int i=1; i<=50; i++){
			panel.add(0, i, 100*Math.exp(-Math.pow(i-25, 2)/25), Color.blue);
			panel.add(1, i, 100*Math.exp(-Math.pow(i-25, 2)/100), Color.gray);
			Utility.pause(20);
		}
	}
}
package edu.ncsa.icr;
import edu.ncsa.image.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.*;
import javax.swing.*;

/**
 * A program for creating AHK scripts (monkey see, monkey do)
 * @author Kenton McHenry
 */
public class ICRMonkey_DS extends JPanel implements MouseListener, MouseMotionListener, ActionListener, Runnable
{
	private BufferedImage desktop;
	private Robot robot;
	private double panel_scale;
	private int panel_width;
	private int panel_height;
	
	private JPopupMenu popup_menu;
	
	private Vector<int[]> positives = new Vector<int[]>();
	private Vector<int[]> negatives = new Vector<int[]>();
  private int selection_box_x0, selection_box_y0, selection_box_x1, selection_box_y1;
  private int selection_box_minx, selection_box_miny, selection_box_maxx, selection_box_maxy;
  private boolean DRAGGING_SELECTION_BOX;
  private boolean GET_POSITIVE_AREA;
  private boolean GET_NEGATIVE_AREA;
  
	/**
	 * Class constructor.
	 * @param panel_scale the scale of the virtual desktop
	 */
	public ICRMonkey_DS(double panel_scale)
	{    
		JMenu submenu1;    	
    JMenuItem item;
		Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
		
		this.panel_scale = panel_scale;
		panel_width = (int)Math.round(panel_scale * dimension.getWidth());
		panel_height = (int)Math.round(panel_scale * dimension.getHeight());
		setPreferredSize(new Dimension(panel_width, panel_height));

		try{
			robot = new Robot();
		}catch(Exception e) {e.printStackTrace();}
    
    //Set up popup menu
    popup_menu = new JPopupMenu();
    
    submenu1 = new JMenu("New Script");
    item = new JMenuItem("Open"); item.addActionListener(this); submenu1.add(item); 
    item = new JMenuItem("Save"); item.addActionListener(this); submenu1.add(item); 
    item = new JMenuItem("Import"); item.addActionListener(this); submenu1.add(item); 
    item = new JMenuItem("Export"); item.addActionListener(this); submenu1.add(item); 
    item = new JMenuItem("Convert"); item.addActionListener(this); submenu1.add(item); 
    item = new JMenuItem("Exit"); item.addActionListener(this); submenu1.add(item); 
    popup_menu.add(submenu1);

    popup_menu.addSeparator();
    submenu1 = new JMenu("Enter");
    item = new JMenuItem("file1.abc"); item.addActionListener(this); submenu1.add(item);
    popup_menu.add(submenu1);
    
    submenu1 = new JMenu("Select");
    item = new JMenuItem("Positive Area"); item.addActionListener(this); submenu1.add(item);
    item = new JMenuItem("Negative Area"); item.addActionListener(this); submenu1.add(item);
    popup_menu.add(submenu1);
    
    popup_menu.addSeparator();
    item = new JMenuItem("End Script"); item.addActionListener(this); popup_menu.add(item);
    
    addMouseListener(this);
    addMouseMotionListener(this);
    
		new Thread(this).start();
	}
	
	/**
	 * Draw a box to the given graphics context.
	 * @param g the graphics context to draw to
	 * @param minx the minimum x value
	 * @param miny the minimum y value
	 * @param maxx the maximum x value
	 * @param maxy the maximum y value
	 * @param color the color of the box
	 * @param fill true if the box should be filled
	 */
	private void drawBox(Graphics g, int minx, int miny, int maxx, int maxy, Color color, boolean FILL)
	{
		Graphics2D g2 = (Graphics2D)g;
		
		g2.setColor(color);
		
	  g2.drawLine(minx, miny, maxx, miny);		//Top
	  g2.drawLine(minx, maxy, maxx, maxy);		//Bottom
	  g2.drawLine(minx, miny, minx, maxy);		//Left
	  g2.drawLine(maxx, miny, maxx, maxy);		//Right
	  	  
	  if(FILL){
			g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
		  g2.fillRect(minx+1, miny+1, maxx-minx-1, maxy-miny-1);
			g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
	  }
	}
	
	/**
	 * Draw a box to the given graphics context.
	 * @param g the graphics context to draw to
	 * @param box an array containing the box coordinates (minx, miny, maxx, maxy)
	 * @param color the color of the box
	 * @param fill true if the box should be filled
	 */
	private void drawBox(Graphics g, int[] box, Color color, boolean FILL)
	{
		drawBox(g, box[0], box[1], box[2], box[3], color, FILL);
	}
	
	/**
	 * Draw this panel to the given graphic context.
	 * @param g the graphics context to draw to
	 */
	public void paint(Graphics g)
	{
		synchronized(this){
			g.drawImage(desktop, 0, 0, null);
		}
		
    //Draw selection box
    if(DRAGGING_SELECTION_BOX){
    	if(GET_NEGATIVE_AREA){
    		drawBox(g, selection_box_minx, selection_box_miny, selection_box_maxx, selection_box_maxy, Color.red, false);
    	}else{
    		drawBox(g, selection_box_minx, selection_box_miny, selection_box_maxx, selection_box_maxy, Color.blue, false);
    	}
    }
    
    //Draw positive areas
    for(int i=0; i<positives.size(); i++){
    	drawBox(g, positives.get(i), Color.blue, true);
    }
    
    //Draw negative areas
    for(int i=0; i<negatives.size(); i++){
    	drawBox(g, negatives.get(i), Color.red, true);
    }
	}
	
	/**
	 * Handle action events.
	 * @param e an action event
	 */
	public void actionPerformed(ActionEvent e)
	{
		Object object = e.getSource();
		JMenuItem menuitem;
		
		if(object instanceof JMenuItem){
			menuitem = (JMenuItem)object;
			
			if(menuitem.getText().equals("Positive Area")){
				GET_POSITIVE_AREA = true;
			}else if(menuitem.getText().equals("Negative Area")){
				GET_NEGATIVE_AREA = true;
			}
		}
	}

	/**
   * Handle mouse pressed events.
   * @param e the mouse event
   */
  public void mousePressed(MouseEvent e)
  {
  	if(e.getButton() == 1){
  		if(GET_POSITIVE_AREA || GET_NEGATIVE_AREA){
	    	selection_box_x0 = e.getX();
	    	selection_box_y0 = e.getY();
	    	DRAGGING_SELECTION_BOX = true;
	    	repaint();
  		}
  	}else if(e.getButton() == 3){		//Show popup menu with conversion options
  		popup_menu.show(e.getComponent(), e.getX(), e.getY());
		}
  }
  
  /**
   * Handle mouse motion events.
   * @param e the mouse event
   */
  public void mouseDragged(MouseEvent e)
  {
  	if(DRAGGING_SELECTION_BOX){
	  	selection_box_x1 = e.getX();
	  	selection_box_y1 = e.getY();
	  	
	  	//Set selection box
	  	if(selection_box_x0 < selection_box_x1){
	  		selection_box_minx = selection_box_x0;
	  		selection_box_maxx = selection_box_x1;
	  	}else{
	  		selection_box_minx = selection_box_x1;
	  		selection_box_maxx = selection_box_x0;
	  	}
	  	
	  	if(selection_box_y0 < selection_box_y1){
	  		selection_box_miny = selection_box_y0;
	  		selection_box_maxy = selection_box_y1;
	  	}else{
	  		selection_box_miny = selection_box_y1;
	  		selection_box_maxy = selection_box_y0;
	  	}
	  	
	  	repaint();
  	}
  }

  /**
   * Handle mouse release events.
   * @param e the mouse event
   */
	public void mouseReleased(MouseEvent e)
	{
		if(DRAGGING_SELECTION_BOX){
			DRAGGING_SELECTION_BOX = false;
			
			if(GET_POSITIVE_AREA){
				positives.add(new int[]{selection_box_minx, selection_box_miny, selection_box_maxx, selection_box_maxy});
				GET_POSITIVE_AREA = false;
			}else if(GET_NEGATIVE_AREA){
				negatives.add(new int[]{selection_box_minx, selection_box_miny, selection_box_maxx, selection_box_maxy});
				GET_NEGATIVE_AREA = false;
			}
			
			repaint();
		}
	}
	
	public void mouseClicked(MouseEvent e) {}	
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mouseMoved(MouseEvent e) {}

	/**
	 * Background thread to capture screen shots and record interaction.
	 */
	public void run()
	{
		double fps = 0;
		long t0 = System.currentTimeMillis();
		long t1;
		Dimension screen_size = Toolkit.getDefaultToolkit().getScreenSize();
		BufferedImage image;
		
		while(true){
	  	image = robot.createScreenCapture(new Rectangle(0,0,(int)screen_size.getWidth(),(int)screen_size.getHeight()));
	  	//image = ImageUtility.resize(image, panel_scale, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
	  	image = ImageUtility.resize(image, panel_scale, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

			synchronized(this){
				desktop = image;
			}

	  	//Display fps
	  	if(false){
	  		fps++;
	  		t1 = System.currentTimeMillis();
	  		
	  		if(t1-t0 > 1000){
	  			System.out.println("FPS: " + fps);
	  			
	  			fps = 0;
	  			t0 = System.currentTimeMillis();
	  		}
	  	}
	  	
	  	repaint();
		}
	}

	/**
	 * Start the ICRMonkey.
	 * @param args command line arguments
	 */
	public static void main(String args[])
	{
		int screens = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices().length;
		
		if(screens > 1){
			ICRMonkey_DS icr_monkey = new ICRMonkey_DS(0.75);
			Dimension screen_size = Toolkit.getDefaultToolkit().getScreenSize();

	    JFrame frame = new JFrame("ICR Monkey Desktop");
	    frame.add(icr_monkey);
	    frame.setResizable(false);
	    frame.pack();
	    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	    frame.setLocation((int)screen_size.getWidth()+5, 5);
	    frame.setVisible(true);
		}else{
			System.out.println("Sorry, ICRMonkeyDS requires two screens!");
		}
	}
}
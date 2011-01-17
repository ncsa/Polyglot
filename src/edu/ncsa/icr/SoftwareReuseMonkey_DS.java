package edu.ncsa.icr;
import edu.ncsa.image.*;
import edu.ncsa.utility.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.swing.*;
import java.io.*;
import java.util.*;

/**
 * A program for creating monkey scripts (monkey see, monkey do)
 * @author Kenton McHenry
 */
public class SoftwareReuseMonkey_DS extends JPanel implements ActionListener, MouseListener, MouseMotionListener, KeyListener, Runnable
{
	private String data_path = "./";
	private String output_path = "./";
	private double panel_scale;
	private int ignored_bottom = 0;
	private int ignored_left = 0;
	
	private Dimension screen_size = null;
	private boolean CAPTURING = true;
	private BufferedImage desktop;
	private BufferedImage desktop_scaled;
	private Robot robot;
	private int panel_width;
	private int panel_height;
	private MonkeyScript script = null;
	
	private String[] operations = new String[]{"open", "save", "convert", "exit"};

	private JPopupMenu popup_menu;
	private int popup_menu_x;
	private int popup_menu_y;
	
	private boolean RECORDING_SCRIPT = false;
	private int script_count = 0;
  private int selection_box_x0, selection_box_y0, selection_box_x1, selection_box_y1;
  private int selection_box_minx, selection_box_miny, selection_box_maxx, selection_box_maxy; 
  private boolean DRAGGING_SELECTION_BOX;
  private boolean GET_POSITIVE_AREA;
  private boolean GET_NEGATIVE_AREA;
  private boolean GET_TARGET_AREA; 
  private int[][] target = null;
	private Vector<Point> target_locations = new Vector<Point>();

	/**
	 * Class constructor.
	 * @param filename the name of the *.ini file to load
	 */
	public SoftwareReuseMonkey_DS(String filename)
	{
		Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();

		loadINI(filename);
		
		panel_width = (int)Math.round(panel_scale * dimension.getWidth());
		panel_height = (int)Math.round(panel_scale * dimension.getHeight());
		setPreferredSize(new Dimension(panel_width, panel_height));

		try{
			robot = new Robot();
		}catch(Exception e) {e.printStackTrace();}
    
		setPopupMenu();
    
    addMouseListener(this);
    addMouseMotionListener(this);
    
		new Thread(this).start();
	}
	
	/**
	 * Load an *.ini initialization file.
	 * @param filename the name of the *.ini file
	 */
	private void loadINI(String filename)
	{
	  try{
	    BufferedReader ins = new BufferedReader(new FileReader(filename));
	    String line, key, value;
	    
	    while((line=ins.readLine()) != null){
	      if(line.contains("=")){
	        key = line.substring(0, line.indexOf('='));
	        value = line.substring(line.indexOf('=')+1);
	        
	        if(key.charAt(0) != '#' && key.charAt(0) != ';'){
	        	if(key.equals("DataPath")){
	        		data_path = value + "/";
	        	}else if(key.equals("OutputPath")){
	        		output_path = value + "/";
	        	}else if(key.equals("Scale")){
	        		panel_scale = Double.valueOf(value);
	        	}else if(key.equals("IgnoredBottom")){
	        		ignored_bottom = Integer.valueOf(value);
	        	}else if(key.equals("IgnoredLeft")){
	        		ignored_left = Integer.valueOf(value);
	        	}
	        }
	      }
	    }
	    
	    ins.close();
	  }catch(Exception e) {e.printStackTrace();}
	}
	
	/**
	 * Build the popup menu.
	 */
	private void setPopupMenu()
	{
		JMenu submenu1;
		JMenuItem item;
		
	  popup_menu = new JPopupMenu();
	  
	  if(!RECORDING_SCRIPT){
		  submenu1 = new JMenu("New Script");
		  
		  for(int i=0; i<operations.length; i++){
		  	item = new JMenuItem(operations[i]); item.addActionListener(this); submenu1.add(item); 
		  }
	
		  popup_menu.add(submenu1);
	  }else{
		  item = new JMenuItem("Run"); item.addActionListener(this); popup_menu.add(item);

		  submenu1 = new JMenu("Send");
		  item = new JMenuItem("Click"); item.addActionListener(this); submenu1.add(item);
		  item = new JMenuItem("DoubleClick"); item.addActionListener(this); submenu1.add(item);
		  item = new JMenuItem("Text"); item.addActionListener(this); submenu1.add(item);
		  popup_menu.add(submenu1);
		  
		  submenu1 = new JMenu("Select");
		  item = new JMenuItem("Positive Area"); item.addActionListener(this); submenu1.add(item);
		  item = new JMenuItem("Negative Area"); item.addActionListener(this); submenu1.add(item);
		  submenu1.addSeparator();
		  item = new JMenuItem("Target Area"); item.addActionListener(this); submenu1.add(item);
		  popup_menu.add(submenu1);
		  
		  item = new JMenuItem("Require Current"); item.addActionListener(this); popup_menu.add(item);
		  popup_menu.addSeparator();
		  
		  item = new JMenuItem("End Script"); item.addActionListener(this); popup_menu.add(item);
	  }
	}

	/**
	 * Add ignored areas to the script.
	 */
	private void addIgnoredAreas()
	{
		//Add bottom area
		if(ignored_bottom > 0){
			script.addNegativeArea(new int[]{0, screen_size.height-ignored_bottom, screen_size.width, screen_size.height});
		}
		
		//Add left area
		if(ignored_left > 0){
			script.addNegativeArea(new int[]{0, 0, ignored_left, screen_size.height});
		}
	}
	
	/**
	 * Wait for the screen capture thread to capture a new image.
	 */
	private void waitForNewScreenShot()
	{
		BufferedImage current = desktop;
		
		while(current == desktop){
			Utility.pause(100);
		}
	}

	/**
	 * Perform the specified action.
	 * @param action the action to perform ("Click", "DoubleClick", "Text")
	 * @param x the x-coordinate to perform the action at
	 * @param y the y-coordinate to perform the action at
	 * @param RESTORE_MOUSE_POSITION true if the mouse should be returned to its orignal position
	 */
	private void performAction(String action, int x, int y, boolean RESTORE_MOUSE_POSITION)
	{
		Point xy_last = MouseInfo.getPointerInfo().getLocation();
		String text;
		int key_code;
		
		//Scale action location appropriately
		x /= panel_scale;
		y /= panel_scale;

		if(action.equals("Click")){
			//Record action
			synchronized(this){
				if(target != null && !target_locations.isEmpty()){
					script.addTargetClick(target, target_locations.get(0).x, target_locations.get(0).y, x, y);
				}else{
					script.addClick(desktop, x, y);
				}
			}
	
			//Perform action
			robot.mouseMove(x, y);
			robot.mousePress(InputEvent.BUTTON1_MASK);
			robot.mouseRelease(InputEvent.BUTTON1_MASK);
		}else if(action.equals("DoubleClick")){
			//Record action
			synchronized(this){
				script.addDoubleClick(desktop, x, y);
			}
	
			//Perform action
			robot.mouseMove(x, y);
			robot.mousePress(InputEvent.BUTTON1_MASK);
			robot.mouseRelease(InputEvent.BUTTON1_MASK);
			robot.mousePress(InputEvent.BUTTON1_MASK);
			robot.mouseRelease(InputEvent.BUTTON1_MASK);
		}else if(action.equals("Text")){
			//Get the text
			setCapturing(false);
			text = JOptionPane.showInputDialog(this, "");
			setCapturing(true);
			
			if(text != null){
				//Enforce the click and wait for the next screen shot to ensure focus and correct desktop!
				performAction("Click", x, y, false);
				Utility.pause(1000);
				waitForNewScreenShot();
				
				//Record action
				synchronized(this){
					script.addText(desktop, text);
				}
				
				//Perform action (start by moving mouse focus back over!)
				robot.mouseMove(x, y);
				robot.mousePress(InputEvent.BUTTON1_MASK);
				robot.mouseRelease(InputEvent.BUTTON1_MASK);
				
				for(int i=0; i<text.length(); i++){
					key_code = KeyStroke.getKeyStroke(Character.toUpperCase(text.charAt(i)), 0).getKeyCode();
											
					if(Character.isUpperCase(text.charAt(i))){
						robot.keyPress(KeyEvent.VK_SHIFT);
						robot.keyPress(key_code);
						robot.keyRelease(key_code);
						robot.keyRelease(KeyEvent.VK_SHIFT);
					}else{
						robot.keyPress(key_code);
						robot.keyRelease(key_code);
					}
				}
			}
		}
		
		//Clear target if set
		target = null;
		
		//Add ignored sections again if need be
		if(script.getNegativeAreas().isEmpty()){
			addIgnoredAreas();
		}
		
		//Move mouse back to it's original position
		if(RESTORE_MOUSE_POSITION){
			robot.mouseMove((int)xy_last.getX(), (int)xy_last.getY());
		}
	}
	
	/**
	 * Enable/disable screen shot capturing.
	 * @param value true if screen shots should be captured
	 */
	public void setCapturing(boolean value)
	{
		CAPTURING = value;
	}
	
	/**
	 * Draw this panel to the given graphic context.
	 * @param g the graphics context to draw to
	 */
	public void paint(Graphics g)
	{
		int[][] image = null;
		int[] box;
		int x, y, w, h;
		int minx, miny, maxx, maxy;
		
		synchronized(this){
			g.drawImage(desktop_scaled, 0, 0, null);
			if(target != null) image = ImageUtility.image2argb(desktop);
		}
		
    //Draw selection box
    if(DRAGGING_SELECTION_BOX){
    	ImageUtility.drawBox(g, selection_box_minx, selection_box_miny, selection_box_maxx, selection_box_maxy, new Color(0x003399ff), 0.25f);
    }
    
    if(script != null){
    	if(target == null){
		    //Draw positive areas
		    for(int i=0; i<script.getPositiveAreas().size(); i++){
		    	box = Arrays.copyOf(script.getPositiveAreas().get(i), 4);
		    	
		    	for(int j=0; j<4; j++){
		    		box[j] *= panel_scale;
		    	}
		    	
		    	ImageUtility.drawBox(g, box, Color.white, 0.25f);
		    }
		    
		    //Draw negative areas
		    for(int i=0; i<script.getNegativeAreas().size(); i++){
		    	box = Arrays.copyOf(script.getNegativeAreas().get(i), 4);
		    	
		    	for(int j=0; j<4; j++){
		    		box[j] *= panel_scale;
		    	}
		    	
		    	ImageUtility.drawBox(g, box, Color.darkGray, 0.25f);
		    }
    	}else{
    		//Draw target matches
    		target_locations = ImageUtility.find(image, target, 0, false);
    		
    		for(int i=0; i<target_locations.size(); i++){
    			x = target_locations.get(i).x;
    			y = target_locations.get(i).y;
    			h = target.length;
    			w = target[0].length;
    			
    			minx = (int)Math.round(panel_scale*x);
    			miny = (int)Math.round(panel_scale*y);
    			maxx = (int)Math.round(panel_scale*(x+w-1));
    			maxy = (int)Math.round(panel_scale*(y+h-1));
    			
        	ImageUtility.drawBox(g, minx, miny, maxx, maxy, Color.red, 0.25f);
    		}
    	}
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
		String menuitem_text;
		String command;
		
		if(object instanceof JMenuItem){
			menuitem = (JMenuItem)object;
			menuitem_text = menuitem.getText();
			
	  	setCapturing(true);		//Undo capturing halt from popup menu
			
			if(Utility.contains(operations, menuitem_text)){
				script = new MonkeyScript();
				script.setOperation(menuitem_text);

				do{		//Find the next available number to assign as a script name
					script.setAlias(Utility.toString(script_count, 3));
					script_count++;
				}while(Utility.exists(output_path + script.getName() + ".ms") || Utility.exists(output_path + script.getName()));
				
				addIgnoredAreas();
				RECORDING_SCRIPT = true;
				setPopupMenu();
			}else if(menuitem_text.equals("Run")){
	      JFileChooser fc = new JFileChooser();
	      
	      if(fc.showDialog(this, "Run") == JFileChooser.APPROVE_OPTION){
	        command = Utility.unixPath(fc.getCurrentDirectory().getAbsolutePath()) + "/" + fc.getSelectedFile().getName();
	        script.addCommand(command);
	        
	        try{
	        	Runtime.getRuntime().exec(command);
	        }catch(Exception ex) {ex.printStackTrace();}
	      }
			}else if(menuitem_text.equals("Click") || menuitem_text.equals("DoubleClick") || menuitem_text.equals("Text")){
				performAction(menuitem_text, popup_menu_x, popup_menu_y, true);
			}else if(menuitem_text.equals("Positive Area")){
				target = null;		//Clear target if set
				GET_POSITIVE_AREA = true;
			}else if(menuitem_text.equals("Negative Area")){
				target = null;		//Clear target if set
				GET_NEGATIVE_AREA = true;
			}else if(menuitem_text.equals("Target Area")){
				target = null;		//Clear target if set
				GET_TARGET_AREA = true;
			}else if(menuitem_text.equals("Require Current")){
				synchronized(this){
					script.addDesktop(desktop);
				}
			}else if(menuitem_text.equals("End Script")){
				script.save(output_path);
				script = null;
				RECORDING_SCRIPT = false;
				setPopupMenu();
				repaint();
			}
		}
	}

	/**
   * Handle mouse pressed events.
   * @param e the mouse event
   */
  public void mousePressed(MouseEvent e)
  {
  	setCapturing(true);		//Undo capturing halt from popup menu
  	
  	if(e.getButton() == 1){
  		if(RECORDING_SCRIPT){
	  		if(GET_POSITIVE_AREA || GET_NEGATIVE_AREA || GET_TARGET_AREA){
		    	selection_box_x0 = e.getX();
		    	selection_box_y0 = e.getY();
		    	DRAGGING_SELECTION_BOX = true;
		    	repaint();
	  		}else{
	  			performAction("Click", e.getX(), e.getY(), true);
	  		}
  		}
  	}else if(e.getButton() == 3){		//Show popup menu with conversion options
  		popup_menu_x = e.getX();
  		popup_menu_y = e.getY();
  		popup_menu.show(e.getComponent(), popup_menu_x, popup_menu_y);
  		setCapturing(false);
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
		int[][] image;
		
		if(DRAGGING_SELECTION_BOX){
			DRAGGING_SELECTION_BOX = false;
			
			if(GET_POSITIVE_AREA || GET_NEGATIVE_AREA || GET_TARGET_AREA){
				selection_box_minx /= panel_scale;
				selection_box_miny /= panel_scale;
				selection_box_maxx /= panel_scale;
				selection_box_maxy /= panel_scale;

				if(GET_POSITIVE_AREA){
					script.addPositiveArea(new int[]{selection_box_minx, selection_box_miny, selection_box_maxx, selection_box_maxy});
					GET_POSITIVE_AREA = false;
				}else if(GET_NEGATIVE_AREA){
					script.addNegativeArea(new int[]{selection_box_minx, selection_box_miny, selection_box_maxx, selection_box_maxy});
					GET_NEGATIVE_AREA = false;
				}else if(GET_TARGET_AREA){
					synchronized(this){
						image = ImageUtility.image2argb(desktop);
					}
					
					target = ImageUtility.crop(image, selection_box_minx, selection_box_miny, selection_box_maxx, selection_box_maxy);
					//ImageViewer.show(target, target[0].length, target.length);
					GET_TARGET_AREA = false;
				}
			}
			
			repaint();
		}
	}
	
	public void mouseClicked(MouseEvent e) {}	
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mouseMoved(MouseEvent e) {}	
	public void keyTyped(KeyEvent e) {}
	public void keyPressed(KeyEvent e) {}
	public void keyReleased(KeyEvent e) {}
	
	/**
	 * Background thread to capture screen shots and record interaction.
	 */
	public void run()
	{
		double fps = 0;
		long t0 = System.currentTimeMillis();
		long t1;
		screen_size = Toolkit.getDefaultToolkit().getScreenSize();
		BufferedImage image;
		
		while(true){
			if(CAPTURING){
				synchronized(this){
					desktop = robot.createScreenCapture(new Rectangle(0,0,(int)screen_size.getWidth(),(int)screen_size.getHeight()));
				}
				
		  	//image = ImageUtility.resize(desktop, panel_scale, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		  	image = ImageUtility.resize(desktop, panel_scale, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
	
				synchronized(this){
					desktop_scaled = image;
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
			}else{
				Utility.pause(100);
			}
		}
	}

	/**
	 * Start the monkey script session.
	 * @param args command line arguments
	 */
	public static void main(String args[])
	{
		int screens = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices().length;
		
		if(screens > 1){
			SoftwareReuseMonkey_DS monkey = new SoftwareReuseMonkey_DS("SoftwareReuseMonkey_DS.ini");
			Dimension screen_size = Toolkit.getDefaultToolkit().getScreenSize();

	    JFrame frame = new JFrame("Monkey Script Session");
	    frame.add(monkey);
	    frame.setResizable(false);
	    frame.pack();
	    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	    frame.setLocation((int)screen_size.getWidth(), 0);
	    frame.setVisible(true);
		}else{
			System.out.println("Sorry, MonkeyScriptSession_DS requires two screens!");
		}
	}
}
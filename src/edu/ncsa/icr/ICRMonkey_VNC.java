package edu.ncsa.icr;
import edu.ncsa.image.*;
import edu.ncsa.utility.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.swing.*;
import java.io.*;
import java.util.*;
import com.tightvnc.vncviewer.*;

/**
 * A program for creating AHK scripts (monkey see, monkey do)
 * @author Kenton McHenry
 */
public class ICRMonkey_VNC extends Component implements ActionListener, MouseListener, MouseMotionListener, KeyListener
{
	private Vector<String> servers = new Vector<String>();
	private String data_path = "./";
	private String output_path = "./";
	private int ignored_bottom = 0;
	private int ignored_left = 0;
	
	private String server;
	private int port;	
	private VncViewer vnc;
	private int screen_width;
	private int screen_height;
	private ICRMonkeyScript script = null;

	private String[] operations = new String[]{"open", "save", "convert", "exit"};
	
	private JPopupMenu popup_menu;
	private int popup_menu_x;
	private int popup_menu_y;
	
	private MouseListener vc_mouse_listener;
	private MouseMotionListener vc_mouse_motion_listener;
	private KeyListener vc_key_listener;
	
	private boolean RECORDING_SCRIPT = false;
	private boolean PAUSING_SCRIPT = false;
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
	public ICRMonkey_VNC(String filename)
	{
		int index = 0;
		
		loadINI(filename);
				
		if(servers.size() > 1){
			index = serverSelector();
		}
				
		setPopupMenu();
		setServer(servers.get(index));
		connect();
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
	        	if(key.equals("Server")){
	        		servers.add(value);
	        	}else if(key.equals("DataPath")){
	        		data_path = value + "/";
	        	}else if(key.equals("OutputPath")){
	        		output_path = value + "/";
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
	 * Allow the user to select a server to connect to.
	 * @return the index of the server to connect to
	 */
	private int serverSelector()
	{
		Console console = System.console();
		String line;
		int index = 0;

		if(console != null){
			System.out.println();
			
			for(int i=0; i<servers.size(); i++){
				System.out.println(i + ") " + servers.get(i));
			}
			
			line = console.readLine("\nserver> ");
			index = Integer.valueOf(line);
		}
		
		return index;
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
	  	if(PAUSING_SCRIPT){
			  item = new JMenuItem("Resume Script"); item.addActionListener(this); popup_menu.add(item);
	  	}else{
			  item = new JMenuItem("Run Command"); item.addActionListener(this); popup_menu.add(item);
			  
			  submenu1 = new JMenu("Select");
			  item = new JMenuItem("Positive Area"); item.addActionListener(this); submenu1.add(item);
			  item = new JMenuItem("Negative Area"); item.addActionListener(this); submenu1.add(item);
			  submenu1.addSeparator();
			  item = new JMenuItem("Target Area"); item.addActionListener(this); submenu1.add(item);
			  popup_menu.add(submenu1);
			   
			  submenu1 = new JMenu("Insert Argument");
			  item = new JMenuItem("Argument 1"); item.addActionListener(this); submenu1.add(item);
			  item = new JMenuItem("Argument 2"); item.addActionListener(this); submenu1.add(item);
			  item = new JMenuItem("Argument 3"); item.addActionListener(this); submenu1.add(item);
			  popup_menu.add(submenu1);
			  
			  item = new JMenuItem("Require Current"); item.addActionListener(this); popup_menu.add(item);
			  popup_menu.addSeparator();
			  
			  item = new JMenuItem("Pause Script"); item.addActionListener(this); popup_menu.add(item);
			  item = new JMenuItem("End Script"); item.addActionListener(this); popup_menu.add(item);
	  	}
	  }
	}

	/**
	 * Add ignored areas to the script.
	 */
	private void addIgnoredAreas()
	{		
		//Add bottom area
		if(ignored_bottom > 0){
			script.addNegativeArea(new int[]{0, screen_height-ignored_bottom, screen_width, screen_height});
		}
		
		//Add left area
		if(ignored_left > 0){
			script.addNegativeArea(new int[]{0, 0, ignored_left, screen_height});
		}
		
		repaint();
	}
	
	/**
	 * Set the VNC server to connect to.
	 * @param server the server to connect to
	 */
	public void setServer(String server)
	{
		int tmpi = server.lastIndexOf(':');
		
		if(tmpi < 0){
			this.server = server;
			port = 5900;
		}else{
	  	this.server = server.substring(0,tmpi);
	  	port = Integer.valueOf(server.substring(tmpi+1));
		}
	}
	
	/**
	 * Connect to the VNC server.
	 */
	public void connect()
	{
		BufferedImage image;
		
    vnc = new VncViewer();
	  vnc.mainArgs = new String[]{"HOST", server, "PORT", String.valueOf(port)};
	  vnc.inAnApplet = false;
	  vnc.inSeparateFrame = true;
	  vnc.init();
	  vnc.start();
	  
	  //Wait for the VncCanvas to be initialized
	  while(vnc.vc == null){
	  	Utility.pause(500);
	  }
	  
	  //Add auxiliary code to the paint method
	  vnc.vc.paint_aux = this;

	  //Replace mouse listeners
	  vc_mouse_listener = vnc.vc.getMouseListeners()[0];
	  vnc.vc.removeMouseListener(vc_mouse_listener);
	  vnc.vc.addMouseListener(this);

	  vc_mouse_motion_listener = vnc.vc.getMouseMotionListeners()[0];
	  vnc.vc.removeMouseMotionListener(vc_mouse_motion_listener);
	  vnc.vc.addMouseMotionListener(this);
	  
	  vc_key_listener = vnc.vc.getKeyListeners()[0];
	  vnc.vc.removeKeyListener(vc_key_listener);
	  vnc.vc.addKeyListener(this);
	  
	  //Wait for a screen capture
	  while(vnc.vc.memImage == null){
	  	Utility.pause(500);
	  }
	  
	  //Get the screen size
	  image = (BufferedImage)vnc.vc.memImage;
	  screen_width = image.getWidth();
	  screen_height = image.getHeight();
	}

	/**
	 * Auxiliary code for the VncViewer's VncCanvas paint method.
	 * @param g the graphics context to draw to
	 */
	public void paint(Graphics g)
	{
		int[][] image = null;
		int[] box;
		int x, y, w, h;
		
		synchronized(vnc.vc.memImage){
			if(target != null) image = ImageUtility.image2argb((BufferedImage)vnc.vc.memImage);
		}
		
    //Draw selection box
    if(DRAGGING_SELECTION_BOX){
    	ImageUtility.drawBox(g, selection_box_minx, selection_box_miny, selection_box_maxx, selection_box_maxy, new Color(0x003399ff), 0.25f);
    }
    
    if(script != null){
    	if(target == null){
		    //Draw positive areas
		    for(int i=0; i<script.getPositiveAreas().size(); i++){
		    	ImageUtility.drawBox(g, script.getPositiveAreas().get(i), Color.white, 0.5f);
		    }
		    
		    //Draw negative areas
		    for(int i=0; i<script.getNegativeAreas().size(); i++){
		    	ImageUtility.drawBox(g, script.getNegativeAreas().get(i), Color.darkGray, 0.5f);
		    }
    	}else{
    		//Draw target matches
    		target_locations = ImageUtility.find(image, target, 0, false);
    		
    		for(int i=0; i<target_locations.size(); i++){
    			x = target_locations.get(i).x;
    			y = target_locations.get(i).y;
    			h = target.length;
    			w = target[0].length;
    			
        	ImageUtility.drawBox(g, x, y, x+w-1, y+h-1, Color.red, 0.25f);
    		}
    	}
    }
	}
	
	/**
	 * Repaint the VncViewer's VncCanvas.
	 */
	public void repaint()
	{
		vnc.vc.repaint();
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

			if(Utility.contains(operations, menuitem_text)){
				script = new ICRMonkeyScript();
				script.setOperation(menuitem.getText());

				do{		//Find the next available number to assign as a script name
					script.setAlias(Utility.toString(script_count, 3));
					script_count++;
				}while(Utility.exists(output_path + script.getName() + ".ms") || Utility.exists(output_path + script.getName()));
				
				addIgnoredAreas();
				RECORDING_SCRIPT = true;
				setPopupMenu();
			}else if(menuitem_text.equals("Run Command")){
				command = JOptionPane.showInputDialog(vnc.vc, "");
        script.addCommand(command);
        PAUSING_SCRIPT = true;
        setPopupMenu();
			}else if(menuitem_text.equals("Positive Area")){
				target = null;
				GET_POSITIVE_AREA = true;
			}else if(menuitem_text.equals("Negative Area")){
				target = null;
				GET_NEGATIVE_AREA = true;
			}else if(menuitem_text.equals("Target Area")){
				target = null;
				GET_TARGET_AREA = true;
			}else if(menuitem_text.startsWith("Argument")){
				script.addArgument(Integer.valueOf(menuitem_text.substring(menuitem_text.length()-1))-1);
				PAUSING_SCRIPT = true;
				setPopupMenu();
			}else if(menuitem_text.equals("Require Current")){
				synchronized(this){
					script.addDesktop((BufferedImage)vnc.vc.memImage);
				}
			}else if(menuitem_text.equals("Pause Script")){
				PAUSING_SCRIPT = true;	
				setPopupMenu();
			}else if(menuitem_text.equals("Resume Script")){
				PAUSING_SCRIPT = false;
				setPopupMenu();
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
  	if(e.getButton() == 1){
  		if(RECORDING_SCRIPT && !PAUSING_SCRIPT){
  			if(GET_POSITIVE_AREA || GET_NEGATIVE_AREA || GET_TARGET_AREA){
		    	selection_box_x0 = e.getX();
		    	selection_box_y0 = e.getY();
		    	DRAGGING_SELECTION_BOX = true;
  			}else{
	  			if(e.getClickCount() == 1){			  		
						synchronized(vnc.vc.memImage){
							if(target != null && !target_locations.isEmpty()){
								script.addTargetClick(target, target_locations.get(0).x, target_locations.get(0).y, e.getX(), e.getY());
							}else{
								script.addClick((BufferedImage)vnc.vc.memImage, e.getX(), e.getY());
								addIgnoredAreas();								//Add ignored sections again
							}
			  		}
					}else if(e.getClickCount() == 2){
						script.lastClickToDoubleClick();	//We will always send a single click first (above)!
					}
					
					vc_mouse_listener.mousePressed(e);	
  			}		    	
  			
  			repaint();
  		}else{
  			vc_mouse_listener.mousePressed(e);
  		}
  	}else if(e.getButton() == 3){		//Show popup menu with conversion options
  		popup_menu_x = e.getX();
  		popup_menu_y = e.getY();
  		popup_menu.show(e.getComponent(), popup_menu_x, popup_menu_y);
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
  	}else{
  		vc_mouse_motion_listener.mouseDragged(e);
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
			
			if(GET_POSITIVE_AREA){
				script.addPositiveArea(new int[]{selection_box_minx, selection_box_miny, selection_box_maxx, selection_box_maxy});
				GET_POSITIVE_AREA = false;
			}else if(GET_NEGATIVE_AREA){
				script.addNegativeArea(new int[]{selection_box_minx, selection_box_miny, selection_box_maxx, selection_box_maxy});
				GET_NEGATIVE_AREA = false;
			}else if(GET_TARGET_AREA){
				synchronized(vnc.vc.memImage){
					image = ImageUtility.image2argb((BufferedImage)vnc.vc.memImage);
				}
				
				target = ImageUtility.crop(image, selection_box_minx, selection_box_miny, selection_box_maxx, selection_box_maxy);
				//ImageViewer.show(target, target[0].length, target.length);
				GET_TARGET_AREA = false;
			}
			
			repaint();
		}else{
			vc_mouse_listener.mouseReleased(e);
		}
	}
		
	/**
	 * Handle key typed events.
	 * @param e the key event
	 */
	public void keyTyped(KeyEvent e)
	{		
		if(!PAUSING_SCRIPT){
			script.addKey(e.getKeyChar());
		}
		
		vc_key_listener.keyTyped(e);
	}

	public void mouseClicked(MouseEvent e) {vc_mouse_listener.mouseClicked(e);}
	public void mouseEntered(MouseEvent e) {vc_mouse_listener.mouseEntered(e);}
	public void mouseExited(MouseEvent e) {vc_mouse_listener.mouseExited(e);}
	public void mouseMoved(MouseEvent e) {vc_mouse_motion_listener.mouseMoved(e);}
	public void keyPressed(KeyEvent e) {vc_key_listener.keyPressed(e);}
	public void keyReleased(KeyEvent e) {vc_key_listener.keyReleased(e);}

	/**
	 * The programs main.
	 * @param args the command line arguments
	 */
	public static void main(String[] args)
	{
		ICRMonkey_VNC monkey = new ICRMonkey_VNC("ICRMonkey_VNC.ini");
	}
}
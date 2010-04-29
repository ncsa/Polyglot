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
	
	private String server;
	private int port;	
	private VncViewer vnc;	
	private ICRMonkeyScript script = null;

	private String[] operations = new String[]{"open", "save", "import", "export", "convert", "exit"};
	
	private JPopupMenu popup_menu;
	private MouseListener vc_mouse_listener;
	private MouseMotionListener vc_mouse_motion_listener;
	private KeyListener vc_key_listener;
	
	private boolean RECORDING_SCRIPT = false;
	private int script_count = 0;
  private int selection_box_x0, selection_box_y0, selection_box_x1, selection_box_y1;
  private int selection_box_minx, selection_box_miny, selection_box_maxx, selection_box_maxy;
  private boolean DRAGGING_SELECTION_BOX;
  private boolean GET_POSITIVE_AREA;
  private boolean GET_NEGATIVE_AREA;
	
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
		  submenu1 = new JMenu("Select");
		  item = new JMenuItem("Positive Area"); item.addActionListener(this); submenu1.add(item);
		  item = new JMenuItem("Negative Area"); item.addActionListener(this); submenu1.add(item);
		  popup_menu.add(submenu1);
		  popup_menu.addSeparator();
		  
		  item = new JMenuItem("End Script"); item.addActionListener(this); popup_menu.add(item);
	  }
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
	 * @param server the server to connect to
	 * @param port the port to connect to
	 */
	public void connect()
	{
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
	}

	/**
	 * Auxiliary code for the VncViewer's VncCanvas paint method.
	 * @param g the graphics context to draw to
	 */
	public void paint(Graphics g)
	{
    //Draw selection box
    if(DRAGGING_SELECTION_BOX){
    	if(GET_NEGATIVE_AREA){
    		ImageUtility.drawBox(g, selection_box_minx, selection_box_miny, selection_box_maxx, selection_box_maxy, Color.red, 0);
    	}else{
    		ImageUtility.drawBox(g, selection_box_minx, selection_box_miny, selection_box_maxx, selection_box_maxy, Color.blue, 0);
    	}
    }
    
    if(script != null){
	    //Draw positive areas
	    for(int i=0; i<script.getPositiveAreas().size(); i++){
	    	ImageUtility.drawBox(g, script.getPositiveAreas().get(i), Color.blue, 0.5f);
	    }
	    
	    //Draw negative areas
	    for(int i=0; i<script.getNegativeAreas().size(); i++){
	    	ImageUtility.drawBox(g, script.getNegativeAreas().get(i), Color.red, 0.5f);
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
		
		if(object instanceof JMenuItem){
			menuitem = (JMenuItem)object;
			
			if(Utility.contains(operations, menuitem.getText())){
				script = new ICRMonkeyScript();
				script.setOperation(menuitem.getText());

				do{		//Find the next available number to assign as a script name
					script.setAlias(Utility.toString(script_count, 3));
					script_count++;
				}while(Utility.exists(output_path + script.getName() + ".ms") || Utility.exists(output_path + script.getName()));
				
				RECORDING_SCRIPT = true;
				setPopupMenu();
			}else if(menuitem.getText().equals("Positive Area")){
				GET_POSITIVE_AREA = true;
			}else if(menuitem.getText().equals("Negative Area")){
				GET_NEGATIVE_AREA = true;
			}else if(menuitem.getText().equals("End Script")){
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
  		if(RECORDING_SCRIPT){
  			if(GET_POSITIVE_AREA || GET_NEGATIVE_AREA){
		    	selection_box_x0 = e.getX();
		    	selection_box_y0 = e.getY();
		    	DRAGGING_SELECTION_BOX = true;
  			}else{
	  			if(e.getClickCount() == 1){			  		
						synchronized(vnc.vc.memImage){
			  			script.addClick((BufferedImage)vnc.vc.memImage, e.getX(), e.getY());
			  		}			  		
					}else if(e.getClickCount() == 2){
						script.lastClickToDoubleClick();
					}
					
					vc_mouse_listener.mousePressed(e);	
  			}		    	
  			
  			repaint();
  		}else{
  			vc_mouse_listener.mousePressed(e);
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
		if(DRAGGING_SELECTION_BOX){
			DRAGGING_SELECTION_BOX = false;
			
			if(GET_POSITIVE_AREA){
				script.addPositiveArea(new int[]{selection_box_minx, selection_box_miny, selection_box_maxx, selection_box_maxy});
				GET_POSITIVE_AREA = false;
			}else if(GET_NEGATIVE_AREA){
				script.addNegativeArea(new int[]{selection_box_minx, selection_box_miny, selection_box_maxx, selection_box_maxy});
				GET_NEGATIVE_AREA = false;
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
		script.addKey(e.getKeyChar());
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
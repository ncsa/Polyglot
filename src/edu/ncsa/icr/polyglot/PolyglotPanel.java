package edu.ncsa.icr.polyglot;
import edu.ncsa.icr.*;
import edu.ncsa.utility.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import javax.swing.*;
import java.io.*;
import java.util.*;
import java.util.List;

/**
 * A panel with a file manager look and feel designed for convenient file format conversions.
 * @author Kenton McHenry
 */
public class PolyglotPanel extends JPanel implements MouseListener, MouseMotionListener, DropTargetListener, ActionListener
{
	private Polyglot polyglot;
	private JFrame frame;
	private JPopupMenu popup_menu;
	private String path;
	private TreeSet<FileLabel> files = new TreeSet<FileLabel>();
	private TreeSet<FileLabel> selected_files = new TreeSet<FileLabel>();
	private JScrollPane scroll_pane;
	
  private Graphics bg;
  private Image offscreen;
  
  private int selection_box_x0, selection_box_y0, selection_box_x1, selection_box_y1;
  private int selection_box_minx, selection_box_miny, selection_box_maxx, selection_box_maxy;
  private boolean DRAGGING_SELECTION_BOX;

  /**
   * Class constructor.
   * @param filename the name of a *.ini file
   */
	public PolyglotPanel(JFrame frame, String filename)
	{   		
		loadINI(filename);
		this.frame = frame;

		setBackground(Color.white);
    setLayout(new edu.ncsa.icr.FlowLayout(java.awt.FlowLayout.LEADING));
    addMouseListener(this);
    this.addMouseMotionListener(this);
    new DropTarget(this, this);

    scroll_pane = new JScrollPane(this);
    scroll_pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
    scroll_pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);				
    		   
    setPath(path);
	}
	
	/**
	 * Initialize based on parameters within the given *.ini file.
	 * @param filename the name of the *.ini file
	 */
	public void loadINI(String filename)
	{
	  try{
	    BufferedReader ins = new BufferedReader(new FileReader(filename));
	    String line, key, value;
	    String server;
	    int port, tmpi;
	    	    
	    while((line=ins.readLine()) != null){
	      if(line.contains("=")){
	        key = line.substring(0, line.indexOf('='));
	        value = line.substring(line.indexOf('=')+1);
	        
	        if(key.charAt(0) != '#'){
	          if(key.equals("DefaultPath")){
	          	path = value + "/";
	          }else if(key.equals("PolyglotType")){
	          	if(value.equals("PolyglotSteward")){
	          		polyglot = new PolyglotSteward();
	          	}
	          }else if(key.equals("ICRServer")){
	          	if(polyglot instanceof PolyglotSteward){
	          		tmpi = value.lastIndexOf(':');
		        		
		        		if(tmpi != -1){
		        			server = value.substring(0, tmpi);
		        			port = Integer.valueOf(value.substring(tmpi+1));
		        			((PolyglotSteward)polyglot).add(server, port);
		        		}
	          	}
	          }
	        }
	      }
	    }
	    
	    ins.close();
	  }catch(Exception e){}
	}
	
	/**
	 * Set the folder path we are looking at.
	 * @param path the folder path
	 */
	public void setPath(String path)
	{
		File file = new File(path);
		File[] path_files;
		FileLabel file_label;
		Iterator<FileLabel> itr;
		
		//Remove old files
		selected_files.clear();
		
		if(!files.isEmpty()){
			itr = files.iterator();
			
			while(itr.hasNext()){
				remove(itr.next());
			}
			
			files.clear();	
		}
		
		//Load files
		if(file.isDirectory()){
			this.path = Utility.unixPath(path);
			if(this.path.charAt(this.path.length()-1) != '/') this.path += "/";
			if(frame != null) frame.setTitle("Polyglot File Manager [" + path + "]");
			
			//Add ".."
			if(file.getParentFile() != null){
				file_label = new FileLabel(file.getParentFile(), "..", this);
				file_label.setLargeIcon();
				add(file_label);
				files.add(file_label);
			}
					
			//Add folders
			path_files = file.listFiles();

			for(int i=0; i<path_files.length; i++){
				if(path_files[i].isDirectory()){
					file_label = new FileLabel(path_files[i], this);
					file_label.setLargeIcon();
					add(file_label);
					files.add(file_label);
				}
			}
			
			//Add files
			for(int i=0; i<path_files.length; i++){
				if(!path_files[i].isDirectory()){
					file_label = new FileLabel(path_files[i], this);
					file_label.setLargeIcon();
					add(file_label);
					files.add(file_label);
				}
			}
		}
		
		revalidate();
		repaint();
	}
	
	/**
   * Set the popup menu.
   */
  private void setPopupMenu()
  {    
  	TreeSet<String> inputs = new TreeSet<String>();
  	TreeSet<String> outputs;
  	String output;
    JMenu submenu1, submenu2;    	
    JMenuItem item;
    TreeSet<String> string_set = null;
    int max_menuitems = 40;
    int count = 0;
    
    //Set inputs/outputs
    for(Iterator<FileLabel> itr=selected_files.iterator(); itr.hasNext();){
    	inputs.add(itr.next().getExtension());
    }
    
    outputs = polyglot.getOutputs(inputs);
    
    //Set up popup menu
    popup_menu = new JPopupMenu(); 

    //Convert sub-menu
    submenu1 = new JMenu("Convert");
    
    if(!outputs.isEmpty()){
    	if(outputs.size() > max_menuitems){
    		submenu2 = new JMenu();
    		string_set = new TreeSet<String>();
    		count = 0;
    	}else{
    		submenu2 = null;
    	}
    	    	
    	//Add outputs to menu/sub-menus
    	for(Iterator<String> itr=outputs.iterator(); itr.hasNext();){
    		if(submenu2 != null && count >= max_menuitems){
    			submenu2.setText(string_set.first() + " to " + string_set.last());
    			submenu1.add(submenu2);
    			submenu2 = new JMenu();
    			string_set.clear();
    			count = 0;
    		}
    		
    		output = itr.next();
    		item = new JMenuItem(output); item.addActionListener(this);
    		
    		if(submenu2 == null){
    			submenu1.add(item);
    		}else{
    			string_set.add(output);
    			submenu2.add(item);
    		}
    		
    		count++;
    	}
      
    	//Add last sub-menu if partially filled
    	if(submenu2 != null && count > 0){
  			submenu2.setText(string_set.first() + " to " + string_set.last());
    		submenu1.add(submenu2);
    	}
    }
    
    popup_menu.add(submenu1);    
  }
  
	/**
	 * Get the scroll pane for this panel.
	 * @return the scroll pane
	 */
	public Component getScrollPane()
	{
		return scroll_pane;
	}
	
	/**
	 * Close polyglot connections.
	 */
	public void close()
	{
		if(polyglot != null) polyglot.close();
	}

	/**
   * Draw the panel to the given graphics context.
   * @param g the graphics context to draw to
   */
  public void paint(Graphics g)
  {
    //Update background buffer if needed
    int width = getSize().width;
    int height = getSize().height;    
    
    if(offscreen == null || width != offscreen.getWidth(null) || height != offscreen.getHeight(null)){ 
      offscreen = createImage(width, height);
      bg = offscreen.getGraphics();
    }
    
    //Draw the panel
    super.paint(bg);
    
    //Draw selection box
    if(DRAGGING_SELECTION_BOX){
    	bg.setColor(Color.blue);
  	  bg.drawLine(selection_box_minx, selection_box_miny, selection_box_maxx, selection_box_miny);		//Top
  	  bg.drawLine(selection_box_minx, selection_box_maxy, selection_box_maxx, selection_box_maxy);		//Bottom
  	  bg.drawLine(selection_box_minx, selection_box_miny, selection_box_minx, selection_box_maxy);		//Left
  	  bg.drawLine(selection_box_maxx, selection_box_miny, selection_box_maxx, selection_box_maxy);		//Right
    }
    
    //Draw background  buffer
    g.drawImage(offscreen, 0, 0, this);
  }
  	
  /**
   * Deselect all labels.
   */
  private void deselectAll()
  {
		Iterator<FileLabel> itr = files.iterator();
		
		while(itr.hasNext()){
			itr.next().setDeselected();
		}
		
		selected_files.clear();  	
  }
  
  /**
   * Handle mouse pressed events.
   * @param e the mouse event
   */
  public void mousePressed(MouseEvent e)
  {
  	Object object = e.getSource();
  	FileLabel file_label;
  	
  	if(object instanceof FileLabel){
  		file_label = (FileLabel)object;
  		
  		if(!file_label.getFile().isDirectory()){
	  		if(e.isControlDown()){	//Select multiple files
		  		if(selected_files.contains(file_label)){
		  			selected_files.remove(file_label);
		  			file_label.setDeselected();
		  		}else{
		  			selected_files.add(file_label);
		  			file_label.setSelected();
		  		}
	  		}else{									//Select one file
	  			if(e.getButton()==1 || (e.getButton()==3 && selected_files.size()<2)){
	  				deselectAll();
	  				selected_files.add(file_label);
	  				file_label.setSelected();
	  			}
	  		}
  		}else{										//Simply highlight directories, don't track their selection however
  			deselectAll();
  			file_label.setSelected();
  		}
  		
  		//Show popup menu with conversion options
  		if(e.getButton() == 3){
  			if(!file_label.getFile().isDirectory()){
	  			setPopupMenu();
	  			popup_menu.show(e.getComponent(), e.getX(), e.getY());
  			}
  		}
  	}else{
  		deselectAll();
  		selection_box_x0 = e.getX();
  		selection_box_y0 = e.getY();
  	}
  	
  	repaint();
  }

  /**
   * Handle mouse motion events.
   * @param e the mouse event
   */
  public void mouseDragged(MouseEvent e)
  {
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
  	
  	DRAGGING_SELECTION_BOX = true;
  	repaint();
  }

  /**
   * Handle mouse release events.
   * @param e the mouse event
   */
	public void mouseReleased(MouseEvent e)
	{
		FileLabel file_label;
		Rectangle rectangle;
		int x, y;
		
		if(DRAGGING_SELECTION_BOX){		//Find selected components
			for(Iterator<FileLabel> itr=files.iterator(); itr.hasNext();){
				file_label = itr.next();
				rectangle = file_label.getBounds();
				x = (int)rectangle.getCenterX();
				y = (int)rectangle.getCenterY();
				
				if(x>selection_box_minx && x<selection_box_maxx && y>selection_box_miny && y<selection_box_maxy){
					selected_files.add(file_label);
					file_label.setSelected();
				}
			}
			
			DRAGGING_SELECTION_BOX = false;
			repaint();
		}
	}

	/**
   * Handle mouse click events.
   * @param e the mouse event
   */
	public void mouseClicked(MouseEvent e)
	{
		Object object = e.getSource();
		File file;
		
		if(object instanceof FileLabel){
			file = ((FileLabel)object).getFile();

			if(e.getButton() == 1 && e.getClickCount() == 2){
				if(file.isDirectory()){
					setPath(file.getAbsolutePath());
				}
			}
		}
	}
	
  /**
   * Handle drop events.
   * @param e the drop event
   */
  public void drop(DropTargetDropEvent e)
	{
    e.acceptDrop(e.getDropAction());
    
    if(e.getTransferable() != null){
	  	try{    	
	      if(e.getTransferable().isDataFlavorSupported(DataFlavor.javaFileListFlavor)){
	        List<File> list = (List<File>)e.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
	        Iterator<File> itr = list.iterator();
	        File file;
	        FileInputStream ins;
	        FileOutputStream outs;
	        byte[] buffer = new byte[4096];
	        int count;
	        
	        //Copy files to the current path
	        while(itr.hasNext()){
	        	file = itr.next();
	        	ins = new FileInputStream(file);
	        	outs = new FileOutputStream(new File(path + file.getName()));
	        	
	        	while((count = ins.read(buffer)) != -1){
	            outs.write(buffer, 0, count);
	        	}
	        	
	        	ins.close();
	        	outs.close();
	        }
	      }
	      
	      //Refresh folder view
	      setPath(path);
	    }catch(Exception ex) {ex.printStackTrace();}
    }
        
    e.dropComplete(true);
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
			
			for(Iterator<FileLabel> itr=selected_files.iterator(); itr.hasNext();){
				polyglot.convert(Utility.unixPath(itr.next().toString()), path, menuitem.getText());
				setPath(path);
			}
		}
	}

	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mouseMoved(MouseEvent e) {}
  public void dragEnter(DropTargetDragEvent e) {}
	public void dragExit(DropTargetEvent e) {}
	public void dragOver(DropTargetDragEvent e) {}
	public void dropActionChanged(DropTargetDragEvent e) {}
	
	/**
	 * Start the Polyglot file manager, essentially an instance of a PolyglotPanel.
	 * @param args command line arguments
	 */
	public static void main(String args[])
	{    
		JFrame frame = new JFrame();
		final PolyglotPanel polyglot_panel = new PolyglotPanel(frame, "PolyglotPanel.ini");
		    
		frame.add(polyglot_panel.getScrollPane());
		frame.addWindowListener(new WindowAdapter(){
			public void windowClosing(WindowEvent event){
				polyglot_panel.close();
			}
		});
		
    frame.setSize(600, 400);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 
    frame.setVisible(true);
	}
}
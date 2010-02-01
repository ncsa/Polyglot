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
public class PolyglotPanel extends JPanel implements MouseListener, DropTargetListener
{
	private Polyglot polyglot;
	private JFrame frame;
	private String path;
	private TreeSet<FileLabel> files = new TreeSet<FileLabel>();
	private TreeSet<FileLabel> selected_files = new TreeSet<FileLabel>();
	private JScrollPane scroll_pane;
	private int width, height;
	
  private Graphics bg;
  private Image offscreen;
  
  /**
   * Class constructor.
   * @param filename the name of a *.ini file
   */
	public PolyglotPanel(JFrame frame, String filename)
	{   		
		loadINI(filename);
		this.frame = frame;
		
		setBackground(Color.white);
    setLayout(new FlowLayout(FlowLayout.LEADING));
    addMouseListener(this);
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
	    
	    while((line=ins.readLine()) != null){
	      if(line.contains("=")){
	        key = line.substring(0, line.indexOf('='));
	        value = line.substring(line.indexOf('=')+1);
	        
	        if(key.charAt(0) != '#'){
	          if(key.equals("DefaultPath")){
	          	path = value + "/";
	          }else if(key.equals("Width")){
	          	width = Integer.valueOf(value) - 25;
	          }else if(key.equals("Height")){
	          	height = Integer.valueOf(value);
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
		
		repaint();
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
   * Draw the panel to the given graphics context.
   * @param g the graphics context to draw to
   */
  public void paint(Graphics g)
  {
  	int width, height;
  	
		if(scroll_pane != null){
			setPreferredSize(new Dimension(scroll_pane.getWidth(), getSize().height));
			revalidate();
		}
		    
    //Update background buffer if needed
    width = getSize().width;
    height = getSize().height;    
    
    if(offscreen == null || width != offscreen.getWidth(null) || height != offscreen.getHeight(null)){ 
      offscreen = createImage(width, height);
      bg = offscreen.getGraphics();
    }
    
    super.paint(bg);
    
    //Draw background  buffer
    g.drawImage(offscreen, 0, 0, this);
  }
  	
  /**
   * Deselect all labels.
   */
  private void deselectAll()
  {
		Iterator<FileLabel> itr = selected_files.iterator();
		
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
  		
  		if(e.isControlDown()){
	  		if(selected_files.contains(file_label)){
	  			selected_files.remove(file_label);
	  			file_label.setDeselected();
	  		}else{
	  			selected_files.add(file_label);
	  			file_label.setSelected();
	  		}
  		}else{
  			deselectAll();
  			selected_files.add(file_label);
  			file_label.setSelected();
  		}
  	}else{
  		deselectAll();
  	}
  	
  	repaint();
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
	
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mouseReleased(MouseEvent e) {}	
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
		PolyglotPanel polyglot_panel = new PolyglotPanel(frame, "PolyglotPanel.ini");
		
    frame.setSize(600, 400);
    frame.add(polyglot_panel.getScrollPane());
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 
    frame.setVisible(true);
	}


}
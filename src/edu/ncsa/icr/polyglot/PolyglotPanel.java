package edu.ncsa.icr.polyglot;
import edu.ncsa.icr.*;
import edu.ncsa.icr.ICRAuxiliary.*;
import edu.ncsa.utility.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import javax.swing.*;
import java.io.*;
import java.util.*;
import java.util.List;

public class PolyglotPanel extends JPanel implements DropTargetListener, MouseListener
{
	private TreeSet<FileLabel> files = new TreeSet<FileLabel>();
	private TreeSet<FileLabel> selected_files = new TreeSet<FileLabel>();
  private Graphics bg;
  private Image offscreen;
  
	public PolyglotPanel()
	{
    setBackground(Color.white);
    new DropTarget(this, this);
    addMouseListener(this);
	}
	
	/**
   * Draw the panel to the given graphics context.
   * @param g the graphics context to draw to
   */
  public void paint(Graphics g)
  {
    int width = getSize().width;
    int height = getSize().height;
        
    //Update background buffer if needed
    if(offscreen == null || width != offscreen.getWidth(null) || height != offscreen.getHeight(null)){ 
      offscreen = createImage(width, height);
      bg = offscreen.getGraphics();
    }
    
    super.paint(bg);
    
    //Draw background  buffer
    g.drawImage(offscreen, 0, 0, this);
  }
  	
  public void drop(DropTargetDropEvent e)
	{
    e.acceptDrop(e.getDropAction());
    
    if(e.getTransferable() != null){
	  	try{    	
	      if(e.getTransferable().isDataFlavorSupported(DataFlavor.javaFileListFlavor)){
	        List<File> list = (List<File>)e.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
	        Iterator<File> itr = list.iterator();
	        FileLabel file_label;
	        
	        while(itr.hasNext()){
	        	file_label = new FileLabel(itr.next(), this);
	        	
	        	if(!files.contains(file_label)){
	        		file_label.setLargeIcon();
	        		add(file_label);
	        		files.add(file_label);
	        	}
	        }
	      }
	      
	      revalidate();
	    }catch(Exception ex) {ex.printStackTrace();}
    }
        
    e.dropComplete(true);
	}
  	
  public void mousePressed(MouseEvent e)
  {
  	Object object = e.getSource();
  	FileLabel file_label;
  	Iterator<FileLabel> itr;
  	
  	if(object instanceof FileLabel){
  		file_label = (FileLabel)object;
  		
  		if(selected_files.contains(file_label)){
  			selected_files.remove(file_label);
  			file_label.setDeselected();
  		}else{
  			selected_files.add(file_label);
  			file_label.setSelected();
  		}
  	}else{
  		itr = selected_files.iterator();
  		
  		while(itr.hasNext()){
  			itr.next().setDeselected();
  		}
  		
  		selected_files.clear();
  	}
  	
  	repaint();
  }

  public void dragEnter(DropTargetDragEvent e) {}
	public void dragExit(DropTargetEvent e) {}
	public void dragOver(DropTargetDragEvent e) {}
	public void dropActionChanged(DropTargetDragEvent e) {}
	public void mouseClicked(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mouseReleased(MouseEvent e) {}	
	
	/**
	 * Start the Polyglot file manager, essentially an instance of a PolyglotPanel.
	 * @param args command line arguments
	 */
	public static void main(String args[])
	{
		/*
		try{
      UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
		}catch(Exception e) {e.printStackTrace();}
  	*/
		
		PolyglotPanel polyglot_panel = new PolyglotPanel();
		
    JFrame frame = new JFrame("Polyglot File Manager");
    frame.setSize(600, 600);
    frame.add(polyglot_panel);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 
    frame.setVisible(true);
	}
}
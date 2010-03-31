package edu.ncsa.icr;
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
 * A program for creating AHK scripts (monkey see, monkey do)
 * @author Kenton McHenry
 */
public class ICRMonkey extends JPanel implements Runnable, DropTargetListener, MouseListener
{
	private TreeSet<FileLabel> files = new TreeSet<FileLabel>();
	private FileLabel selected_file;
  private Graphics bg;
  private Image offscreen;
  
	public ICRMonkey()
	{
		FlowLayout flow_layout = new FlowLayout(FlowLayout.LEFT);
		flow_layout.setHorizontalLayout(false);
		setLayout(flow_layout);

    setBackground(new Color(0x003a6ea5));
    new DropTarget(this, this);
    addMouseListener(this);
    
    new Thread(this).start();
	}
	
	/**
	 * Background thread to record user interaction.
	 */
	public void run()
	{
		//Create invisible window the size of the screen
		
		while(true){
			Point point = MouseInfo.getPointerInfo().getLocation();
			System.out.println(point.x + ", " + point.y);
			Utility.pause(1000);
		}
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
  	
  /**
   * Handle mouse pressed events.
   * @param e the mouse event
   */
  public void mousePressed(MouseEvent e)
  {
  	Object object = e.getSource();
  	FileLabel file_label;
  	Iterator<FileLabel> itr;
  	
  	//De-select previous select
		if(selected_file != null){
			selected_file.setDeselected();
			selected_file = null;
		}
		
  	if(object instanceof FileLabel){
  		selected_file = (FileLabel)object;
  		selected_file.setSelected();
  	}
  	
  	repaint();
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
	        FileLabel file_label;
	        String text;
	        
	        while(itr.hasNext()){
	        	file = itr.next();
	        	text = Utility.getFilenameName(file.getName());
	        	if(text.length() > 8) text = text.substring(0, 8) + "...";
	        	file_label = new FileLabel(file, text, this);
	        	
	        	if(!files.contains(file_label)){
	        		file_label.setLargeIcon();
	        		file_label.setForeground(Color.white);
	        		file_label.setSelectionColor(0x00002163);
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

	public void dragEnter(DropTargetDragEvent e) {}
	public void dragExit(DropTargetEvent e) {}
	public void dragOver(DropTargetDragEvent e) {}
	public void dropActionChanged(DropTargetDragEvent e) {}
	public void mouseClicked(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mouseReleased(MouseEvent e) {}	
	
	/**
	 * Start the ICRMonkey.
	 * @param args command line arguments
	 */
	public static void main(String args[])
	{
		ICRMonkey polyglot_panel = new ICRMonkey();
		
    JFrame frame = new JFrame("ICR Monkey Desktop");
    frame.setSize(600, 600);
    frame.add(polyglot_panel);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 
    frame.setVisible(true);
	}
}
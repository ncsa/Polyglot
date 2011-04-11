package edu.ncsa.icr.polyglot;
import edu.ncsa.utility.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;
import java.util.*;

/**
 * A panel with a file manager look and feel designed for convenient file format conversions.
 * @author Kenton McHenry
 */
public class PolyglotPanel extends FilePanel
{
	private Polyglot polyglot = null;
  
  /**
   * Class constructor.
   * @param filename the name of a *.ini file
   */
	public PolyglotPanel(JFrame frame, String filename)
	{   		
		super(frame);
		loadINI(filename);
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
	          }else if(key.equals("SoftwareServer")){
	          	if(polyglot == null || !(polyglot instanceof PolyglotSteward)){
	          		polyglot = new PolyglotSteward();
	          	}
	          	
          		tmpi = value.lastIndexOf(':');
	        		
	        		if(tmpi != -1){
	        			server = value.substring(0, tmpi);
	        			port = Integer.valueOf(value.substring(tmpi+1));
	        			((PolyglotSteward)polyglot).add(server, port);
	        		}
	          }else if(key.equals("PolyglotServer")){
          		tmpi = value.lastIndexOf(':');
	        		
	        		if(tmpi != -1){
	        			server = value.substring(0, tmpi);
	        			port = Integer.valueOf(value.substring(tmpi+1));
	        			polyglot = new PolyglotClient(server, port);
	        		}
	          }
	        }
	      }
	    }
	    
	    ins.close();
	  }catch(Exception e){}
	}
	
	/**
   * Set the popup menu.
   */
  protected void setPopupMenu()
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
	
	/**
	 * Close polyglot connections.
	 */
	public void close()
	{
		if(polyglot != null) polyglot.close();
	}

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
package edu.ncsa.utility;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * A transparent JFrame.
 * @author Kenton McHenry
 */
public class TransparentJFrame extends JFrame implements WindowListener, Runnable
{
	private TransparentJPanel panel = new TransparentJPanel();
	private boolean PASSED_INITIAL_REFRESH = false;
  private boolean REFRESH = false;
  
  /**
   * Class constructor.
   */
	public TransparentJFrame()
	{
    getContentPane().add("Center", panel);
    addComponentListener(panel);
    addWindowListener(this);
    new Thread(this).start();
	}
	
	/**
	 * Class constructor.
	 * @param title the frame's title
	 */
	public TransparentJFrame(String title)
	{
		this();
		setTitle(title);
	}
	
	/**
	 * Get the transparent JPanel used by the JFrame
	 * @return the transparent JPanel used
	 */
	public TransparentJPanel getPanel()
	{
		return panel;
	}
	
	/**
	 * Add a component to the JFrame.
	 * @param comp the component to add
	 */
	public Component add(Component comp)
	{
		return panel.add(comp);
	}
  
	/**
	 * Handle window activated events.
	 * @param e the window event
	 */
	public void windowActivated(WindowEvent e)
	{
		REFRESH = true;
	}
	
	/**
	 * Handle window deactivated events.
	 * @param e the window event
	 */
	public void windowDeactivated(WindowEvent e) 
	{
		panel.useBackgroundImage(false);
	}	
	
	public void windowIconified(WindowEvent e) {}
	public void windowDeiconified(WindowEvent e) {}
	public void windowClosed(WindowEvent e) {}
	public void windowClosing(WindowEvent e) {}
	public void windowOpened(WindowEvent e) {}

	/**
	 * Refresh the background image as needed.
	 */
	public void run()
	{  	
	  while(true){
	    if(REFRESH){
	    	if(PASSED_INITIAL_REFRESH){	      	
	        setVisible(false);
	        Utility.pause(200);		//Wait for Windows Aero animations to finish
	        
	        panel.updateBackgroundImage();
	    		panel.useBackgroundImage(true);
	    		
	        setVisible(true);
	        Utility.pause(200);		//Let some time pass to prevent responding to the show operation
	    	}else{
	    		PASSED_INITIAL_REFRESH = true;
	    	}
	      
	      REFRESH = false;
	    }else{
	    	Utility.pause(200);
	    }
	  }
	}

	/**
	 * A simple main for debug purposes.
	 * @param args command line arguments
	 */
	public static void main(String args[])
	{
		TransparentJFrame frame = new TransparentJFrame();
		
	  frame.setSize(600, 400);
	  frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	  frame.getPanel().setBackground(0x000000ff);
	  frame.getPanel().setAlpha(0.5f);
	  frame.getPanel().setLayout(new FlowLayout());
	  frame.add(new JButton("Button1"));
	  frame.add(new JButton("Button2"));
	  frame.setVisible(true);
	}
}
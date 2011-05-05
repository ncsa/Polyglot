package kgm.utility;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * A transparent JPanel.  Background updates must be triggered externally!
 * Note: A JFrame isn't used as a paintComponent method is required to draw the 
 * background without drawing over added buttons.
 * @author Kenton McHenry
 */
public class TransparentJPanel extends JPanel implements ComponentListener
{
	private BufferedImage background;
	private BufferedImage background_alpha = null;
	private Float alpha = null;
	private boolean USE_BACKGROUND_IMAGE = true;
	private Robot robot = null;

	/**
	 * Class constructor.
	 */
	public TransparentJPanel()
	{
		try{
			robot = new Robot();
		}catch(Exception e) {e.printStackTrace();}
		
		setBackground(new Color(0x00ffffff));
		updateBackgroundImage();
    addComponentListener(this);
	}
	
	/**
	 * Set the background color when the background image is not used.
	 * @param color the background color in ARGB
	 */
	public void setBackground(int color)
	{
		super.setBackground(new Color(color));
	}
	
	/**
	 * Set the alpha component of the background image with regards to the background color.
	 * @param alpha the alpha value to use
	 */
	public void setAlpha(float alpha)
	{
		this.alpha = alpha;
		updateAlphaImage();
	}
	
	/**
	 * Update the background image.
	 */
	public void updateBackgroundImage()
	{
	  Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
	  background = robot.createScreenCapture(new Rectangle(0,0,(int)dimension.getWidth(),(int)dimension.getHeight()));
	  if(alpha != null) updateAlphaImage();
	}
	
	/**
	 * Update the background image containing an alpha value.
	 */
	private void updateAlphaImage()
	{
		Graphics2D g;
		
		background_alpha = new BufferedImage(background.getWidth(null), background.getHeight(null), BufferedImage.TRANSLUCENT);
		g = background_alpha.createGraphics();
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
		g.drawImage(background, null, 0, 0);
		g.dispose();
	}
	
	/**
	 * Set whether or not to use the background image.
	 * @param value true if the background image should be used
	 */
	public void useBackgroundImage(boolean value)
	{
		USE_BACKGROUND_IMAGE = value;
		repaint();
	}

	/**
	 * Draw the panel.
	 * @param g the graphics context to draw to
	 */
	public void paintComponent(Graphics g)
	{
    Point position = this.getLocationOnScreen();
    Point offset = new Point(-position.x, -position.y);
    
    super.paintComponent(g);
    
    if(USE_BACKGROUND_IMAGE){
    	if(background_alpha != null){
    		g.drawImage(background_alpha, offset.x, offset.y, null);
    	}else{
    		g.drawImage(background, offset.x, offset.y, null);
    	}
    }
	}
	
	/**
	 * Handle component shown events.
	 * @param e the component event
	 */
	public void componentShown(ComponentEvent e)
	{
		repaint();
	}
	
	/**
	 * Handle component resized events.
	 * @param e the component event
	 */
  public void componentResized(ComponentEvent e)
  {
  	repaint();
  }
  
	/**
	 * Handle component moved events.
	 * @param e the component event
	 */
  public void componentMoved(ComponentEvent e)
  {
  	repaint();
  }
  
  public void componentHidden(ComponentEvent e) {}

	/**
	 * A simple main for debug purposes.
	 * @param args command line arguments
	 */
	public static void main(String args[])
	{
		JFrame frame = new JFrame();
		TransparentJPanel panel = new TransparentJPanel();
		
    panel.setLayout(new FlowLayout());
    panel.add(new JButton("Button1"));
    panel.add(new JButton("Button2"));		
		
    frame.setSize(600, 400);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.getContentPane().add("Center", panel);
    frame.addComponentListener(panel);
    frame.setVisible(true);
	}
}
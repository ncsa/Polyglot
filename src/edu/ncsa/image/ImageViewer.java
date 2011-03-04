package edu.ncsa.image;
import edu.ncsa.utility.*;
import edu.ncsa.matrix.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.swing.*;
import java.util.*;

/**
 * A modified JFrame that allows for easily poping up windows containing images based on 
 * arrays, matrices, or image data.
 * @author Kenton McHenry
 */
public class ImageViewer extends JFrame implements Runnable, MouseListener, MouseWheelListener
{
  private Vector<Object> data = new Vector<Object>();
  private Vector<Pair<Integer,Integer>> wh = new Vector<Pair<Integer,Integer>>();
  private int index = 0;
  
  private BufferedImage image = null;
  private Vector<int[]> pixels = new Vector<int[]>();
  private int width = 600;
  private int height;
  private Object data_lock = new Object();
  private Object image_lock = new Object();   //Prevent flickering while drawing frame
  private String title = "Image";
  private Thread t1;
  private boolean CLEAR_ON_ADD = false;
  
  private boolean WAIT_FOR_CLICK = false;
  private int clicked_button = 0;
  private int clicked_x = 0;
  private int clicked_y = 0;
  
  private int offx = 3;
  private int offy = 26;
  
  /**
   * Class constructor.
   * @param object the object containing image data
   * @param w the image width
   * @param h the image height
   * @param width the width of the frame (note: height is set automatically!)
   */
  public ImageViewer(Object object, int w, int h, int width)
  {
    super();
    
    if(width >= 0){
    	this.width = width;
    }
    
    if(object instanceof Vector){
      for(int i=0; i<((Vector<Object>)object).size(); i++){
        add(((Vector<Object>)object).get(i), w, h, false);
      }
    }else{
      add(object, w, h, false);
    }
    
    addMouseListener(this);
    addMouseWheelListener(this);
    setResizable(false);
    setVisible(true);
    
    t1 = new Thread(this);
    t1.start();
  }
  
  /**
	 * Class constructor.
	 * @param object the object containing image data
	 * @param w the image width
	 * @param h the image height
	 * @param width the width of the frame (note: height is set automatically!)
	 * @param title the frames title
	 */
	public ImageViewer(Object object, int w, int h, int width, String title)
	{
	  this(object, w, h, width);
	  this.title = title;
	  setTitle(this.title + "  [" + w + "x" + h + "]");
	}
	
  /**
	 * Class constructor.
	 * @param object the object containing image data
	 * @param w the image width
	 * @param h the image height
	 * @param title the frames title
	 */
	public ImageViewer(Object object, int w, int h, String title)
	{
	  this(object, w, h, -1);
	  this.title = title;
	  setTitle(this.title + "  [" + w + "x" + h + "]");
	}

	/**
   * Class constructor.
   * @param object the object containing image data
   * @param w the image width
   * @param h the image height
   */
  public ImageViewer(Object object, int w, int h)
  {
  	this(object, w, h, -1);
  }
  
  /**
   * Class constructor.
   * @param object the object containing image data (must include width/height values)
   * @param title the frames title
   */
  public ImageViewer(Object object, String title)
  {
    this(object, 0, 0, -1, title);
  }

  /**
	 * Class constructor.
	 */
	public ImageViewer(String title)
	{
	  this(new int[100*100], 100, 100, -1, title);
	  CLEAR_ON_ADD = true;
	}

	/**
   * Class constructor.
   * @param width the width of the frame (note: height is set automatically!)
   */
  public ImageViewer(int width)
  {
    this(new int[100*100], 100, 100, width);
    CLEAR_ON_ADD = true;
  }
  
	/**
   * Class constructor.
   */
  public ImageViewer()
  {
    this(new int[100*100], 100, 100, -1);
    CLEAR_ON_ADD = true;
  }
  
  /**
   * Get the data stored in this viewer.
   * @return the viewers image data
   */
  public Vector<Object> getData()
  {
    return data;  
  }
  
  /**
   * Get the number of images this viewer is storing.
   * @return the number of images the viewer is storing
   */
  public int getDataSize()
  {
    synchronized(data_lock){
      return data.size();
    }
  }
  
  /**
   * Add another image object to the frame.
   * @param object the object containing image data
   * @param DRAW true this image should be display right away
   */
  public void add(Object object, boolean DRAW)
  {
  	if(object instanceof double[][] || object instanceof int[][]){
  		add(object, -1, -1, DRAW);
  	}
  }
  
  /**
   * Add another image object to the frame.
   * @param object the object containing image data
   * @param w the image width
   * @param h the image height
   * @param DRAW true this image should be display right away
   */
  public void add(Object object, int w, int h, boolean DRAW)
  {
    synchronized(data_lock){
      if(CLEAR_ON_ADD){
        data.clear();
        wh.clear();
        pixels.clear();
        CLEAR_ON_ADD = false;
      }
      
      if(object instanceof BufferedImage){
        BufferedImage image = (BufferedImage)object;
        data.add(image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth()));
        wh.add(new Pair<Integer,Integer>(image.getWidth(), image.getHeight()));
      }else if(object instanceof double[][]){
        double[][] img = (double[][])object;
        data.add(MatrixUtility.to1D(img));
        wh.add(new Pair<Integer,Integer>(img[0].length, img.length));
      }else if(object instanceof int[][]){
      	int[][] img = (int[][])object;
      	data.add(ImageUtility.to1D(img));
      	wh.add(new Pair<Integer,Integer>(img[0].length, img.length));
      }else{
        data.add(object);    
        wh.add(new Pair<Integer,Integer>(w, h));
      }
      
      pixels.add(null);
      
      if(data.size() == 1){
        index = 0;
        draw(0);
      }else if(DRAW){
        index = data.size() - 1;
        draw(index);
      }else{
        setTitle(this.title + ": " + (index+1) + " of " + data.size() + "  [" + wh.get(index).first + "x" + wh.get(index).second + "]");
      }
    }
  }
  
  /**
   * Set the image object, clearing all previously added data.
   * @param object the object containing image data
   * @param w the image width
   * @param h the image height
   */
  public void set(Object object, int w, int h)
  {
  	CLEAR_ON_ADD = true;
  	add(object, w, h, true);
  }
  
  /**
   * This is a helper function to set the pixels of the image that is displayed in the frame
   * with the data stored in the given object.
   * @param object the object containing image data
   * @param w the image width
   * @param h the image height
   */
  private void draw(int index)
  {
    Object object = data.get(index);
    int w = wh.get(index).first;
    int h = wh.get(index).second;
    int[] img = null;
    
    synchronized(image_lock){
      height = (int)Math.round(((double)width)*((double)h)/((double)w));
      
      if(pixels.get(index) == null){
        if(object instanceof double[]){
          img = ImageUtility.g2argb((double[])object, w, h);
        }else if(object instanceof int[]){
          img = (int[])object;
        }
        
        //img = ImageUtility.smooth(img, w, h, 1);
        pixels.set(index, ImageUtility.resize(img, w, h, width, height));
      }
      
      image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
      image.setRGB(0, 0, width, height, pixels.get(index), 0, width);
      setSize(width, height+30);
      setTitle(this.title + ": " + (index+1) + " of " + data.size() + "  [" + w + "x" + h + "]");
    }
  }
  
  /**
   * Draw a box on the current image.
   * @param color the color of the point in ARGB
   */
  public void drawBox(Pixel p, int r, int color)
  {
  	synchronized(image_lock){
  		ImageUtility.drawBox(pixels.get(index), width, height, p, r, color);
  	}
  
  	draw(index);
  }
  
  /**
   * Draw the stored image data to the given graphics context
   * @param g the graphics context to draw to
   */
  public void paint(Graphics g)
  {
    if(image != null) g.drawImage(image, offx, offy, null);
  }
  
  /**
   * Listener for mouse pressed events.
   * @param e the mouse event
   */
  public void mousePressed(MouseEvent e)
  {  	
    if(e.getButton() == MouseEvent.BUTTON1){
      clicked_button = 1;
      clicked_x = e.getX() - offx;
      clicked_y = e.getY() - offy;
      WAIT_FOR_CLICK = false;
    }
  }
  
  /**
   * Mouse wheel listener that handles changing the currently displayed image (if several have been added).
   * @param e the mouse wheel event
   */
  public void mouseWheelMoved(MouseWheelEvent e)
  {  	
    if(e.getWheelRotation() < 0){
      index++;
    }else{
      index--;
    }
    
    synchronized(data_lock){
      if(index < 0){
        index = data.size() - 1;
      }else if(index == data.size()){
        index = 0;
      }
      
      draw(index);
    }
  }
  
  public void mouseExited(MouseEvent e) {}
  public void mouseEntered(MouseEvent e) {}
  public void mouseClicked(MouseEvent e) {}
  public void mouseReleased(MouseEvent e){}
  
  /**
   * Wait for a mouse click in the image and return its coordinates.
   * @return the clicked point
   */
  public Pixel getClick()
  {
  	WAIT_FOR_CLICK = true;
  	
  	while(WAIT_FOR_CLICK){
  		Utility.pause(100);
  	}
  	
  	return new Pixel(clicked_x, clicked_y);
  }
  
  /**
   * The starting point for the automatically started thread that continuosly updates the frame.
   */
  public void run()
  {  
    while(true){
      try{
        synchronized(image_lock){
          paint(this.getGraphics());
        }
        
        Thread.sleep(50);
      }catch(Exception e){}
    }
  }
  
  /**
	 * A helpful method to create a new ImageViewer to show an image.
	 * @param object the object containing image data
	 * @param title the title of the frame
	 * @return the image viewer created
	 */
	public static ImageViewer show(Object object, String title)
	{
	  return new ImageViewer(object, title); 
	}

	/**
   * A helpful method to create a new ImageViewer to show an image.
   * @param object the object containing image data
   * @param w the width of the image
   * @param h the height of the image
   * @return the image viewer created
   */
  public static ImageViewer show(Object object, int w, int h)
  {
    return new ImageViewer(object, w, h);
  }
  
  /**
   * A helpful method to create a new ImageViewer to show an image.
   * @param object the object containing image data
   * @param w the width of the image
   * @param h the height of the image
   * @param title the title of the frame
   * @return the image viewer created
   */
  public static ImageViewer show(Object object, int w, int h, String title)
  {
    return new ImageViewer(object, w, h, title);
  }
  
  /**
   * A helpful method to create a new ImageViewer to show an image.
   * @param object the object containing image data
   * @param w the width of the image
   * @param h the height of the image
   * @param width the width of the frame (note: height is set automatically!)
   * @param title the title of the frame
   * @return the image viewer created
   */
  public static ImageViewer show(Object object, int w, int h, int width, String title)
  {
    return new ImageViewer(object, w, h, width, title);
  }
}
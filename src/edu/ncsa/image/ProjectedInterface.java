package edu.ncsa.image;
import java.awt.*;
import javax.swing.*;

/**
 * TODO: A projected interface utilizing a small projector and a webcam to allow for display interaction.
 * @author Kenton McHenry
 */
public class ProjectedInterface implements Runnable
{	
	private Rectangle screen = null;
	
	private int bt_thickness = 20;
	private JWindow bt_top = new JWindow();
	private JWindow bt_bottom = new JWindow();
	private JWindow bt_left = new JWindow();
	private JWindow bt_right = new JWindow();
		
	/**
	 * Class constructor.
	 * @param screen_number the screen to use
	 */
	public ProjectedInterface(int screen_number)
	{
		screen = ImageUtility.getScreenRectangle(screen_number);
		
		if(screen != null){
			//Setup border target
			bt_top.setLocation(screen.x, 0);
			bt_top.setSize(screen.width, bt_thickness);
			bt_top.getContentPane().setBackground(Color.red);
			
			bt_bottom.setLocation(screen.x, screen.height-bt_thickness);
			bt_bottom.setSize(screen.width, bt_thickness);
			bt_bottom.getContentPane().setBackground(Color.green);
			
			bt_left.setLocation(screen.x, 0);
			bt_left.setSize(bt_thickness, screen.height);
			bt_left.getContentPane().setBackground(Color.red);
			
			bt_right.setLocation(screen.x+screen.width-bt_thickness, 0);
			bt_right.setSize(bt_thickness, screen.height);
			bt_right.getContentPane().setBackground(Color.green);
		}			
		
		new Thread(this).start();

	}
	
	/**
	 * Enable/disable the border target.
	 * @param value true if the border target should be drawn
	 */
	public void enableBorderTarget(boolean value)
	{
		bt_top.setVisible(value);
		bt_bottom.setVisible(value);
		bt_left.setVisible(value);
		bt_right.setVisible(value);
	}
	
	/**
	 * Find the projections and watch for interactions.
	 */
	public void run()
	{
		WebCam camera = null;
		int[][] camera_image = null;
		int[][] desktop_image = null;
		double[][] camera_redness = null;
		double[][] camera_greenness = null;
		ImageViewer viewer1 = new ImageViewer();
		ImageViewer viewer2 = new ImageViewer(); viewer2.setLocation(viewer1.getWidth()+10, 0);
		
		if(screen != null){
			camera = new WebCam();
		}else{
			camera_image = ImageUtility.load("C:/Kenton/Data/Images/Temp/ProjectedInterface0/target_camera.jpg");
			desktop_image = ImageUtility.load("C:/Kenton/Data/Images/Temp/ProjectedInterface0/target_desktop.jpg");
		}
		
		while(true){
			enableBorderTarget(true);
			
			if(screen != null){
				camera_image = camera.grab();
				desktop_image = ImageUtility.getScreen(screen);
			}

			if(camera_image != null){	
			  camera_redness = similarity(camera_image, 1, 0, 0, 1);
			  camera_greenness = similarity(camera_image, 0, 1, 0, 0.3);
			  
				if(true){		//Debug
					//ImageUtility.save("camera.jpg", camera_image);
					//ImageUtility.save("desktop.jpg", desktop_image);
					
					viewer1.set(camera_image, camera_image[0].length, camera_image.length);
					//viewer2.set(desktop_image, desktop_image[0].length, desktop_image.length);
					viewer2.set(ImageUtility.g2argb(camera_greenness), camera_image[0].length, camera_image.length);
				}
			}
		}
	}
	
	/**
	 * Find the similarity of each color pixel with the given color.
	 * @param Irgb a color image
	 * @param r0 the red component of the color to find
	 * @param g0 the green component of the color to find
	 * @param b0 the blue component of the color to find
	 * @param sigma the standard deviation to use when converting distance to similarity
	 * @return a gray scale image indicating pixel similarities to the given color
	 */
	public static double[][] similarity(int[][] Irgb, double r0, double g0, double b0, double sigma)
	{
		int h = Irgb.length;
		int w = Irgb[0].length;
		double[][] Ig = new double[h][w];
		double sigmasigma = sigma * sigma;
		int rgb;
		double r, g, b;
		double d;
		
		for(int y=0; y<h; y++){
			for(int x=0; x<w; x++){
				rgb = Irgb[y][x];
				
				r = (rgb >> 16) & 0x000000ff;
				g = (rgb >> 8) & 0x000000ff;
				b = rgb & 0x000000ff;
				
				r /= 255.0;
				g /= 255.0;
				b /= 255.0;
				
				r -= r0;
				g -= g0;
				b -= b0;
				
				d = Math.sqrt(r*r + g*g + b*b);
				
				Ig[y][x] = Math.exp(-(d*d)/(2*sigmasigma));
			}
		}
		
		return Ig;
	}

	/**
   * A simple main for debug purposes.
   * @param args the command line arguments
   */
	public static void main(String args[])
	{
		ProjectedInterface pi = new ProjectedInterface(-1);
	}
}
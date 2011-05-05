package kgm.image;
import javax.media.*;
import javax.media.control.*;
import javax.media.format.VideoFormat;
import javax.media.util.BufferToImage;
import java.awt.*;
import java.awt.image.*;
import java.util.*;

/**
 * An instance of a web cam.
 * @author Kenton McHenry
 */
public class WebCam
{
	private Player player = null;
	private FrameGrabbingControl frame_grabber = null;

	/**
	 * Class constructor, initializes camera connection to first VFW device.
	 */
	public WebCam()
	{
		Vector<CaptureDeviceInfo> devices = CaptureDeviceManager.getDeviceList(null);
		CaptureDeviceInfo cdi = null;
		
		for(Iterator<CaptureDeviceInfo> i=devices.iterator(); i.hasNext();){
			cdi = i.next();
			if(cdi.getName().startsWith("vfw:")) break;
		}
		
		if(cdi != null){
			try{
				player = Manager.createRealizedPlayer(cdi.getLocator());
				player.start();
			}catch(Exception e){
				e.printStackTrace();
			}
			
			frame_grabber = (FrameGrabbingControl)player.getControl("javax.media.control.FrameGrabbingControl");

			if(false){	//View available formats
				FormatControl fc = (FormatControl)player.getControl("javax.media.control.FormatControl");
				Format[] formats = fc.getSupportedFormats();
				
				System.out.println("Enabled format: " + fc.getFormat() + "\n");
				System.out.println("Supported formats: ");
				
				for(int i=0; i<formats.length; i++){
					System.out.println(formats[i]);
				}
			}
		}else{
			System.out.println("No cameras found!");
		}
	}
	
	/**
	 * Grab an image from the active camera.
	 * @return a 2D integer array containing the image in ARGB format
	 */
	public int[][] grab()
	{
		if(frame_grabber != null){
			Image image;
			BufferedImage buffered_image;
			Graphics2D g;
			Buffer buffer = frame_grabber.grabFrame();
			
			while(buffer.getFormat() == null){	//Wait for first frame
				buffer = frame_grabber.grabFrame();
			}
						
			image = (new BufferToImage((VideoFormat)buffer.getFormat())).createImage(buffer);
			buffered_image = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_RGB);
			g = buffered_image.createGraphics();
			g.drawImage(image, null, null);
			g.dispose();

	    return ImageUtility.image2argb(buffered_image);
		}else{
			return null;
		}
	}
	
	/**
	 * A simple main for debug purposes.
	 * @param args input arguments (not used)
	 */
	public static void main(String args[])
	{
		ImageViewer viewer = new ImageViewer();
		WebCam camera = new WebCam();
		int[][] Irgb;
		
		while(true){
			Irgb = camera.grab();
			if(Irgb != null) viewer.set(Irgb, Irgb[0].length, Irgb.length);
		}
	}
}
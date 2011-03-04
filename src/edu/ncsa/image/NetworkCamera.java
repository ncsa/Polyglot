package edu.ncsa.image;
import java.awt.image.*;
import java.io.*;
import java.net.*;
import com.sun.image.codec.jpeg.*;

/**
 * An instance of a network camera.
 * @author Kenton McHenry
 */
public class NetworkCamera
{ 
	private String url = "";
	private String username = "";
	private String password = "";
	private HttpURLConnection huc = null;
  private DataInputStream dis;
  private boolean AXIS_CAMERA = true;
  private boolean USE_MJPG = true;
  private boolean CONNECTED = false;
  
	/**
	 * Class constructor.
	 * @param url the URL where to access the image data
	 * @param username the user name to use
	 * @param password the password to use
	 * @param AXIS_CAMERA true if this is this an AXIS camera
	 * @param USE_MJPG true if the given URL is for motion JPEG
	 */
	public NetworkCamera(String url, String username, String password, boolean AXIS_CAMERA, boolean USE_MJPG)
	{
		this.url = url;
		this.username = username;
		this.password = password;
		this.AXIS_CAMERA = AXIS_CAMERA;
		this.USE_MJPG = USE_MJPG;
		
		connect();
	}
	
  /**
   * Encode user name and password in base 64 for a web request.
   * @param username the user name
   * @param password the password
   * @return the encoded user name and password
   */
  private String encodeUsernameAndPasswordInBase64(String username, String password)
  {
    String string = username + ":" + password;
    string = new sun.misc.BASE64Encoder().encode(string.getBytes());
    
    return "Basic " + string;
  }
	
  /**
	 * Read a line from a data input stream.
	 * @param dis the data input stream
	 */
	private void readLine(DataInputStream dis)
	{
	  try{
	    String delimiter = "\n";    //Assumes that the end of the line is marked with this
	    int delimiter_bytes = delimiter.getBytes().length;
	    byte[] buffer = new byte[delimiter_bytes];
	    String line = "";
	    boolean FOUND = false;
	    
	    while(!FOUND){
	      dis.read(buffer, 0, delimiter_bytes);
	      line = new String(buffer);
	      //System.out.print(line);
	      
	      if(line.equals(delimiter)) FOUND = true;
	    }
	  }catch(Exception e) {e.printStackTrace();}
	}

	/**
	 * Read lines from the given data input stream.
	 * @param n the number of lines to read
	 * @param dis the data input stream
	 */
	private void readLine(DataInputStream dis, int n)
	{
	  //Used to strip out the header lines
	  for(int i=0; i<n;i++){
	    readLine(dis);
	  }
	}

	/**
   * Connect to the network camera.
   */
  public void connect()
  {
    try{
      huc = (HttpURLConnection)(new URL(url)).openConnection();
      huc.setRequestProperty("Authorization", encodeUsernameAndPasswordInBase64(username, password));
      InputStream is = huc.getInputStream();
      CONNECTED = true;
      
      BufferedInputStream bis = new BufferedInputStream(is);
      dis = new DataInputStream(bis);
    }catch(IOException e){  //If no connection exists wait and try again
      try{
        huc.disconnect();
        Thread.sleep(60);
      }catch(InterruptedException ie){
        huc.disconnect();
        connect();
      }
      
      connect();
    }catch(Exception e) {e.printStackTrace();}
  }

  /**
   * Disconnect from the network camera.
   */
  public void disconnect()
  {
    try{
      if(CONNECTED){
        dis.close();
        CONNECTED = false;
      }
    }catch(Exception e) {e.printStackTrace();}
  }

  /**
	 * Grab an image from the camera.
	 * @return a 2D integer array containing the image in ARGB format
	 */
	public int[][] grab()
	{
    try{    			
    	BufferedImage image = null;

      if(USE_MJPG){					//Remove the MJPG encapsulation
        readLine(dis, 4);		//Discard the first 4 lines
        image = JPEGCodec.createJPEGDecoder(dis).decodeAsBufferedImage();
        
        if(AXIS_CAMERA){
          readLine(dis, 1);	//Discard one line 
        }else{
          readLine(dis, 2);	//Discard the last two lines
        }   
      }else{
        connect();
        image = JPEGCodec.createJPEGDecoder(dis).decodeAsBufferedImage();
        disconnect();
      }
      
      return ImageUtility.image2argb(image);
    }catch(Exception e) {e.printStackTrace();}
    
    return null;
	}
	
	/**
	 * A simple main for debug purposes.
	 * @param args input arguments (not used)
	 */
	public static void main(String args[])
	{	  
		NetworkCamera camera;
		ImageViewer viewer = new ImageViewer();
		int[][] Irgb;
		
		//D-Link DCS-900 images
		//camera = new NetworkCamera("http://141.142.210.65:81/IMAGE.JPG", "user1", "pw1", false, false);
		
		//D-Link DCS-900 video
		//camera = new NetworkCamera("http://141.142.210.65:81/video.cgi", "user1", "pw1", false, true);

		//Axis 212 PTZ images
		//camera = new NetworkCamera("http://141.142.210.65:81/axis-cgi/jpg/image.cgi?resolution=640x480", "user1", "pw1", true, false);

		//Axis 212 PTZ video
		camera = new NetworkCamera("http://141.142.210.65:81/axis-cgi/mjpg/video.cgi?resolution=640x480", "user1", "pw1", true, true);

		while(true){
			Irgb = camera.grab();
			if(Irgb != null) viewer.set(Irgb, Irgb[0].length, Irgb.length);
		}
	}
}
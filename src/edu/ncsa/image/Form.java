package edu.ncsa.image;
import edu.ncsa.matrix.*;
import edu.ncsa.utility.*;
import ncsa.im2learn.core.datatype.*;
import ncsa.im2learn.core.io.*;
import java.io.*;
import java.util.*;
import org.apache.log4j.*;

/**
 * An interface to data contained within imaged forms.
 * @author Kenton McHenry
 */
public class Form
{
	private int[][] image_rgb;
	private double[] image_g;
	int width, height;
	
	/**
	 * Class constructor.
	 * @param filename the name of the image to load
	 */
	public Form(String filename)
	{
		if(false){
			image_rgb = ImageUtility.load(filename);
			image_g = ImageUtility.argb2g(image_rgb);
			width = image_rgb[0].length;
			height = image_rgb.length;		
		}else{
			try{
				Logger.getRootLogger().setLevel(Level.OFF);
				ImageObject image_object = ImageLoader.readImage(filename);
				byte[] bytes = (byte[])image_object.getData();
				int value, at;
				
				width = image_object.getNumCols();
				height = image_object.getNumRows();
				image_rgb = new int[height][width];
				image_g = new double[width*height];

				for(int x=0; x<width; x++){
					for(int y=0; y<height; y++){
						at = y*width + x;
						value = bytes[at] & 0xff;
						image_rgb[y][x] = (value << 16) | (value << 8) | value;
						image_g[at] = value / 255.0;
					}
				}
			}catch(Exception e) {e.printStackTrace();}
			
			if(false){			//Crop out margins
				int margin = 600;
				
				image_rgb = ImageUtility.crop(image_rgb, margin, margin, width-2*margin, height-2*margin);
				image_g = ImageUtility.crop(image_g, width, height, margin, margin, width-2*margin, height-2*margin);
				width = (width-2*margin)-margin+1;
				height = (height-2*margin)-margin+1;
			}
		}
		
		if(true){				//Binarize grayscale image
			for(int i=0; i<image_g.length; i++){
				if(image_g[i] < 0.5){
					image_g[i] = 1;
				}else{
					image_g[i] = 0;
				}
			}			
		}
	}
	
	/**
	 * Display the form.
	 */
	public void show()
	{
		ImageViewer viewer = new ImageViewer(image_rgb, width, height, 1200, "Form");
		viewer.add(image_g, width, height, true);
	}
	
	/**
	 * Scale the form.
	 * @param scale the scale factor
	 */
	public void scale(double scale)
	{
		image_rgb = ImageUtility.resize(image_rgb, scale);
		image_g = MatrixUtility.to1D(ImageUtility.resize(MatrixUtility.to2D(height, width, image_g), scale));
		height = image_rgb.length;
		width = image_rgb[0].length;		
	}
	
	/**
	 * De-skew a scanned form.
	 */
	public void unrotate()
	{
		Vector<Pair<Double,Double>> lines;
		double theta;
				
		ImageUtility.thin(image_g, width, height);
		lines = JavaCVUtility.getLines(image_g, width, height, 0.5, 450);
		
		if(true){		//Debug
			int[] image_rgb1d = ImageUtility.to1D(image_rgb);
			Vector<Pixel[]> line_segments = JavaCVUtility.getLineSegments(lines, width, height);

			System.out.println("Lines: " + lines.size());
			ImageUtility.drawLines(image_rgb1d, width, height, line_segments, 0x000000ff);		
			ImageViewer viewer = ImageViewer.show(image_g, width, height, 1200, "Lines");
			viewer.add(image_rgb1d, width, height, true);
			ImageUtility.save("tmp/output.jpg", ImageUtility.to2D(image_rgb1d, width, height));
		}
		
		//Determine skew based on found lines and un-rotate the image
		Histogram<Double> histogram = new Histogram<Double>(Histogram.createDoubleBins(0, 1, 180));
		
		for(int i=0; i<lines.size(); i++){
			histogram.add(lines.get(i).second * 180/Math.PI);
		}
		
		theta = histogram.mean(histogram.getValues(histogram.getMax()));
		theta = 90 - theta;
		System.out.println("Theta: " + theta);
		
		image_g = ImageUtility.rotate(image_g, width, height, theta, width/2, height/2);
	}

	/**
	 * Test if clustering y/x filter responses can indicate skew of the form.
	 */
	public void test1()
	{
  	double[][] F = ImageUtility.getFilter(ImageUtility.Option.EDGE, 0.5, 1, 0);
  	double[] image_gx = ImageUtility.convolve(image_g, width, height, F);
  	double[] image_gy = ImageUtility.convolve(image_g, width, height, MatrixUtility.transpose(F));
  	int at;
  	double dx, dy, slope;
  	
  	for(int i=0; i<image_gx.length; i++){
  		image_gx[i] = Math.abs(image_gx[i]);
  		image_gy[i] = Math.abs(image_gy[i]);
  	}
  	
  	ImageViewer viewer = ImageViewer.show(image_gx, width, height, 1200, "");
  	viewer.add(image_gy, width, height, true);
  	
  	if(false){
	    try{
	      FileWriter outs = new FileWriter("tmp/output.txt");
	      
	      for(int x=0; x<width; x++){
		  		for(int y=0; y<height; y++){
		  			at = y*width+x;
		  			dx = image_gx[at];
		  			dy = image_gy[at];
		  			slope = (dx != 0) ? dy/dx : 0;
		  			if(slope > 0) outs.write(Utility.round(slope,2) + "\n");
		  		}
		  	}  
	      
	      outs.close();
	    }catch(Exception e)	{}
  	}
	}
	
	/**
	 * Test Harris corners.
	 */
	public void test2()
	{
		if(true){
			image_rgb = ImageUtility.resize(image_rgb, 0.25);
			height = image_rgb.length;
			width = image_rgb[0].length;
		}
		
		int[] image_rgb1d = ImageUtility.to1D(image_rgb);
		double[] image_g = ImageUtility.argb2g(image_rgb);
	
		Vector<Pixel> corners = JavaCVUtility.getCorners(image_g, width, height, 10, 31, 0.04, 0);
	
		System.out.println("Corners: " + corners.size());
		ImageUtility.drawBoxes(image_rgb1d, width, height, corners, 5, 0x00ff0000);
		ImageViewer.show(image_rgb1d, width, height, 1200, "Corners");
		ImageUtility.save("tmp/output.jpg", ImageUtility.to2D(image_rgb1d, width, height));
	}

	/**
	 * A main for debug purposes.
	 * @param args the command line arguments
	 */
	public static void main(String args[])
	{
		String filename = "C:/Users/kmchenry/Files/Data/NARA/DataSets/1930Census-ChampaignCounty/illinoiscensus00reel410_0003.jpg";
		Form form = new Form(filename);
		form.scale(0.25);
		form.unrotate();
		//form.test1();
		//form.test2();
		form.show();
	}
}
package edu.ncsa.image;
import edu.ncsa.utility.*;
import java.awt.image.*;
import java.util.*;
import java.nio.*;
import com.googlecode.javacpp.*;
import com.googlecode.javacv.*;
import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_highgui.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.*;
import static com.googlecode.javacv.cpp.opencv_features2d.*;

/**
 * Image utilities utilizing the JavaCV interface to the OpenCV library. 
 * @author Kenton McHenry
 */
public class JavaCVUtility
{
	/**
	 * Convert a gray scale image to an IplImage.
	 * @param image the grayscale image
	 * @param w the width of the image
	 * @param h the height of the image
	 * @return the IplImage
	 */
	public static IplImage g2ipl(double[] image, int w, int h)
	{
		IplImage iplimage = IplImage.create(w, h, IPL_DEPTH_8U, 1);;
		byte[] bytes = new byte[image.length];
		
		for(int i=0; i<image.length; i++){
			bytes[i] = (byte)(int)Math.round(255*image[i]);
		}
		
		iplimage.getByteBuffer().put(bytes);
		
		return iplimage;
	}
	
	/**
	 * Convert an array of floats into an array of doubles.
	 * @param array an array of floats
	 * @return an array of doubles
	 */
	public static double[] f2d(float[] array)
	{
		double[] double_array = new double[array.length];
		
		for(int i=0; i<array.length; i++){
			double_array[i] = array[i];
		}
		
		return double_array;
	}

	/**
	 * Get image edges using the Canny method.
	 * @param image an image
	 * @param threshold1 the first threshold
	 * @param threshold2 the second threshold
	 * @param aperture the aperture size
	 * @return the edge map
	 */
	public static double[] getEdges(int[][] image, double threshold1, double threshold2, int aperture)
	{
		IplImage iplimage = IplImage.createFrom(ImageUtility.argb2image(image));
		IplImage iplgray = IplImage.create(iplimage.width(), iplimage.height(), IPL_DEPTH_8U, 1);
		IplImage ipledges = IplImage.create(iplimage.width(), iplimage.height(), iplgray.depth(), iplgray.nChannels());
		
		cvCvtColor(iplimage, iplgray, CV_RGB2GRAY);
		cvCanny(iplgray, ipledges, threshold1, threshold2, aperture);

		return ImageUtility.argb2g(ImageUtility.image2argb(ipledges.getBufferedImage()));
	}
	
	/**
	 * Get image edges using the Canny method.
	 * @param image an image
	 * @return the edge map
	 */
	public static double[] getEdges(int[][] image)
	{
		return getEdges(image, 600, 200, 3);
	}
	
	/**
	 * Get image lines using a Hough transform.
	 * @param image a grayscale image
	 * @param w the image width
	 * @param h the image height
	 * @param theta_resolution the resolution of angles to search for (in degrees)
	 * @param threshold the threshold
	 * @return the lines found
	 */
	public static Vector<Pair<Double,Double>> getLines(double[] image, int w, int h, double theta_resolution, int threshold)
	{
		Vector<Pair<Double,Double>> lines = new Vector<Pair<Double,Double>>();
		IplImage iplimage = g2ipl(image, w, h);
		CvSeq cvlines;
		FloatPointer cvline;
		double rho, theta;
		
		cvlines = cvHoughLines2(iplimage, CvMemStorage.create(), CV_HOUGH_STANDARD, 1, theta_resolution*Math.PI/180, threshold, 0, 0);
				
		for(int i=0; i<cvlines.total(); i++){
			cvline = new FloatPointer(cvGetSeqElem(cvlines,i));
			rho = cvline.position(0).get();
			theta = cvline.position(1).get();			
			lines.add(new Pair<Double,Double>(rho, theta));
		}
		
		return lines;
	}
	
	/**
	 * Get image bound line segments from lines represented as (rho, theta).
	 * @param lines the lines to convert into line segments within an image
	 * @param w the width of the image
	 * @param h the height of the image
	 * @return
	 */
	public static Vector<Pixel[]> getLineSegments(Vector<Pair<Double,Double>> lines, int w, int h)
	{
		Vector<Pixel[]> line_segments = new Vector<Pixel[]>();
		double rho, theta;
		double c, s;
		Pixel[] line;
		
		for(int i=0; i<lines.size(); i++){
			rho = lines.get(i).first;
			theta = lines.get(i).second;
			c = Math.cos(theta);
			s = Math.sin(theta);
			
			line = new Pixel[2];
			line[0] = new Pixel();
			line[1] = new Pixel();
			
			if(Math.abs(c) < 0.001){
				line[0].x = (int)Math.round(rho);
				line[0].y = 0;
				line[1].x = line[0].x;
				line[1].y = h-1;
			}else if(Math.abs(s) < 0.001){
				line[0].x = 0;
				line[0].y = (int)Math.round(rho);
				line[1].x = w-1;
				line[1].y = line[0].y;
			}else{
				line[0].x = 0;
				line[0].y = (int)Math.round(rho/s);
				line[1].x = (int)Math.round(rho/c);
				line[1].y = 0;
			}
			
			line_segments.add(line);
		}
		
		return line_segments;
	}
	
	/**
	 * Get Harris corners.
	 * @param image a grayscale image
	 * @param w the width of the image
	 * @param h the height of the image
	 * @param block the neighborhood size
	 * @param aperture the aperture parameter for the Sobel operator
	 * @param k Harris detector free parameter
	 * @param threshold the threshold
	 * @return the corners
	 */
	public static Vector<Pixel> getCorners(double[] image, int w, int h, int block, int aperture, double k, double threshold)
	{
		IplImage iplimage = g2ipl(image, w, h);
		IplImage iplharris = IplImage.create(iplimage.width(), iplimage.height(), IPL_DEPTH_32F, 1);
		Vector<Pixel> corners = new Vector<Pixel>();
		int minx, maxx, miny, maxy;
		
		cvCornerHarris(iplimage, iplharris, block, aperture, k);

		//Extract corners
		FloatBuffer float_buffer = iplharris.getFloatBuffer();
		float[] float_array = new float[float_buffer.limit()];
		float_buffer.get(float_array);
		double[] harris_image = ImageUtility.nonMaximumSuppression(f2d(float_array), w, h);
		
		for(int x=0; x<w; x++){
			for(int y=0; y<h; y++){
				if(harris_image[y*w+x] > threshold){
					corners.add(new Pixel(x, y));
				}
			}
		}
		
		return corners;
	}
	
	/**
	 * Get Harris corners.
	 * @param image a grayscale image
	 * @param w the width of the image
	 * @param h the height of the image
	 * @param threshold the threshold
	 * @return the corners
	 */
	public static Vector<Pixel> getCorners(double[] image, int w, int h, double threshold)
	{
		return getCorners(image, w, h, 5, 3, 0.04, threshold);
	}
	
	/**
	 * Get SURF features.
	 * @param image a grayscale image
	 * @param w the width of the image
	 * @param h the height of the image
	 * @return the features
	 */
	public static Pair<Vector<Ellipse>,Vector<double[]>> getSURFFeatures(double[] image, int w, int h)
	{
		IplImage iplimage = g2ipl(image, w, h);
		CvSeq cvkeypoints = new CvSeq(null);
		CvSeq cvdescriptors = new CvSeq(null);
		CvSURFParams parameters = cvSURFParams(500, 0);
		Vector<Ellipse> keypoints = new Vector<Ellipse>();
		Vector<double[]> descriptors = new Vector<double[]>();
		
		cvExtractSURF(iplimage, null, cvkeypoints, cvdescriptors, CvMemStorage.create(), parameters, 0);
		
		for(int i=0; i<cvkeypoints.total(); i++){
			CvSURFPoint cvpoint = new CvSURFPoint(cvGetSeqElem(cvkeypoints, i));
			keypoints.add(new Ellipse(cvpoint.pt().x(), cvpoint.pt().y(), cvpoint.size()/4, cvpoint.size()/4, 0));

			FloatBuffer float_buffer = cvGetSeqElem(cvdescriptors, i).asByteBuffer(cvdescriptors.elem_size()).asFloatBuffer();
			float[] float_array = new float[float_buffer.limit()];
			float_buffer.get(float_array);
			descriptors.add(f2d(float_array));
		}
		
		return new Pair<Vector<Ellipse>,Vector<double[]>>(keypoints, descriptors);
	}
	
	/**
	 * A main for debug purposes.
	 * @param args the command line arguments
	 */
	public static void main(String args[])
	{
		//Simple JavaCV test
		if(false){
			IplImage image = cvLoadImage("C:/Users/kmchenry/Files/Data/Images/scar1.jpg", CV_LOAD_IMAGE_GRAYSCALE);
			cvSmooth(image, image, CV_GAUSSIAN, 9, 0, 0, 0);
			//cvCanny(image, image, 600, 200, 3);
			
			if(false){
				CanvasFrame frame = new CanvasFrame("JavaCV");
				frame.showImage(image);
			}else{
				ImageViewer.show(image.getBufferedImage(), "JavaCV");
			}
		}
		
		//Test edge detection
		if(false){
			int[][] image = ImageUtility.load("C:/Users/kmchenry/Files/Data/Images/scar1.jpg");
			int h = image.length;
			int w = image[0].length;
			double[] edges = getEdges(image);

			ImageViewer.show(edges, w, h, "Edges");
		}
			
		//Test line detection
		if(true){
			int[][] image = ImageUtility.load("C:/Users/kmchenry/Files/Data/Images/house.png");
			int[] image1d = ImageUtility.to1D(image);
			int h = image.length;
			int w = image[0].length;
			
			double[] edges = getEdges(image, 200, 200, 3);
			Vector<Pair<Double,Double>> lines = getLines(edges, w, h, 1, 40);
			Vector<Pixel[]> line_segments = getLineSegments(lines, w, h);
			
			System.out.println("Lines: " + lines.size());
			ImageUtility.drawLines(image1d, w, h, line_segments, 0x000000ff);		
			ImageViewer viewer = ImageViewer.show(edges, w, h, "Lines");
			viewer.add(image1d, w, h, true);
		}
		
		//Test Harris detector
		if(false){
			int[][] image = ImageUtility.load("C:/Users/kmchenry/Files/Data/Images/scar1.jpg");
			double[] image_g = ImageUtility.argb2g(image);
			int[] image1d = ImageUtility.to1D(image);
			int h = image.length;
			int w = image[0].length;
			
			Vector<Pixel> corners = getCorners(image_g, w, h, 0.001);
			
			System.out.println("Corners: " + corners.size());
			ImageUtility.drawBoxes(image1d, w, h, corners, 4, 0x00ffff00);
			ImageViewer.show(image1d, w, h, "Corners");
		}
		
		//Test SURF
		if(false){
			int[][] image = ImageUtility.load("C:/Users/kmchenry/Files/Data/Images/scar1.jpg");
			double[] image_g = ImageUtility.argb2g(image);
			int h = image.length;
			int w = image[0].length;
			
			Pair<Vector<Ellipse>,Vector<double[]>> features = getSURFFeatures(image_g, w, h);
			
			System.out.println("Features: " + features.first.size());
			ImageUtility.drawEllipses(image, features.first, 0x00ffff00);
			ImageViewer.show(image, w, h, "SURF");
		}
	}
}
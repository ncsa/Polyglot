package edu.ncsa.image;
import edu.ncsa.utility.*;
import edu.ncsa.matrix.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import sun.awt.shell.*;
import javax.swing.*;
import javax.imageio.*;
import java.io.*;
import java.util.*;

/**
 * Utility functions used to manipulate images.
 *  @author Kenton McHenry
 */
public class ImageUtility
{
  public enum Option{SOBEL, SPOT, EDGE, BAR, SAME, FULL};
  
  /**
   * Convert a 2D ARGB image into a 1D ARGB image.
   *  @param img2D the 2D version of the image
   *  @return the 1D version of the image
   */
  public static int[] to1D(int[][] img2D)
  {
  	int h = img2D.length;
  	int w = img2D[0].length;
  	int[] img1D = new int[w*h];
  	
  	for(int x=0; x<w; x++){
  		for(int y=0; y<h; y++){
  			img1D[y*w+x] = img2D[y][x];
  		}
  	}
  	
  	return img1D;
  }

	/**
   * Convert a 1D ARGB image into a 2D ARGB image.
   *  @param img1D the 1D version of the image
   *  @param w the width of the image
   *  @param h the height of the image
   *  @return the 2D version of the image
   */
  public static int[][] to2D(int[] img1D, int w, int h)
  {
  	int[][] img2D = new int[h][w];
  	
  	for(int x=0; x<w; x++){
  		for(int y=0; y<h; y++){
  			img2D[y][x] = img1D[y*w+x];
  		}
  	}
  	
  	return img2D;
  }

	/**
   * Convert a BufferedImage into a 2D ARGB image
   * @param image the BufferedImage
   * @return the 2D ARGB representation of the given BufferedImage data
   */
  public static int[][] image2argb(BufferedImage image)
  {
    int w = image.getWidth(null);
    int h = image.getHeight(null);
    int[] img = new int[w*h];
    
    image.getRGB(0, 0, w, h, img, 0, w);

    return ImageUtility.to2D(img, w, h);
  }
  
  /**
   * Convert an ARGB image into a BufferedImage
   * @param img a 2D ARGB image
   * @return a BufferedImage representation of the given image
   */
  public static BufferedImage argb2image(int[][] img)
  {
  	int h = img.length;
  	int w = img[0].length;
  	BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
    
  	image.setRGB(0, 0, w, h, to1D(img), 0, w);

    return image;
  }
  
  /**
   * Convert an ARGB image into a BufferedImage
   * @param img a 1D ARGB image
   * @param w the width of the image
   * @param h the height of the image
   * @return a BufferedImage representation of the given image
   */
  public static BufferedImage argb2image(int[] img, int w, int h)
  {
  	BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
  	image.setRGB(0, 0, w, h, img, 0, w);

    return image;
  }

	/**
   * Convert a color image to a gray scale image.
   *  @param img the color image in ARGB format.
   *  @param w the width of the image
   *  @param h the height of the image
   *  @return the converted gray scale image
   */
  public static double[] argb2g(int[] img, int w, int h)
  {
  	double[] img_g = new double[w*h];
  	int tmpi;
  	double tmpd;
  	
  	for(int i=0; i<img.length; i++){
  		tmpi = img[i];
  		
  		tmpd = 0;
  		tmpd += (tmpi>>16) & 0x000000ff;
  		tmpd += (tmpi>>8) & 0x000000ff;
  		tmpd += tmpi & 0x000000ff;
  		tmpd /= 3.0;
  		tmpd /= 255.0;
  		
  		img_g[i] = tmpd;
  	}
  	
  	return img_g;
  }
  
	/**
   * Convert a color image to a gray scale image.
   * @param img the color image in ARGB format.
   * @return the converted gray scale image
   */
  public static double[] argb2g(int[][] img)
  {
  	int h = img.length;
  	int w = img[0].length;
  	double[] img_g = new double[w*h];
  	int rgb;
  	double tmpd;
  	
  	for(int x=0; x<w; x++){
  		for(int y=0; y<h; y++){
  			rgb = img[y][x];
  			
    		tmpd = 0;
    		tmpd += (rgb>>16) & 0x000000ff;
    		tmpd += (rgb>>8) & 0x000000ff;
    		tmpd += rgb & 0x000000ff;
    		tmpd /= 3.0;
    		tmpd /= 255.0;
    		
  			img_g[y*w+x] = tmpd;
  		}
  	}
  	
  	return img_g;
  }

	/**
   * Convert and scale a gray scale image to an ARGB image.
   *  @param img the gray scale image
   *  @param w the image width
   *  @param h the image height
   *  @return the converted ARGB image
   */
  public static int[] g2argb(double[] img, int w, int h)
  {
    int[] img_rgb = new int[w*h];
    double mind = Double.MAX_VALUE;
    double maxd = -Double.MAX_VALUE;
    double sclI;
    int tmpi;
    
    //Find extreme values (to scale image intensity)
    for(int i=0; i<img.length; i++){
      if(img[i] < mind) mind = img[i];
      if(img[i] > maxd) maxd = img[i];
    }
    
    sclI = 1.0 / (maxd-mind);
    
    for(int i=0; i<img.length; i++){
      tmpi = (int)Math.round(255.0*(img[i]-mind)*sclI);
      img_rgb[i] = (tmpi<<16) | (tmpi<<8) | tmpi; 
    }
    
    return img_rgb;
  }
  
	/**
   * Convert and scale a gray scale image to an ARGB image.
   * @param img the gray scale image
   * @return the converted ARGB image
   */
  public static int[][] g2argb(double[][] img)
  {
  	int h = img.length;
  	int w = img[0].length;
    int[][] img_rgb = new int[h][w];
    double mind = Double.MAX_VALUE;
    double maxd = -Double.MAX_VALUE;
    double sclI;
    int tmpi;
    
    //Find extreme values (to scale image intensity)
    for(int y=0; y<h; y++){
    	for(int x=0; x<w; x++){
	      if(img[y][x] < mind) mind = img[y][x];
	      if(img[y][x] > maxd) maxd = img[y][x];
    	}
    }
    
    sclI = 1.0 / (maxd-mind);
    
    for(int y=0; y<h; y++){
    	for(int x=0; x<w; x++){
	      tmpi = (int)Math.round(255.0*(img[y][x]-mind)*sclI);
	      img_rgb[y][x] = (tmpi<<16) | (tmpi<<8) | tmpi; 
	    }
    }
    
    return img_rgb;
  }

	/**
   * Convert a gray scale image into a black and white image.
   *  @param img the gray scale image.
   *  @param w the width of the image
   *  @param h the height of the image
   *  @param t the threshold by which to decide which pixels should be white.
   *  @return the converted black and white image
   */
  public static double[] g2bw(double[] img, int w, int h, double t)
  {
  	double[] img_bw = new double[w*h];
  	
  	for(int i=0; i<img.length; i++){
  		if(img[i] > t){
  			img_bw[i] = 1.0;
  		}else{
  			img_bw[i] = 0.0;
  		}
  	}
  	
  	return img_bw;
  }
  
  /**
   * Convert an RGB color image to a BGR color image.
   * @param Irgb a color image in ARGB format
   * @return a color image in ABGR format
   */
  public static int[][] argb2abgr(int[][] Irgb)
  {
  	int h = Irgb.length;
  	int w = Irgb[0].length;
  	int[][] Ibgr = new int[h][w];
  	int rgb, r, g, b;
  	
  	for(int x=0; x<w; x++){
  		for(int y=0; y<h; y++){
				rgb = Irgb[y][x];
				r = (rgb >> 16) & 0x000000ff;
				g = (rgb >> 8) & 0x000000ff;
				b = rgb & 0x000000ff;
				Ibgr[y][x] = (b<<16) | (g<<8) | r;
			}
  	}
  	
  	return Ibgr;
  }
  
  /**
   * Convert an ARGB image into an HSV image.
   * @param Irgb the ARGB image
   * @return an HSV image stored as a 1D array of doubles
   */
  public static double[] argb2hsv(int[][] Irgb)
	{
		int h = Irgb.length;
		int w = Irgb[0].length;
		double[] Ihsv = new double[3 * w * h];
		int rgb, at3;
		double r, g, b;
		double minv, maxv, delta;
		double tmpd = 0;

		for(int x = 0; x < w; x++){
			for(int y = 0; y < h; y++){
				at3 = 3 * (y*w + x);

				rgb = Irgb[y][x];
				r = ((rgb >> 16) & 0x000000ff) / 255.0;
				g = ((rgb >> 8) & 0x000000ff) / 255.0;
				b = (rgb & 0x000000ff) / 255.0;

				maxv = r;
				if(g > maxv) maxv = g;
				if(b > maxv) maxv = b;

				minv = r;
				if(g < minv) minv = g;
				if(b < minv) minv = b;

				Ihsv[at3 + 2] = maxv; 				//Set brightness value

				if(maxv != 0){ 								//Calculate saturation
					Ihsv[at3 + 1] = (maxv - minv) / maxv;
				}else{
					Ihsv[at3 + 1] = 0;
				}

				if(Ihsv[at3 + 1] == 0){ 			//Determine hue
					Ihsv[at3 + 0] = 0;
				}else{
					delta = maxv - minv;
					if(r == maxv)
						tmpd = (g - b) / delta;
					else if(g == maxv)
						tmpd = 2.0 + (b - r) / delta;
					else if(b == maxv)
						tmpd = 4.0 + (r - g) / delta;

					tmpd *= 60; 								//Convert hue to degrees
					if(tmpd < 0) tmpd += 360.0; //Make sure non-negative
					Ihsv[at3 + 0] = tmpd / 360.0;
				}
			}
		}

		return Ihsv;
	}

  /**
   * Convert an image of number labels into a color image.
   * @param Is the labeled image
   * @return the color version of the image
   */
  public static int[][] n2argb(int[][] Is)
  {
  	int h = Is.length;
  	int w = Is[0].length;
    int[][] Irgb = new int[h][w];
    int[] colors;
    Random random = new Random();
    int i, x, y, rgb, r, g, b, n;

    //Find largest number
    n = 0;
    
    for(x=0; x<w; x++){
      for(y=0; y<h; y++){
        if(Is[y][x] > n) n = Is[y][x];
      }
    }

    //Build color map randomly
    colors = new int[n+1];
    
    for(i=0; i<=n; i++){
      r = random.nextInt() % 128 + 128;
      g = random.nextInt() % 128 + 128;
      b = random.nextInt() % 128 + 128;
      rgb = (r<<16) | (g<<8) | b;
      colors[i] = rgb;
    }

    //Assign colors
    for(x=0; x<w; x++){
      for(y=0; y<h; y++){
        if(Is[y][x] >= 0){
          Irgb[y][x] = colors[Is[y][x]];
        }
      }
    }

    return Irgb;
  }
  
  /**
   * Convert an image of number labels into a color image using the colors from the orignal image.
   * @param Is the segmented/labeled image
   * @param Irgb the original color image
   * @return the color version of the image
   */
  public static int[][] n2argb(int[][] Is, int[][] Irgb)
  {
  	int h = Is.length;
  	int w = Is[0].length;
  	int[][] Is_rgb = new int[h][w];
  	Vector<Integer> colors = getRegionColors(Is, Irgb);
  	
  	for(int x=0; x<w; x++){
  		for(int y=0; y<h; y++){
  			Is_rgb[y][x] = colors.get(Is[y][x]);
  		}
  	}
  	
  	return Is_rgb;
  }
  
  /**
   * L1 Normalize the given 2D grayscale image.
   * @param F the image
   */
  public static void normalizeL1(double[][] F)
  {
  	int w = F[0].length;
  	int h = F.length;
  	int x, y;
  	double sump = 0;
  	double sumn = 0;
  
  	for(x = 0; x < w; x++){
  		for(y = 0; y < h; y++){
  			if(F[y][x] > 0){
  				sump += F[y][x];
  			}else if(F[y][x] < 0){
  				sumn -= F[y][x];
  			}
  		}
  	}
  
  	for(x = 0; x < w; x++){
  		for(y = 0; y < h; y++){
  			if(F[y][x] > 0){
  				F[y][x] /= sump;
  			}else if(F[y][x] < 0){
  				F[y][x] /= sumn;
  			}
  		}
  	}
  }
  
  /**
   * Calculate the pixel by pixel difference between two gray scale images.
   * @param diff_img an array which will contain the difference image
   * @param img0 a gray scale image
   * @param img1 another gray scale image
   * @param threshold the difference threshold to use when counting pixels (0 indicates any difference will be counted)
   * @param SHOW_DIFFERENCE true if pixels brightness should indicate difference
   */
  public static int difference(double[] diff_img, double[] img0, double[] img1, double threshold, boolean SHOW_DIFFERENCE)
  {  	
  	int count = 0;
  	double diff;
  	
  	if(diff_img!=null && diff_img.length == img0.length && img0.length==img1.length){
			for(int i=0; i<diff_img.length; i++){
				diff = Math.abs(img0[i] - img1[i]);
				
				if(diff > threshold){
					if(SHOW_DIFFERENCE){
						diff_img[i] = diff;
					}else{
						diff_img[i] = 1;
					}
					
					count++;
				}else{
					diff_img[i] = 0;
				}
			}
  	}
  	
  	return count;
  }
  
  /**
   * Calculate the pixel by pixel difference between two color images.
   * @param diff_img an array which will contain the gray scale difference image
   * @param img0 a color image
   * @param img1 another color image
   * @param threshold the difference threshold to use when counting pixels (0 indicates any difference will be counted)
   * @param SHOW_DIFFERENCE true if pixels brightness should indicate difference
   */
  public static int difference(double[] diff_img, int[] img0, int[] img1, int threshold, boolean SHOW_DIFFERENCE)
  {
  	int count = 0;
  	int rgb;
  	int r0, g0, b0, r1, g1, b1;
  	int diff;
  	
  	if(diff_img!=null && diff_img.length == img0.length && img0.length==img1.length){
			for(int i=0; i<diff_img.length; i++){
				rgb = img0[i];
		    r0 = (rgb >> 16) & 0x000000ff;
		    g0 = (rgb >> 8) & 0x000000ff;
		    b0 = rgb & 0x000000ff;
		    
				rgb = img1[i];
		    r1 = (rgb >> 16) & 0x000000ff;
		    g1 = (rgb >> 8) & 0x000000ff;
		    b1 = rgb & 0x000000ff;
		    
		    diff = 0;
		    diff += Math.abs(r0 - r1);
		    diff += Math.abs(g0 - g1);
		    diff += Math.abs(b0 - b1);
		    
		    if(diff > threshold){
		    	if(SHOW_DIFFERENCE){
		    		diff_img[i] = diff;
		    	}else{
		    		diff_img[i] = 1;
		    	}
		    	
		    	count++;
		    }else{
		    	diff_img[i] = 0;
		    }
			}
  	}
  	
  	return count;
  }
  
	/**
	 * Find a target image within another image.
	 * @param image the image to search
	 * @param target the target sub-image to search for
	 * @param threshold a threshold to determine when two pixels are close enough to be a match
	 * @param STOP_AT_ONE if true return after finding one match
	 * @return the upper left corner locations of the found target images
	 */
	public static Vector<Point> find(int[][] image, int[][] target, int threshold, boolean STOP_AT_ONE)
	{		
		Vector<Point> locations = new Vector<Point>();
		int image_height = image.length;
		int image_width = image[0].length;
		int target_height = target.length;
		int target_width = target[0].length;
  	int rgb;
  	int r0, g0, b0, r1, g1, b1;
  	int diff;
  	boolean FOUND;
  						
		for(int y=0; y<image_height-target_height+1; y++){			
			for(int x=0; x<image_width-target_width+1; x++){
		    FOUND = true;
		    
				for(int u=0; u<target_width; u++){
					for(int v=0; v<target_height; v++){
						rgb = image[y+v][x+u];
				    r0 = (rgb >> 16) & 0x000000ff;
				    g0 = (rgb >> 8) & 0x000000ff;
				    b0 = rgb & 0x000000ff;
				    
						rgb = target[v][u];
				    r1 = (rgb >> 16) & 0x000000ff;
				    g1 = (rgb >> 8) & 0x000000ff;
				    b1 = rgb & 0x000000ff;
				    
				    diff = 0;
				    diff += Math.abs(r0 - r1);
				    diff += Math.abs(g0 - g1);
				    diff += Math.abs(b0 - b1);
				    
				    if(diff > threshold){
				    	FOUND = false;
				    	break;
				    }
					}
					
					if(!FOUND) break;
				}
				
				if(FOUND){
					locations.add(new Point(x,y));
					if(STOP_AT_ONE) break;
				}
			}
			
			if(STOP_AT_ONE && !locations.isEmpty()) break;
		}
		
		return locations;
	}

	/**
	 * Calculate the sum of squared differences between two gray scale images.
	 * @param img0 a gray scale image
	 * @param img1 another gray scale image
	 * @return the sum of squared differences between the two images
	 */
	public static double ssd(double[] img0, double[] img1)
	{
		double ssd = -1;
		double tmpd;
		
		if(img0.length == img1.length){
			ssd = 0;
			
			for(int i=0; i<img0.length; i++){
				tmpd = img1[i] - img0[i];
				ssd += tmpd*tmpd;
			}
		}
		
		return ssd;
	}

	/**
	 * Calculate the normalized cross correlation between two gray scale images.
	 * @param img0 a gray scale image
	 * @param img1 another gray scale image
	 * @return the normalized cross correlation between the two images
	 */
	public static double ncc(double[] img0, double[] img1)
	{
		double ncc = -1;
		double mean0 = 0;
		double mean1 = 0;
		double std0 = 0;
		double std1 = 0;
		double tmpd;		
		
		if(img0.length == img1.length){
			//Calculate means
			for(int i=0; i<img0.length; i++){
				mean0 += img0[i];
				mean1 += img1[i];
			}
			
			mean0 /= img0.length;
			mean1 /= img0.length;
			
			//Calculate standard deviations
			for(int i=0; i<img0.length; i++){
				tmpd = img0[i]-mean0;
				std0 += tmpd*tmpd;
				
				tmpd = img1[i]-mean1;
				std1 += tmpd*tmpd;
			}
			
			std0 = Math.sqrt(std0 / img0.length);
			std1 = Math.sqrt(std1 / img0.length);
	
			//Calculate difference
			ncc = 0;
			
			for(int i=0; i<img0.length; i++){
				ncc += ((img0[i]-mean0)*(img1[i]-mean1))/(std0*std1);
			}
		
			ncc /= img0.length - 1;
		}
		
		return ncc;
	}

	/**
	 * Calculate the distance between the given points
	 * @param x0 the x-coordinate of the first point
	 * @param y0 the y-coordinate of the first point
	 * @param x1 the x-coordinate of the second point
	 * @param y1 the y-coordinate of the second point
	 */
	public static double distance(double x0, double y0, double x1, double y1)
	{
	  double dx = x1 - x0;
	  double dy = y1 - y0;
	  
	  return Math.sqrt(dx*dx + dy*dy);
	}

	/**
	 * Calculate the distance of the given point from the given line segment.
	 * Note: Uses perpendicular distances to line segment.
	 * @param x the x-coordinate of the point
	 * @param y the y-coordinate of the point
	 * @param x0 the x-coordinate of the line segments starting point
	 * @param y0 the y-coordinate of the line segments starting point
	 * @param x1 the x-coordinate of the line segments ending point
	 * @param y1 the y-coordinate of the line segments ending point
	 * @return the distance of the point from the line segment
	 */
	public static double distance(double x, double y, double x0, double y0, double x1, double y1)
	{
		double dx, dy, dx0, dx1, dy0, dy1, length, t;
		
		dx0 = x - x0;
		dy0 = y - y0;
		dx1 = x1 - x0;
		dy1 = y1 - y0;
		length = distance(x0, y0, x1, y1);
		t = (dx0*dx1 + dy0*dy1) / (length*length);
		
		if(t <= 0){
			return distance(x, y, x0, y0);
		}else if(t >= 1){
			return distance(x, y, x1, y1);
		}else{
	  	dx = x1 - x0;
	  	dy = y1 - y0;
	  	
			return distance(x, y, (int)Math.round(x0+t*dx), (int)Math.round(y0+t*dy));
		}
	}
	
	/**
	 * Crop the given image.
	 * @param img the image to crop
	 * @param minx the minimum x-coordinate
	 * @param miny the minimum y-coordinate
	 * @param maxx the maximum x-coordinate
	 * @param maxy the maximum y-coordinate
	 * @return the cropped image
	 */
	public static int[][] crop(int[][] img, int minx, int miny, int maxx, int maxy)
	{		
		int[][] img_cropped = null;
		int h = img.length;
		int w = img[0].length;
		
		if(minx < 0) minx = 0;
		if(miny < 0) miny = 0;
		if(maxx >= w) maxx = w-1;
		if(maxy >= h) maxy = h-1;
		
		img_cropped = new int[maxy-miny+1][maxx-minx+1];
		
		for(int x=minx; x<=maxx; x++){
			for(int y=miny; y<=maxy; y++){
				img_cropped[y-miny][x-minx] = img[y][x];
			}
		}
		
		return img_cropped;
	}
	
	/**
	 * Crop the given image.
	 * @param img the image to crop
	 * @param w the width of the image
	 * @param h the height of the image
	 * @param minx the minimum x-coordinate
	 * @param miny the minimum y-coordinate
	 * @param maxx the maximum x-coordinate
	 * @param maxy the maximum y-coordinate
	 * @return the cropped image
	 */
	public static int[] crop(int[] img, int w, int h, int minx, int miny, int maxx, int maxy)
	{
		int[] img_cropped;
		int w_cropped, h_cropped;
		
		if(minx < 0) minx = 0;
		if(miny < 0) miny = 0;
		if(maxx >= w) maxx = w-1;
		if(maxy >= h) maxy = h-1;
		
		w_cropped = maxx-minx+1;
		h_cropped = maxy-miny+1;
		img_cropped = new int[w_cropped*h_cropped];
		
		for(int x=minx; x<=maxx; x++){
			for(int y=miny; y<=maxy; y++){
				img_cropped[(y-miny)*w_cropped+(x-minx)] = img[y*w+x];
			}
		}
		
		return img_cropped;
	}
	
	/**
	 * Crop the given image.
	 * @param img the image to crop
	 * @param w the width of the image
	 * @param h the height of the image
	 * @param minx the minimum x-coordinate
	 * @param miny the minimum y-coordinate
	 * @param maxx the maximum x-coordinate
	 * @param maxy the maximum y-coordinate
	 * @return the cropped image
	 */
	public static double[] crop(double[] img, int w, int h, int minx, int miny, int maxx, int maxy)
	{
		double[] img_cropped;
		int w_cropped, h_cropped;
		
		if(minx < 0) minx = 0;
		if(miny < 0) miny = 0;
		if(maxx >= w) maxx = w-1;
		if(maxy >= h) maxy = h-1;
		
		w_cropped = maxx-minx+1;
		h_cropped = maxy-miny+1;
		img_cropped = new double[w_cropped*h_cropped];
		
		for(int x=minx; x<=maxx; x++){
			for(int y=miny; y<=maxy; y++){
				img_cropped[(y-miny)*w_cropped+(x-minx)] = img[y*w+x];
			}
		}
		
		return img_cropped;
	}
	
	/**
   * Resize an ARGB image.
   *  @param img the image
   *  @param w the image width
   *  @param h the image height
   *  @param w_new the desired image width
   *  @param h_new the desired image height
   *  @return the resized image in ARGB
   */
  public static int[] resize(int[] img, int w, int h, int w_new, int h_new)
  {
    int[] img_new = new int[w_new*h_new];
    double sclX = ((double)(w-1))/((double)(w_new-1));
    double sclY = ((double)(h-1))/((double)(h_new-1));
    int u, v;
    
    for(int x=0; x<w_new; x++){
      u = (int)Math.round(sclX*((double)x));
      
      for(int y=0; y<h_new; y++){
        v = (int)Math.round(sclY*((double)y));
        img_new[y*w_new+x] = img[v*w+u]; 
      }
    }
    
    return img_new;
  }
  
	/**
   * Resize an ARGB image.
   * @param img the image
   * @param w_new the desired image width
   * @param h_new the desired image height
   * @return the resized image in ARGB
   */
  public static int[][] resize(int[][] img, int w_new, int h_new)
  {
  	int h = img.length;
  	int w = img[0].length;
    int[][] img_new = new int[h_new][w_new];
    double sclX = ((double)(w-1))/((double)(w_new-1));
    double sclY = ((double)(h-1))/((double)(h_new-1));
    int u, v;
    
    for(int x=0; x<w_new; x++){
      u = (int)Math.round(sclX*((double)x));
      
      for(int y=0; y<h_new; y++){
        v = (int)Math.round(sclY*((double)y));
        img_new[y][x] = img[v][u]; 
      }
    }
    
    return img_new;
  }
  
	/**
   * Resize an ARGB image.
   * @param img the image
   * @param scale the scale of the new image
   * @return the resized image in ARGB
   */
  public static int[][] resize(int[][] img, double scale)
  {
  	int h = img.length;
  	int w = img[0].length;
    int w_new = (int)Math.round(scale * w);
    int h_new = (int)Math.round(scale * h);
    
    return resize(img, w_new, h_new);
  }

	/**
   * Resize a grayscale image.
   *  @param img the image
   *  @param w_new the desired image width
   *  @param h_new the desired image height
   *  @return the resized image
   */
  public static double[][] resize(double[][] img, int w_new, int h_new)
  {
  	int h = img.length;
  	int w = img[0].length;
    double[][] img_new = new double[h_new][w_new];
    double sclX = ((double)(w-1))/((double)(w_new-1));
    double sclY = ((double)(h-1))/((double)(h_new-1));
    int u, v;
    
    for(int x=0; x<w_new; x++){
      u = (int)Math.round(sclX*((double)x));
      
      for(int y=0; y<h_new; y++){
        v = (int)Math.round(sclY*((double)y));
        img_new[y][x] = img[v][u]; 
      }
    }
    
    return img_new;
  }
  
	/**
   * Resize a grayscale image.
   * @param img the image
   * @param scale the scale of the new image
   * @return the resized image
   */
  public static double[][] resize(double[][] img, double scale)
  {
  	int h = img.length;
  	int w = img[0].length;
    int w_new = (int)Math.round(scale * w);
    int h_new = (int)Math.round(scale * h);
    
    return resize(img, w_new, h_new);
  }

	/**
	 * Resize an image.
	 * @param image the image to scale
	 * @param scale the scale factor to use
	 * @param method the RenderingHints object describing the interpolation method to use 
	 * @return the scaled image
	 */
	public static BufferedImage resize(BufferedImage image, double scale, Object method)
	{
		int scaled_width = (int)Math.ceil(scale * image.getWidth());
		int scaled_height = (int)Math.ceil(scale * image.getHeight());
		BufferedImage scaled_image = new BufferedImage(scaled_width, scaled_height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = scaled_image.createGraphics();
		
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, method);
		g.drawImage(image, AffineTransform.getScaleInstance(scale, scale), null);
		g.dispose();
		
		return scaled_image;
	}

	/**
   * Resize an image using bicubic interpolation.
   *  @param I the image
   *  @param w_new the desired image w_new
   *  @param h_new the desired image h_new
   */
  public static double[][] resizeBicubic(double[][] I, int w_new, int h_new)
  {
    int w = I[0].length;
    int h = I.length;
    double x, y;
    int ix, iy;
    double rx, ry;
    double A, B, C, D;
    double scale_x, scale_y;
    double[][] R = new double[h_new][w_new];
  
    //Pad left and bottom border of image so interpolation is always correct
    double[][] I_padded = new double[h+1][w+1];
    
    for(int u=0; u<w; u++){
      for(int v=0; v<h; v++){
        I_padded[v][u] = I[v][u];
      }
    }
    
    for(int u=0; u<w; u++) I_padded[h][u] = I[h-1][u];
    for(int v=0; v<h; v++) I_padded[v][w] = I[v][w-1];
    I_padded[h][w] = I[h-1][w-1];
  
    scale_x = ((double)(w_new-1)) / ((double)(w-1));
    scale_y = ((double)(h_new-1)) / ((double)(h-1));
  
    for(int u=0; u<w_new; u++){
      for(int v=0; v<h_new; v++){
        x = ((double)u) / scale_x;
        y = ((double)v) / scale_y;
        ix = (int)Math.round(x);
        iy = (int)Math.round(y);
        rx = x - (double)ix;
        ry = y - (double)iy;
  
        A = I_padded[iy][ix];
        B = I_padded[iy+1][ix];
        C = I_padded[iy][ix+1];
        D = I_padded[iy+1][ix+1];
  
        R[v][u] = ((1.0-rx) * (1.0-ry) * A +
                         rx * (1.0-ry) * B +
                         (1.0-rx) * ry * C +
                               rx * ry * D);
        
        if(R[v][u] < 0.0) R[v][u] = 0.0;
        if(R[v][u] > 1.0) R[v][u] = 1.0;
      }
    }
  
    return R;
  }

	/**
   * Resize an image using bicubic interpolation.
   *  @param img the image
   *  @param w the image width
   *  @param h the image height
   *  @param w_new the desired image w_new
   *  @param h_new the desired image h_new
   */
  public static int[] resizeBicubic(int[] img, int w, int h, int w_new, int h_new)
  {
    int[] img_new = new int[w_new*h_new]; 
    double[][] I = new double[h][w];
    double[][] I_new;
    
    //Interpolate red
    for(int x=0; x<w; x++){
      for(int y=0; y<h; y++){
        I[y][x] = ((double)((img[y*w+x] >> 16) & 0x000000ff)) / 255.0;
      }
    }
    
    I_new = resizeBicubic(I, w_new, h_new);
    
    for(int x=0; x<w_new; x++){
      for(int y=0; y<h_new; y++){
        img_new[y*w_new+x] |= ((int)Math.round(I_new[y][x]*255.0) << 16) | 0xff000000;
      }
    }
    
    //Interpolate green
    for(int x=0; x<w; x++){
      for(int y=0; y<h; y++){
        I[y][x] = ((double)((img[y*w+x] >> 8) & 0x000000ff)) / 255.0;
      }
    }
    
    I_new = resizeBicubic(I, w_new, h_new);
    
    for(int x=0; x<w_new; x++){
      for(int y=0; y<h_new; y++){
        img_new[y*w_new+x] |= ((int)Math.round(I_new[y][x]*255.0) << 8);
      }
    }
    
    //Interpolate blue
    for(int x=0; x<w; x++){
      for(int y=0; y<h; y++){
        I[y][x] = ((double)(img[y*w+x] & 0x000000ff)) / 255.0;
      }
    }
    
    I_new = resizeBicubic(I, w_new, h_new);
    
    for(int x=0; x<w_new; x++){
      for(int y=0; y<h_new; y++){
        img_new[y*w_new+x] |= (int)Math.round(I_new[y][x]*255.0);
      }
    }
    
    return img_new;
  }
  
  /**
   * Warp the specified image by the given transformation.
   * @param I the grayscale image
   * @param M the desired transformation
   * @param default_value the value of pixels outside the image
   * @param option indaces wheter or not to maintain the image dimensions or the image content
   * @return the warped image
   */
  public static double[][] warp(double[][] I, double[][] M, double default_value, Option option)
  {
  	int w = I[0].length;
  	int h = I.length;
    boolean PROJECTIVE = false;
    int ww, hw;
    double xo = w / 2.0;
    double yo = h / 2.0;;
    double xow, yow;
    int size;
    int i, x, y, at;
    double minx, maxx, miny, maxy;
    double tmpx, tmpy, tmpd;
  
    if(M.length == 3) PROJECTIVE = true;
  
    if(option == Option.SAME){
      ww = w;
      hw = h;
    }else{
      double[][] C;
      
      if(!PROJECTIVE){
        C = MatrixUtility.zeros(4,2);
      }else{
        C = MatrixUtility.ones(4,3);
      }
  
      C[0][0] = 0.0 - xo;      C[0][1] = 0.0 - yo;
      C[1][0] = w - 1.0 - xo;  C[1][1] = 0.0 - yo;
      C[2][0] = 0.0 - xo;      C[2][1] = h - 1.0 - yo;
      C[3][0] = w - 1.0 - xo;  C[3][1] = h - 1.0 - yo;
      
      double[][] Cw = MatrixUtility.mtimes(M, MatrixUtility.transpose(C));
  
      if(PROJECTIVE){
        for(i=0; i<4; i++){
          Cw[0][i] /= Cw[2][i];
          Cw[1][i] /= Cw[2][i];
          Cw[2][i] /= Cw[2][i];
        }
      }
  
      maxx = -1e10;
      minx =  1e10;
      maxy = -1e10;
      miny =  1e10;
  
      for(i=0; i<4; i++){
        if(Cw[0][i] > maxx) maxx = Cw[0][i];
        if(Cw[0][i] < minx) minx = Cw[0][i];
        if(Cw[1][i] > maxy) maxy = Cw[1][i];
        if(Cw[1][i] < miny) miny = Cw[1][i];
      }
  
      if(!PROJECTIVE){
        ww = (int)Math.ceil(maxx - minx) + 1;
        hw = (int)Math.ceil(maxy - miny) + 1;
        if(ww%2 == 0) ww++;         //Keep dimensions odd
        if(hw%2 == 0) hw++;
      }else{                      //Must be careful since projection is not isotropic
        maxx = Math.ceil(maxx);
        minx = Math.floor(minx);
        maxy = Math.ceil(maxy);
        miny = Math.floor(miny);
  
        ww = (int)Math.round(maxx - minx + 1);
        hw = (int)Math.round(maxy - miny + 1);
        if(ww%2 == 0){ ww++; maxx++; }
        if(hw%2 == 0){ hw++; maxy++; }
      }
    }
  
    xow = ww / 2.0;
    yow = hw / 2.0;
    size = ww * hw;
    double[][] P = new double[size][2];
    M = JAMAMatrixUtility.inverse(M);
  
    //To make rotation consistent with matlab version start pixel indexing at 1.
    // For rotation part ONLY!
    if(!PROJECTIVE){
      at = 0;
      
      for(x=0; x<ww; x++){
        for(y=0; y<hw; y++){
          tmpx = (x + 1.0) - xow;
          tmpy = (y + 1.0) - yow;
          P[at][0] = M[0][0]*tmpx + M[0][1]*tmpy - 1.0 + xo;
          P[at][1] = M[1][0]*tmpx + M[1][1]*tmpy - 1.0 + yo;
          at++;
        }
      }
    }else{
      at = 0;
      
      for(x=0; x<ww; x++){
        for(y=0; y<hw; y++){
          tmpx = x;
          tmpy = y;
          
          P[at][0] = M[0][1]*tmpx + M[0][1]*tmpy + M[0][2]*1.0;
          P[at][1] = M[1][1]*tmpx + M[1][1]*tmpy + M[1][2]*1.0;
          tmpd = M[2][1]*tmpx + M[2][1]*tmpy + M[2][2]*1.0;
          
          if(tmpd == 0) tmpd = 1e-10;
          P[at][0] = (P[at][0]/tmpd) - 1.0 + xo;
          P[at][1] = (P[at][1]/tmpd) - 1.0 + yo;
          at++;
        }
      }
    }
  
    double[] Pi = interpolate(I, P, default_value);
    double[][] Iw = new double[hw][ww];
    at = 0;
    
    for(x=0; x<ww; x++){
      for(y=0; y<hw; y++){
        Iw[y][x] = Pi[at];
        at++;
      }
    }
    
    return Iw;
  }
  
  /**
   * Rotate a gray scale image.
   * @param Ig a grayscale image
   * @param w the width of the image
   * @param h the height of the image
   * @param theta the angle of rotation (in degrees)
   * @param xc the x-coordinate of the center of rotation
   * @param yc the y-coordinate of the center of rotation
   * @return the rotated image
   */
  public static double[] rotate(double[] Ig, int w, int h, double theta, double xc, double yc)
  {
  	double[] Ig_r = new double[Ig.length];
  	int x0, y0;
  	double c = Math.cos(-theta*Math.PI/180);
  	double s = Math.sin(-theta*Math.PI/180);
  	
  	for(int x=0; x<w; x++){
  		for(int y=0; y<h; y++){
  			x0 = (int)Math.round((x-xc)*c - (y-yc)*s + xc);
  			y0 = (int)Math.round((x-xc)*s + (y-yc)*c + yc);
  			
  			if(x0>=0 && x0<w && y0>=0 && y0<h){
  				Ig_r[y*w+x] = Ig[y0*w+x0];
  			}
  		}
  	}
  	
  	return Ig_r;
  }

	/**
   * Interpolate the points specified in P within the image I.
   * @param I the grayscale image
   * @param P the 2D points to find
   * @param default_value the intensity at the borders of the image
   * @return the intentsities at each of the requested points
   */
  public static double[] interpolate(double[][] I, double[][] P, double default_value)
  {
  	int w = I[0].length;
  	int h = I.length;
    double[] Iy = new double[P.length];
    int u, v;
    double x, y;
    int ix, iy;
    double rx, ry;
    double A, B, C, D;
  
    //Pad left and bottom border of image so interpolation is always correct
    double[][] I_padded = new double[h+1][w+1];
    for(u=0; u<w; u++){
     for(v=0; v<h; v++){
        I_padded[v][u] = I[v][u];
      }
    }
    for(u=0; u<w; u++) I_padded[h][u] = I[h-1][u];
    for(v=0; v<h; v++) I_padded[v][w] = I[v][w-1];
    I_padded[h][w] = I[h-1][w-1];
  
    for(int i=0; i<P.length; i++){
      x = P[i][0];
      y = P[i][1];
      ix = (int)Math.round(x);
      iy = (int)Math.round(y);
  
      if(ix<0 || ix>=w || iy<0 || iy>=h){
        Iy[i] = default_value;
      }else{
        rx = x - ix;
        ry = y - iy;
  
        A = I_padded[iy][ix];
        B = I_padded[iy][ix+1];
        C = I_padded[iy+1][ix];
        D = I_padded[iy+1][ix+1];
  
        Iy[i] = ((1.0-rx) * (1.0-ry) * A +
                       rx * (1.0-ry) * B +
                       (1.0-rx) * ry * C +
                             rx * ry * D);
      }
    }
  
    return Iy;
  }

	/**
   * Smooth a color image.
   *  @param I the image
   *  @param w the width of the image
   *  @param h the height of the image
   *  @param iterations the number of smoothing iterations to apply
   *  @return the smoothed image
   */
  public static int[] smooth(int[] I, int w, int h, int iterations)
  {
    int[] I_new = null;
    int minu, maxu, minv, maxv;
    double r_mean, g_mean, b_mean, count;
    int at;
  
    for(int i=0; i<iterations; i++){
      I_new = new int[w*h];
      
      for(int x=0; x<w; x++){
        for(int y=0; y<h; y++){
          r_mean = 0;
          g_mean = 0;
          b_mean = 0;
          count = 0;
          
          minu = x - 1;
          maxu = x + 1;
          minv = y - 1;
          maxv = y + 1;
          
          if(minu < 0) minu = 0;
          if(maxu >= w) maxu = w-1;
          if(minv < 0) minv = 0;
          if(maxv >= h) maxv = h-1;
          
          for(int u=minu; u<=maxu; u++){
            for(int v=minv; v<=maxv; v++){
              at = v*w+u;
              r_mean += (I[at] >> 16) & 0x000000ff;
              g_mean += (I[at] >> 8) & 0x000000ff;
              b_mean += I[at] & 0x000000ff;
              count++;
            }
          }
  
          r_mean /= count;
          if(r_mean < 0) r_mean = 0;
          if(r_mean > 255.0) r_mean = 255.0;
          
          g_mean /= count;
          if(g_mean < 0) g_mean = 0;
          if(g_mean > 255.0) g_mean = 255.0;
          
          b_mean /= count;
          if(b_mean < 0) b_mean = 0;
          if(b_mean > 255.0) b_mean = 255.0;
          
          at = y*w+x;
          I_new[at] = 0xff000000;
          I_new[at] |= ((int)Math.round(r_mean)) << 16;
          I_new[at] |= ((int)Math.round(g_mean)) << 8;
          I_new[at] |= ((int)Math.round(b_mean));
        }
      }
      
      I = I_new;
    }
  
  
    return I_new;
  }

	/**
   * Smooth a grayscale image.
   *  @param Ig the image
   *  @param iterations the number of smoothing iterations to apply
   *  @return the smoothed image
   */
  public static double[][] smooth(double[][] Ig, int iterations)
  {
  	int h = Ig.length;
  	int w = Ig[0].length;
    double[][] Ig_new = null;
    int minu, maxu, minv, maxv;
    double mean, count;
  
    for(int i=0; i<iterations; i++){
      Ig_new = new double[h][w];
      
      for(int x=0; x<w; x++){
        for(int y=0; y<h; y++){
        	mean = 0;
          count = 0;
          
          minu = x - 1;
          maxu = x + 1;
          minv = y - 1;
          maxv = y + 1;
          
          if(minu < 0) minu = 0;
          if(maxu >= w) maxu = w-1;
          if(minv < 0) minv = 0;
          if(maxv >= h) maxv = h-1;
          
          for(int u=minu; u<=maxu; u++){
            for(int v=minv; v<=maxv; v++){
              mean += Ig[v][u];
              count++;
            }
          }
  
          mean /= count;
          
          Ig_new[y][x] = mean;
        }
      }
      
      Ig = Ig_new;
    }
  
  
    return Ig_new;
  }

	/**
	 * Thin the given image.
	 * @param Ig a grayscale image
	 * @param w the width of the image
	 * @param h the height of the image
	 */
	public static void thin(double[] Ig, int w, int h)
	{		
		int c, t, b, l, r, tl, tr, bl, br;
		boolean THINNING = true;
		
		while(THINNING){
			THINNING = false;
			
			for(int x=1; x<w-1; x++){
				for(int y=1; y<h-1; y++){
					c = y*w+x;
					
					if(Ig[c] > 0){
						l = c-1;
						r = c+1;
						
						t = (y-1)*w+x;
						tl = t-1;
						tr = t+1;
						
						b = (y+1)*w+x;
						bl = b-1;
						br = b+1;
						
						if(Ig[tl]>0 && Ig[t]>0 && Ig[tr]>0){	//Top
							Ig[c] = 0;
							THINNING = true;
						}
						
						if(Ig[t]>0 && Ig[tr]>0 && Ig[r]>0){		//Top right
							Ig[c] = 0;
							THINNING = true;
						}
						
						if(Ig[tr]>0 && Ig[r]>0 && Ig[br]>0){	//Right
							Ig[c] = 0;
							THINNING = true;
						}
						
						if(Ig[r]>0 && Ig[br]>0 && Ig[b]>0){		//Bottom right
							Ig[c] = 0;
							THINNING = true;
						}
						
						if(Ig[br]>0 && Ig[b]>0 && Ig[bl]>0){	//Bottom
							Ig[c] = 0;
							THINNING = true;
						}
						
						if(Ig[b]>0 && Ig[bl]>0 && Ig[l]>0){		//Bottom left
							Ig[c] = 0;
							THINNING = true;
						}
						
						if(Ig[bl]>0 && Ig[l]>0 && Ig[tl]>0){	//Left
							Ig[c] = 0;
							THINNING = true;
						}
						
						if(Ig[l]>0 && Ig[tl]>0 && Ig[t]>0){		//Top Left
							Ig[c] = 0;
							THINNING = true;
						}
					}
				}
			}
		}
	}

	/**
   * Convolve an image with a given filter.
   * @param img the image
   * @param w the width of the image
   * @param h the height of the image
   * @param filter the filter
   * @return the convolved image
   */
  public static double[] convolve(double[] img, int w, int h, double[][] filter)
  {
  	int wf = filter[0].length;
  	int hf = filter.length;
  	double[] R = new double[w * h];
  	int xr = (wf % 2 == 1) ? (wf - 1) / 2 : wf / 2;
  	int yr = (hf % 2 == 1) ? (hf - 1) / 2 : hf / 2;
  	int xu, yv;
  	double sum = 0;
  	int x, y, u, v;
  
  	for(x = xr; x < (w - xr); x++){
  		for(y = yr; y < (h - yr); y++){
  			sum = 0;
  
  			for(u = 0; u < wf; u++){
  				xu = u + x - xr;
  
  				for(v = 0; v < hf; v++){
  					yv = v + y - yr;
  					sum += filter[v][u] * img[yv * w + xu];
  				}
  			}
  
  			R[y * w + x] = sum;
  		}
  	}
  
  	return R;
  }
  
  /**
   * Perform non-maximum suppression.
   * @param Ig a grayscale image
   * @param w the width of the image
   * @param h the height of the image
   * @return the result image with non-maxima suppressed
   */
  public static double[] nonMaximumSuppression(double[] Ig, int w, int h)
  {
  	double[] Ig_nms = new double[Ig.length];
  	double minv = 1e10;
  	int minx, miny, maxx, maxy;
  	int atxy, atuv;
  	boolean NONMAX;
  	
  	//Find minimum value
  	for(int i=0; i<Ig.length; i++){
  		if(Ig[i] < minv) minv = Ig[i];
  	}
  	
		for(int x=0; x<w; x++){
			for(int y=0; y<h; y++){
				atxy = y*w+x;
				
				minx = x - 1;
				miny = y - 1;
				maxx = x + 1;
				maxy = y + 1;
				
				if(minx < 0) minx = 0;
				if(miny < 0) miny = 0;
				if(maxx >= w) maxx = w - 1;
				if(maxy >= h) maxy = h - 1;
				
				NONMAX = false;
				
				for(int u=minx; u<=maxx; u++){
					for(int v=miny; v<=maxy; v++){
						atuv = v*w+u;
						
						if(atuv != atxy && Ig[atuv] >= Ig[atxy]){
							Ig_nms[atxy] = minv;
							NONMAX = true;
							break;
						}
					}
					
					if(NONMAX) break;
				}
				
				if(!NONMAX) Ig_nms[atxy] = Ig[atxy];
			}
		}
		
		return Ig_nms;
  }
  
  /**
   * Perform a type of hysteresis thresholding on the given grayscale image.
   * @param Ig the grayscale image
   * @param w the width of the image
   * @param h the height of th image
   * @param low the low threshold
   * @param high the high threshold
   * @return the threshold black and white image
   */
  public static double[] hysteresisThreshold(double[] Ig, int w, int h, double low, double high)
  {
  	double[] Ie = new double[Ig.length];
  	double[] Ig_low, Ig_high;
  	double[] Igrp_low, Igrp_high;
  	double[] tmp;
  
  	Ig_low = MatrixUtility.gt(Ig, low);
  	Ig_high = MatrixUtility.gt(Ig, high);
  	
  	Igrp_low = getGroups(Ig_low, w, h);
  	Igrp_high = getGroups(Ig_high, w, h);
  
  	tmp = MatrixUtility.unique(MatrixUtility.vector(Igrp_low, MatrixUtility.find(Igrp_high)));
  	Ie = MatrixUtility.ismember(Igrp_low, tmp);
  	
  	return Ie;
  }

	public static double[][] distanceTransform(double[][] Ibw)
  {
  	int h = Ibw.length;
  	int w = Ibw[0].length;
  	double[][] Id = MatrixUtility.matrix(h, w, -1);
  	int i, x, y, u, v;
  	int minx, maxx, miny, maxy;
  	double d;
  
  	Vector<Pixel> points = new Vector<Pixel>();
  	Vector<Pixel> points_next = new Vector<Pixel>();
  
  	//Find the foreground points
  	for(x = 0; x < w; x++){
  		for(y = 0; y < h; y++){
  			if(Ibw[y][x] > 0){
  				Id[y][x] = 0;
  				points_next.add(new Pixel(x, y, 0));
  			}
  		}
  	}
  
  	if(!points_next.isEmpty()){ //Assign distances to next layer of points
  		Id = MatrixUtility.matrix(h, w, -1);
  
  		while(!points_next.isEmpty()){
  			points = points_next;
  			points_next.clear();
  
  			for(i = 0; i < points.size(); i++){
  				x = points.get(i).x;
  				y = points.get(i).y;
  				d = points.get(i).value + 1;
  
  				minx = x - 1;
  				maxx = x + 1;
  				miny = y - 1;
  				maxy = y + 1;
  
  				if(minx < 0) minx = 0;
  				if(maxx >= w) maxx = w - 1;
  				if(miny < 0) miny = 0;
  				if(maxy >= h) maxy = h - 1;
  
  				for(u = minx; u <= maxx; u++){
  					for(v = miny; v <= maxy; v++){
  						if(Id[v][u] < 0){
  							Id[v][u] = d;
  							points_next.add(new Pixel(u, v, d));
  						}
  					}
  				}
  			}
  		}
  	}else{
  		Id = MatrixUtility.matrix(h, w, Double.MAX_VALUE);
  	}
  
  	return Id;
  }

	/**
   * Transform a polygon.
   *  @param points a polygon represented as an ordered sequence of points
   *  @param sx the scale factor in the x direction
   *  @param sy the scale factor in the y direction
   *  @param tx the translation in the x direction
   *  @param ty the translation in the y direction
   *  @return the transformed polygon
   */
  private static Vector<Pixel> transformPolygon(Vector<Pixel> points, double sx, double sy, double tx, double ty)
  {
    Vector<Pixel> points_new = new Vector<Pixel>();
    int x, y;
    
    for(int i=0; i<points.size(); i++){
      x = (int)Math.round(sx*((double)points.get(i).x)+tx);
      y = (int)Math.round(sy*((double)points.get(i).y)+ty);
      points_new.add(new Pixel(x, y)); 
    }
    
    return points_new;
  }
  
  /**
   * Load a *.pbm bitmap image.
   *  @param filename the filename
   *  @return the bitmap stored in a 2D array
   */
  public static double[][] load_PBM(String filename)
  {
    double[][] img = null;
    int w, h;
    int x, y;
    
    try{    
      BufferedReader ins = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
      String line = Utility.nextUncommentedLine(ins);
      
      if(line.equals("P1")){
        line = Utility.nextUncommentedLine(ins);
        
        //Read width and height
        Scanner sc = new Scanner(line);
        w = Integer.valueOf(sc.next());
        h = Integer.valueOf(sc.next());
        
        //Read in image data
        img = new double[h][w];
        x = 0;
        y = 0;
        
        while((line=Utility.nextUncommentedLine(ins)) != null){
          for(int i=0; i<line.length(); i++){
            img[y][x] = (line.charAt(i) == '1') ? 0.0 : 1.0;
            x++;
            
            if(x == w){ //Move to next row
              x = 0;
              y++;
            }
          }
        }
      }
      
      ins.close();
    }catch(Exception e){
      e.printStackTrace();
    }
    
    return img;
  }

	/**
   * Load a *.pgm grayscale image.
   *  @param filename the filename
   *  @return the image stored in a 2D array
   */
  public static double[][] load_PGM(String filename)
  {
    double[][] img = null;
    int w, h;
    double max;
    int x, y;
    
    try{    
      BufferedReader ins = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
      String line = Utility.nextUncommentedLine(ins);
      Vector<String> tmpv;
      
      if(line.equals("P2")){
        line = Utility.nextUncommentedLine(ins);
        
        //Read width and height
        Scanner sc = new Scanner(line);
        w = Integer.valueOf(sc.next());
        h = Integer.valueOf(sc.next());
        
        //Read maximum value
        max = Double.valueOf(Utility.nextUncommentedLine(ins));
        
        //Read in image data
        img = new double[h][w];
        x = 0;
        y = 0;
        
        while((line=Utility.nextUncommentedLine(ins)) != null){
          tmpv = Utility.split(line, ' ', true);
          
          for(int i=0; i<tmpv.size(); i++){
            img[y][x] = Double.valueOf(tmpv.get(i)) / max;
            x++;
            
            if(x == w){ //Move to next row
              x = 0;
              y++;
            }
          }
        }
      }
      
      ins.close();
    }catch(Exception e){
      e.printStackTrace();
    }
    
    return img;
  }

	/**
	 * Load an image from a file type supported by Java's ImageIO library.
	 * @param filename the filename
	 * @return the image stored as a BufferedImage
	 */
	public static BufferedImage load_BufferedImage(String filename)
	{
		BufferedImage image = null;
		
		try{
	    image = ImageIO.read(new File(filename));
		}catch(Exception e) {e.printStackTrace();}
	  
		return image;
	}

	/**
   * Load an image from a file type supported by Java's ImageIO library.
   *  @param filename the filename
   *  @return the image stored in a 2D ARGB array
   */
  public static int[][] load(String filename)
  {
  	int[] img1D;
  	int[][] img2D = null;
  	BufferedImage image;
  	int w, h;
  	
  	try{
      image = ImageIO.read(new File(filename));
      w = image.getWidth(null);
      h = image.getHeight(null);
      img1D = new int[w*h];
      image.getRGB(0, 0, w, h, img1D, 0, w);
      img2D = to2D(img1D, w, h);
  	}catch(Exception e) {e.printStackTrace();}
    
  	return img2D;
  }
  
	/**
   * Save a *.pbm bitmap image.
   *  @param filename the filename
   *  @param img the image
   */
  public static void save_PBM(String filename, double[][] img)
  {
    int w = img[0].length;
    int h = img.length;
    
    try{    
      BufferedWriter outs = new BufferedWriter(new FileWriter(filename));
      outs.write("P1"); outs.newLine();
      outs.write("#Created by NCSA ModelViewer"); outs.newLine();
      outs.write(w + " " + h); outs.newLine();
      
      for(int y=0; y<h; y++){
        for(int x=0; x<w; x++){
          outs.write(img[y][x] > 0 ? '0' : '1');
        }
        
        outs.newLine();
      }
      
      outs.close();
    }catch(Exception e){
      e.printStackTrace();
    }
  }
  
	/**
   * Save a *.pgm grayscale image.
   *  @param filename the filename
   */
  public static void save_PGM(String filename, double[][] img)
  {
  	int h = img.length;
  	int w = img[0].length;
    double mind = Double.MAX_VALUE;
    double maxd = -Double.MAX_VALUE;
    double sclI;
    int tmpi;
    
    //Find extreme values (to scale image intensity)
    for(int x=0; x<w; x++){
    	for(int y=0; y<h; y++){
    		 if(img[y][x] < mind) mind = img[y][x];
    		 if(img[y][x] > maxd) maxd = img[y][x];
    	}
    }
    
    sclI = 1.0 / (maxd-mind);
        
    try{    
      BufferedWriter outs = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename)));
      outs.write("P2"); outs.newLine();
      outs.write("#Created by NCSA ModelViewer"); outs.newLine();
      outs.write(w + " " + h); outs.newLine();
      outs.write("255"); outs.newLine();
      
      for(int y=0; y<h; y++){
        for(int x=0; x<w; x++){
          tmpi = (int)Math.round(255.0*(img[y][x]-mind)*sclI);
          outs.write(tmpi + " ");
        }
        
        outs.newLine();
      }
      
      outs.close();
    }catch(Exception e){
      e.printStackTrace();
    }
  }

	/**
   * Save an image to a file type supported by Java's ImageIO library.
   * @param filename the filename
   * @param img the image to save
   */
  public static void save(String filename, BufferedImage image)
  {
  	String type = Utility.getFilenameExtension(filename);
  	FileOutputStream fos;
  	
  	try{
  		fos = new FileOutputStream(filename);
      ImageIO.write(image, type, fos);
      fos.flush();
      fos.close();
  	}catch(Exception e) {e.printStackTrace();}
  }
  
	/**
   * Save an image to a file type supported by Java's ImageIO library.
   * @param filename the filename
   * @param img the image to save represented as a 2D array of ARGB integers
   */
  public static void save(String filename, int[][] img)
  {
  	save(filename, argb2image(img));
  }
  
	/**
	 * Save the icon of the given file to an image.
	 * @param imagename the image to save the icon to
	 * @param filename the name of the file whose icon we want
	 */
	public static void saveIcon(String imagename, String filename)
	{
		try{
			ShellFolder shell_folder = ShellFolder.getShellFolder(new File(filename));
			ImageIcon icon = new ImageIcon(shell_folder.getIcon(true));
			int[][] image = image2argb((BufferedImage)icon.getImage());
			int h = image.length;
			int w = image[0].length;
			
			//Change black background to white
			for(int x=0; x<w; x++){
				for(int y=0; y<h; y++){
					if(image[y][x] == 0){
						image[y][x] = 0xffffffff;
					}
				}
			}
						
	  	save(imagename, image);
		}catch(Exception e) {e.printStackTrace();}
	}
  
	/**
   * Draw the given point onto the given image.
   *  @param img the image to draw on
   *  @param w the width of the image
   *  @param h the height of the image
   *  @param p the point to draw
   *  @param color the color of the points
   */
  public static void drawPoint(int[] img, int w, int h, Pixel p, int color)
  {
    int at = p.y*w+p.x;   
    if(at < img.length) img[at] = color;
  }
  
  /**
   * Draw the given vector of points onto the given image.
   *  @param img the image to draw on
   *  @param w the width of the image
   *  @param h the height of the image
   *  @param points the points to draw
   *  @param color the color of the points
   */
  public static void drawPoints(int[] img, int w, int h, Vector<Pixel> points, int color)
  {
    int at;
    
    for(int i=0; i<points.size(); i++){
      at = points.get(i).y*w+points.get(i).x;   
      if(at < img.length) img[at] = color;
    }
  }
  
  /**
   * Draw the given points to an image and return it.
   *  @param points the points to draw
   *  @param w the width of the desired image
   *  @param h the height of the desired image
   *  @param color the color of the points
   *  @return an ARGB 1D image with the drawn points
   */
  public static int[] drawPoints(Vector<Pixel> points, int w, int h, int color)
  {
    int[] img = new int[w*h];
    
    drawPoints(img, w, h, points, color);
    
    return img;
  }
  
  /**
   * Draw multiple groups of points to an image and return it.
   *  @param img the image to draw to
   *  @param points the groups of points to draw
   *  @param w the width of the desired image
   *  @param h the height of the desired image
   *  @param color the color of the points
   */
  public static void drawPointGroups(int[] img, Vector<Vector<Pixel>> points, int w, int h, int color)
  {
    for(int i=0; i<points.size(); i++){
      drawPoints(img, w, h, points.get(i), color);
    }
  }
  
  /**
   * Draw multiple groups of points to an image and return it.
   *  @param points the groups of points to draw
   *  @param w the width of the desired image
   *  @param h the height of the desired image
   *  @return an ARGB 1D image with all the drawn point groups in different random colors
   */
  public static int[] drawPointGroups(Vector<Vector<Pixel>> points, int w, int h)
  {
    int[] img = new int[w*h];
    int color;
    
    for(int i=0; i<points.size(); i++){
      color = getRandomBrightColor();
      drawPoints(img, w, h, points.get(i), color);
    }
    
    return img;
  }
  
  /**
   * Draw multiple groups of points to an image and return it.
   *  @param points the groups of points to draw
   *  @param w the width of the desired image
   *  @param h the height of the desired image
   *  @param bg_color background color for the image
   *  @param fg_color foreground color for the image
   */
  public static int[] drawPointGroups(Vector<Vector<Pixel>> points, int w, int h, int bg_color, int fg_color)
  {
    int[] img = new int[w*h];
    
    for(int i=0; i<img.length; i++) img[i] = bg_color;
    
    for(int i=0; i<points.size(); i++){
      drawPoints(img, w, h, points.get(i), fg_color);
    }
    
    return img;
  }
  
  /**
   * Draw n sets of point groups to n different images and return them.
   *  @param points the sets of point groups
   *  @param w the width of the desired image
   *  @param h the height of the desired image
   *  @return the vector of ARGB 1D images with the drawn point groups
   */
  public static Vector<int[]> drawMultiplePointGroups(Vector<Vector<Vector<Pixel>>> points, int w, int h)
  {
    Vector<int[]> tmpv = new Vector<int[]>();
    
    for(int i=0; i<points.size(); i++){
      tmpv.add(drawPointGroups(points.get(i), w, h));
    }
    
    return tmpv;
  }
  
  /**
   * Draw a line segment between two points.  End points need not be in the image.
   *  @param img the image to draw to
   *  @param w the width of the image
   *  @param h the height of the image
   *  @param x0 the x-coordiante of the starting point of the segment
   *  @param y0 the y-coordiante of the starting point of the segment
   *  @param x1 the x-coordiante of the ending point of the segment
   *  @param y1 the y-coordiante of the ending point of the segment
   *  @param color the color of the segment in ARGB
   */
  public static void drawLine(int[] img, int w, int h, int x0, int y0, int x1, int y1, int color)
  {
    double dx = x1 - x0;
    double dy = y1 - y0;
    double m, b;   
    int tmpi;
    
    if(x0>=0 && x0<w && y0>=0 && y0<h) img[y0*w+x0] = color;
    
    if(Math.abs(dx) > Math.abs(dy)){
      m = dy / dx;
      b = ((double)y0) - m*((double)x0);
      
      if(x1 < x0){
        tmpi = x0;
        x0 = x1;
        x1 = tmpi;
      }
      
      if(x0 < 0) x0 = 0;
      if(x1 >= w) x1 = w-1;
      
      while(x0 < x1){
        x0++;
        y0 = (int)Math.round(((double)x0)*m + b);
        if(y0 >= 0 && y0 < h) img[y0*w+x0] = color;
      }
    }else{
      m = dx / dy;
      b = ((double)x0) - m*((double)y0);
      
      if(y1 < y0){
        tmpi = y0;
        y0 = y1;
        y1 = tmpi;
      }
      
      if(y0 < 0) y0 = 0;
      if(y1 >= h) y1 = h-1;
      
      while(y0 < y1){
        y0++;
        x0 = (int)Math.round(((double)y0)*m + b);
        if(x0 >= 0 && x0 < w) img[y0*w+x0] = color;
      }
    }
  }

	/**
   * Draw an infinite length line to an image.
   *  @param img the image to draw to
   *  @param w the width of the image
   *  @param h the height of the image
   *  @param m the slope of the line
   *  @param b the y-intercept of the line
   *  @param color the color of the line in RGBA
   */
  public static void drawLine(int[] img, int w, int h, double m, double b, int color)
  {
    int x, y;
    
    if(Math.abs(m) < 1.0){
      for(x=0; x<w; x++){
        y = (int)Math.round(((double)x)*m + b);
        if(y>=0 && y<h) img[y*w+x] = color;
      }
    }else{
      m = 1.0/m;
      b = -m*b;
      
      for(y=0; y<h; y++){
        x = (int)Math.round(((double)y)*m + b);
        if(x>=0 && x<w) img[y*w+x] = color;
      }
    }
  }

	/**
   * Draw a line segment between two points.  End points need not be in the image.
   *  @param img the image to draw to
   *  @param w the width of the image
   *  @param h the height of the image
   *  @param x0 the x-coordiante of the starting point of the segment
   *  @param y0 the y-coordiante of the starting point of the segment
   *  @param x1 the x-coordiante of the ending point of the segment
   *  @param y1 the y-coordiante of the ending point of the segment
   *  @param color the color of the segment in ARGB
   */
  public static void drawLine(int[] img, int w, int h, double x0, double y0, double x1, double y1, int color)
  {
    int x0i = (int)Math.round(x0);
    int y0i = (int)Math.round(y0);
    int x1i = (int)Math.round(x1);
    int y1i = (int)Math.round(y1);
    
    drawLine(img, w, h, x0i, y0i, x1i, y1i, color);
  }

	/**
   * Draw an infinite length line to an image.
   *  @param img the image to draw to
   *  @param w the width of the image
   *  @param h the height of the image
   *  @param p0 a point on the line
   *  @param theta the angle of the line
   *  @param color the color of the line
   */
  public static void drawLine(int[] img, int w, int h, Pixel p0, double theta, int color)
  {
    double m = Math.tan(theta*Math.PI/180.0);
    double b = p0.y - m*p0.x;
    
    drawLine(img, w, h, m, b, color);
  }
  
	/**
   * Draw line segments.  End points need not be in the image.
   * @param img the image to draw to
   * @param w the width of the image
   * @param h the height of the image
   * @param color the color of the segment in ARGB
   */
	public static void drawLines(int[] image, int w, int h, Vector<Pixel[]> lines, int color)
	{
		for(Pixel[] line : lines){
			ImageUtility.drawLine(image, w, h, line[0].x, line[0].y, line[1].x, line[1].y, color);
		}
	}

	/**
   * Draw a polygon to an image.
   *  @param img the image to draw to
   *  @param w the width of the image
   *  @param h the height of the image
   *  @param points the polygon represented by an ordered sequence of points
   *  @param color the color of the polygon
   */
  public static void drawPolygon(int[] img, int w, int h, Vector<Pixel> points, int color)
  {
    Pixel p0;
    Pixel p1;
    
    for(int i=0; i<points.size(); i++){
      p0 = points.get(i);
      p1 = points.get((i+1)%points.size());
      drawLine(img, w, h, p0.x, p0.y, p1.x, p1.y, color);
    }
  }

	/**
   * Draw a box to an image.
   *  @param img the image to draw to
   *  @param w the width of the image
   *  @param h the height of the image
   *  @param minx the minimum x coordinate
   *  @param miny the minimum y coordinate
   *  @param maxx the maximum x coordinate
   *  @param maxy the maximum y coordinate
   *  @param color the color of the box
   */
  public static void drawBox(int[] img, int w, int h, int minx, int miny, int maxx, int maxy, int color)
  {
    drawLine(img, w, h, minx, miny, minx, maxy, color);
    drawLine(img, w, h, minx, miny, maxx, miny, color);
    drawLine(img, w, h, maxx, maxy, minx, maxy, color);
    drawLine(img, w, h, maxx, maxy, maxx, miny, color);
  }

	/**
   * Draw a box to an image.
   *  @param img the image to draw to
   *  @param w the width of the image
   *  @param h the height of the image
   *  @param minx the minimum x coordinate
   *  @param miny the minimum y coordinate
   *  @param maxx the maximum x coordinate
   *  @param maxy the maximum y coordinate
   *  @param color the color of the box
   */
  public static void drawBox(int[] img, int w, int h, double minx, double miny, double maxx, double maxy, int color)
  {
    int minxi = (int)Math.round(minx);
    int minyi = (int)Math.round(miny);
    int maxxi = (int)Math.round(maxx);
    int maxyi = (int)Math.round(maxy);
    
    drawBox(img, w, h, minxi, minyi, maxxi, maxyi, color);
  }

	/**
   * Draw a box to an image.
   *  @param img the image to draw to
   *  @param w the width of the image
   *  @param h the height of the image
   *  @param p the center of the box
   *  @param r the radius of the box
   *  @param color the color fo the box
   */
  public static void drawBox(int[] img, int w, int h, Pixel p, int r, int color)
  {
  	drawBox(img, w, h, p.x-r, p.y-r, p.x+r, p.y+r, color);
  }

	/**
   * Draw a box to an image.
   *  @param img the image to draw to
   *  @param w the width of the image
   *  @param h the height of the image
   *  @param x the x-coordinate of the center of the box
   *  @param y the y-coordinate of the center of the box
   *  @param r the radius of the box
   *  @param color the color fo the box
   */
  public static void drawBox(int[] img, int w, int h, int x, int y, int r, int color)
  {
  	drawBox(img, w, h, x-r, y-r, x+r, y+r, color);
  }
  
	/**
   * Draw boxes to an image.
   * @param img the image to draw to
   * @param w the width of the image
   * @param h the height of the image
   * @param centers the box centers
   * @param r the radius of the box
   * @param color the color fo the box
   */
  public static void drawBoxes(int[] img, int w, int h, Vector<Pixel> centers, int r, int color)
  {
  	int x, y;
  	
  	for(int i=0; i<centers.size(); i++){
  		x = centers.get(i).x;
  		y = centers.get(i).y;
  		drawBox(img, w, h, x-r, y-r, x+r, y+r, color);
  	}
  }
  
	/**
	 * Draw a box to the given graphics context.
	 * @param g the graphics context to draw to
	 * @param minx the minimum x value
	 * @param miny the minimum y value
	 * @param maxx the maximum x value
	 * @param maxy the maximum y value
	 * @param color the color of the box
	 * @param alpha the alpha value of then interior
	 */
	public static void drawBox(Graphics g, int minx, int miny, int maxx, int maxy, Color color, float alpha)
	{
		Graphics2D g2 = (Graphics2D)g;
		
		g2.setColor(color);
		
	  g2.drawLine(minx, miny, maxx, miny);		//Top
	  g2.drawLine(minx, maxy, maxx, maxy);		//Bottom
	  g2.drawLine(minx, miny, minx, maxy);		//Left
	  g2.drawLine(maxx, miny, maxx, maxy);		//Right
	  	  
	  if(alpha > 0){
			g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
		  g2.fillRect(minx+1, miny+1, maxx-minx-1, maxy-miny-1);
			g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
	  }
	}
	
	/**
	 * Draw a box to the given graphics context.
	 * @param g the graphics context to draw to
	 * @param box an array containing the box coordinates (minx, miny, maxx, maxy)
	 * @param color the color of the box
	 * @param alpha the alpha value of then interior
	 */
	public static void drawBox(Graphics g, int[] box, Color color, float alpha)
	{
		drawBox(g, box[0], box[1], box[2], box[3], color, alpha);
	}
  
	/**
   * Draw a triangle to an image via horizontal scan conversion.
   *  @param img the image to draw to
   *  @param w the width of the image
   *  @param h the height of the image
   *  @param p0 a point on the triangle
   *  @param p1 a point on the triangle
   *  @param p2 a point on the triangle
   *  @param color the color in ARGB
   */
  public static void drawTriangle(int[] img, int w, int h, Pixel p0, Pixel p1, Pixel p2, int color)
  {
  	Pixel p_miny, p_midy, p_maxy;
  	double m1i, m2i, tmpd1, tmpd2;
  	int minx, maxx;
  	
  	//Set extreme points in the y-axis
  	p_miny = p0;
  	if(p1.y < p_miny.y) p_miny = p1;
  	if(p2.y < p_miny.y) p_miny = p2;
  	
  	p_maxy = p0;
  	if(p1.y > p_maxy.y) p_maxy = p1;
  	if(p2.y > p_maxy.y) p_maxy = p2;
  	
  	if(p0 != p_miny && p0 != p_maxy){
  		p_midy = p0;
  	}else if(p1 != p_miny && p1 != p_maxy){
  		p_midy = p1;
  	}else{
  		p_midy = p2;
  	}
  
  	//Scan traingle
  	if(p_miny.y == p_midy.y){		//Special case with horizontal top edge
  		m1i = (double)(p_maxy.x-p_miny.x) / (double)(p_maxy.y-p_miny.y);
  		m2i = (double)(p_maxy.x-p_midy.x) / (double)(p_maxy.y-p_midy.y);
  		tmpd1 = p_miny.x;
  		tmpd2 = p_midy.x;
  		
  		for(int y=p_miny.y; y<p_maxy.y; y++){
  			if(tmpd1 < tmpd2){
  				minx = (int)Math.round(tmpd1);
  				maxx = (int)Math.round(tmpd2);
  			}else{
  				minx = (int)Math.round(tmpd2);
  				maxx = (int)Math.round(tmpd1);
  			}
  			
  			for(int x=minx; x<=maxx; x++){
  				img[y*w+x] = color;
  			}
  			
  			tmpd1 += m1i;
  			tmpd2 += m2i;
  		}
  	}else{											//Typical case
  		m1i = (double)(p_maxy.x-p_miny.x) / (double)(p_maxy.y-p_miny.y);
  		m2i = (double)(p_midy.x-p_miny.x) / (double)(p_midy.y-p_miny.y);
  		tmpd1 = p_miny.x;
  		tmpd2 = p_miny.x;
  		
  		for(int y=p_miny.y; y<p_midy.y; y++){
  			if(tmpd1 < tmpd2){
  				minx = (int)Math.round(tmpd1);
  				maxx = (int)Math.round(tmpd2);
  			}else{
  				minx = (int)Math.round(tmpd2);
  				maxx = (int)Math.round(tmpd1);
  			}
  			
  			for(int x=minx; x<=maxx; x++){
  				img[y*w+x] = color;
  			}
  			
  			tmpd1 += m1i;
  			tmpd2 += m2i;
  		}
  		
  		m2i = (double)(p_maxy.x-p_midy.x) / (double)(p_maxy.y-p_midy.y);
  		
  		for(int y=p_midy.y; y<p_maxy.y; y++){
  			if(tmpd1 < tmpd2){
  				minx = (int)Math.round(tmpd1);
  				maxx = (int)Math.round(tmpd2);
  			}else{
  				minx = (int)Math.round(tmpd2);
  				maxx = (int)Math.round(tmpd1);
  			}
  			
  			for(int x=minx; x<=maxx; x++){
  				img[y*w+x] = color;
  			}
  			
  			tmpd1 += m1i;
  			tmpd2 += m2i;
  		}
  	}
  }
 
  /**
   * Draw an ellipse to the image.
   * @param Irgb the image to draw to
   * @param e the ellipse to draw
   * @param rgb the color of the ellipse
   */
  public static void drawEllipse(int[][] Irgb, Ellipse e, int rgb)
  {
  	int h = Irgb.length;
  	int w = Irgb[0].length;
    double xu, yv, xx, yy;
    int x, y;
    
    for(double t=0; t<2.0*Math.PI; t+=Math.PI/200.0){
      xu = e.xr*Math.cos(t);
      yv = e.yr*Math.sin(t);

      //xx = xu*Math.cos(e.theta) - yv*Math.sin(e.theta);
      //yy = xu*Math.sin(e.theta) - yv*Math.cos(e.theta);
      xx = xu*Math.cos(e.theta) + yv*Math.sin(e.theta);	//FIX: above should be this!
      yy = -xu*Math.sin(e.theta) + yv*Math.cos(e.theta);

      x = (int)Math.round(e.x + xx);
      y = (int)Math.round(e.y + yy);

      if(x >= 0 && x < w && y >= 0 && y < h){
        Irgb[y][x] = rgb;
      }
    }
  }
  
  /**
   * Draw ellipses to an image.
   * @param Irgb the image to draw to
   * @param ellipses the ellipses to draw
   * @param rgb the color of the ellipses
   */
  public static void drawEllipses(int[][] Irgb, Vector<Ellipse> ellipses, int rgb)
  {
  	for(int i=0; i<ellipses.size(); i++){
  		drawEllipse(Irgb, ellipses.get(i), rgb);
  	}
  }

	/**
   * Create a new ARGB image stored as a 1D integer array.
   *  @param w the width of the image
   *  @param h the height of the image
   *  @param color the default color of the image
   *  @return the ARGB image
   */
  public static int[] getNewARGBImage(int w, int h, int color)
  {
    int[] img = new int[w*h];
    
    for(int i=0; i<img.length; i++){
      img[i] = color;
    }
    
    return img;
  }

  /**
   * Get a screen capture from the first screen.
   * @return an image of the screen as a 2D integer array in ARGB format
   */
  public static int[][] getScreen()
  {
  	int[][] Irgb = null;
  	
  	try{
  		BufferedImage buffered_image = new Robot().createScreenCapture(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));
  		Irgb = image2argb(buffered_image);
  	}catch(Exception e) {e.printStackTrace();}
  	
  	return Irgb;
  }
  
  /**
   * Get the rectangle describing the given screen.
   * @param screen the number of the desired screen
   * @return a rectangle giving the position and dimensions of the desired screen
   */
  public static Rectangle getScreenRectangle(int screen)
  {
  	Rectangle rectangle = null;
  	int x, width, height;
  	
  	if(screen >= 0){
			GraphicsDevice[] screens = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
	
			if(screen <= screens.length){
				x = 0;
	
				for(int i=0; i<screen-1; i++){
					x += screens[i].getDisplayMode().getWidth();
				}
				
				width = screens[screen-1].getDisplayMode().getWidth();
				height = screens[screen-1].getDisplayMode().getHeight();
				
				rectangle = new Rectangle(x, 0, width, height);
			}
  	}
		
  	return rectangle;
  }
  
  /**
   * Get a screen capture.
   * @param rectangle the rectangle to capture
   * @return an image of the screen as a 2D integer array in ARGB format
   */
  public static int[][] getScreen(Rectangle rectangle)
  {
  	int[][] Irgb = null;
		
  	try{
  		BufferedImage buffered_image = new Robot().createScreenCapture(rectangle);
  		Irgb = image2argb(buffered_image);
  	}catch(Exception e) {e.printStackTrace();}
  	
  	return Irgb;
  }
  
	/**
   * Create a random color
   *  @return the random color color
   */
  public static int getRandomColor()
  {
    int r = (int)Math.round(Math.random() * 255.0);
    int g = (int)Math.round(Math.random() * 255.0);
    int b = (int)Math.round(Math.random() * 255.0);
    int color = r << 16 | g << 8 | b;
    
    return color;
  }

	/**
   * Create a random bright color
   *  @return the random color color
   */
  public static int getRandomBrightColor()
  {
    int r = (int)Math.round(Math.random() * 128.0 + 127.0);
    int g = (int)Math.round(Math.random() * 128.0 + 127.0);
    int b = (int)Math.round(Math.random() * 128.0 + 127.0);
    int color = r << 16 | g << 8 | b;
    
    return color;
  }

	/**
   * Generate a color in between two other colors.
   *  @param c0 the starting color
   *  @param c1 the ending color
   *  @param t the parameter governing how much of each color is to be used
   *  @return the interpolated color
   */
  public static int getGradientColor(int c0, int c1, double t)
  {
    double r0 = (c0 >> 16) & 0x000000ff;
    double g0 = (c0 >> 8) & 0x000000ff;
    double b0 = c0 & 0x000000ff;
    double r1 = (c1 >> 16) & 0x000000ff;
    double g1 = (c1 >> 8) & 0x000000ff;
    double b1 = c1 & 0x000000ff;
    
    int rt = (int)Math.round((1.0-t)*((double)r0) + t*((double)r1));
    int gt = (int)Math.round((1.0-t)*((double)g0) + t*((double)g1));
    int bt = (int)Math.round((1.0-t)*((double)b0) + t*((double)b1));
    int ct =  0xff000000 | (rt << 16) | (gt << 8) | bt;
    
    return ct;
  }

	/**
   * Use finite difference to calculate the x and y direction derivatives of the given grayscale image.
   * @param I a grayscale image
   * @return the x and y direction derivatives of the given image
   */
  public static Pair<double[][],double[][]> getGradient(double[][] I)
  {
  	int w = I[0].length;
  	int h = I.length;
  	double[][] Ix = new double[h][w];
  	double[][] Iy = new double[h][w];
  
  	for(int x = 0; x < w; x++){
  		for(int y = 0; y < h; y++){
  			if(x == 0){
  				Ix[y][x] = I[y][x + 1] - I[y][x];
  			}else if(x < w - 1){
  				Ix[y][x] = 0.5 * (I[y][x + 1] - I[y][x - 1]);
  			}else{
  				Ix[y][x] = I[y][x] - I[y][x - 1];
  			}
  
  			if(y == 0){
  				Iy[y][x] = I[y + 1][x] - I[y][x];
  			}else if(y < h - 1){
  				Iy[y][x] = 0.5 * (I[y + 1][x] - I[y - 1][x]);
  			}else{
  				Iy[y][x] = I[y][x] - I[y - 1][x];
  			}
  		}
  	}
  
  	return new Pair<double[][], double[][]>(Ix, Iy);
  }

	/**
   * Create a gaussian filter.
   * @param w the width of the filter
   * @param h the height of the filter
   * @param sigma the standard deviation of gaussian
   * @return the filter
   */
  private static double[][] fgaussian(int w, int h, double sigma)
  {
  	double[][] F = new double[h][w];
  	double sigma_sqrd = sigma * sigma;
  	double C = 1.0 / (2.0 * Math.PI * sigma_sqrd);
  	double xr, yr;
  	double xtmp, ytmp;
  
  	xr = w / 2.0 - 0.5;
  	yr = h / 2.0 - 0.5;
  
  	for(int x = 0; x < w; x++){
  		for(int y = 0; y < h; y++){
  			xtmp = x - xr;
  			ytmp = y - yr;
  			F[y][x] = C * Math.exp(-0.5 * ((xtmp * xtmp + ytmp * ytmp) / sigma_sqrd));
  		}
  	}
  
  	return F;
  }

	/**
   * Create a laplacian of gaussian filter.
   * @param w the width of the filter
   * @param h the height of the filter
   * @param sigma the standard deviation of the gaussian
   * @return the filter
   */
  private static double[][] flog(int w, int h, double sigma)
  {
  	double[][] F = new double[h][w];
  	double sigma_sqrd = sigma * sigma;
  	double C = -1.0 / (Math.PI * sigma_sqrd * sigma_sqrd);
  	double xr, yr;
  	double xtmp, ytmp;
  	double x_sqrd, y_sqrd;
  
  	xr = w / 2.0 - 0.5;
  	yr = h / 2.0 - 0.5;
  
  	//To make consitent with matlab version start pixel indexing at 1.
  	for(int x = 0; x < w; x++){
  		for(int y = 0; y < h; y++){
  			xtmp = x - xr;
  			ytmp = y - yr;
  			x_sqrd = xtmp * xtmp;
  			y_sqrd = ytmp * ytmp;
  			F[y][x] = C * (1.0 - ((x_sqrd + y_sqrd) / (2.0 * sigma_sqrd))) * Math.exp(-0.5 * ((x_sqrd + y_sqrd) / sigma_sqrd));
  		}
  	}
  
  	return F;
  }

	/**
   * Create a filter.
   * @param type the type of filter to create
   * @param sigma the standard deviation of the filter
   * @param elongation the length of the filter
   * @param theta the rotation of the filter
   * @return the filter
   */
  public static double[][] getFilter(Option type, double sigma, double elongation, double theta)
  {
    double[][] F = null;
    double[][] Fx;
    double[][] Fy;
    double[][] Ftmp;
    Pair<double[][],double[][]> tmpp;
    int w = (int)Math.round(8.0*sigma + 1.0);
    int h = w;
  
    if(type == Option.SOBEL){
    	F = new double[3][3];
    	
    	if(theta == 0){
      	F[0][0] = 1;	F[0][1] = 2;	F[0][2] = 1;
      	F[1][0] = 0;	F[1][1] = 0;	F[1][2] = 0;
      	F[2][0] =-1;	F[2][1] =-2;	F[2][2] =-1;
    	}else if(theta > 0){
      	F[0][0] = 0;	F[0][1] = 1;	F[0][2] = 2;
      	F[1][0] =-1;	F[1][1] = 0;	F[1][2] = 1;
      	F[2][0] =-2;	F[2][1] =-1;	F[2][2] = 0;
    	}else{
      	F[0][0] = 2;	F[0][1] = 1;	F[0][2] = 0;
      	F[1][0] = 1;	F[1][1] = 0;	F[1][2] =-1;
      	F[2][0] = 0;	F[2][1] =-1;	F[2][2] =-2;
    	}
    	
    	//normL1(F);
    }else if(type == Option.SPOT){
    	F = flog(w, h, sigma);
    	normalizeL1(F);
    }else{
      double[][] R = new double[2][2];
      R[0][0] =  Math.cos(theta); R[0][1] = Math.sin(theta);
      R[1][0] = -Math.sin(theta); R[1][1] = Math.cos(theta);
  
      double[][] S = MatrixUtility.eye(2);
      S[0][0] = elongation;
  
      double[][] M = MatrixUtility.mtimes(R, S);
      
      if(type == Option.EDGE){
        Ftmp = fgaussian(w, h, sigma);
        tmpp = getGradient(Ftmp);
        Fx = tmpp.first;
        F = tmpp.second;
        F = warp(F, M, 0, Option.FULL);
        normalizeL1(F);
      }else if(type == Option.BAR){
        Ftmp = fgaussian(w, h, sigma);
        tmpp = getGradient(Ftmp);
        Fx = tmpp.first;
        Fy = tmpp.second;
        tmpp = getGradient(Fy);
        Fx = tmpp.first;
        F = tmpp.second;
        F = warp(F, M, 0, Option.FULL);
        normalizeL1(F);
      }
    }
  
    return F;
  }

	/**
   * Get the points on the line between the given endpoints.  End points need not be in the image.
   *  @param img the image to draw to
   *  @param w the width of the image
   *  @param h the height of the image
   *  @param x0 the x-coordiante of the starting point of the segment
   *  @param y0 the y-coordiante of the starting point of the segment
   *  @param x1 the x-coordiante of the ending point of the segment
   *  @param y1 the y-coordiante of the ending point of the segment
   */
  public static Vector<Pixel> getLinePoints(int[] img, int w, int h, int x0, int y0, int x1, int y1)
  {
  	Vector<Pixel> points = new Vector<Pixel>();
    double dx = x1 - x0;
    double dy = y1 - y0;
    double m, b;   
    int tmpi;   
    
    if(x0>=0 && x0<w && y0>=0 && y0<h) points.add(new Pixel(x0, y0, img[y0*w+x0]));
    
    if(Math.abs(dx) > Math.abs(dy)){
      m = dy / dx;
      b = ((double)y0) - m*((double)x0);
      
      if(x1 < x0){
        tmpi = x0;
        x0 = x1;
        x1 = tmpi;
      }
      
      if(x0 < 0) x0 = 0;
      if(x1 >= w) x1 = w-1;
      
      while(x0 < x1){
        x0++;
        y0 = (int)Math.round(((double)x0)*m + b);
        if(y0 >= 0 && y0 < h) points.add(new Pixel(x0, y0, img[y0*w+x0]));
      }
    }else{
      m = dx / dy;
      b = ((double)x0) - m*((double)y0);
      
      if(y1 < y0){
        tmpi = y0;
        y0 = y1;
        y1 = tmpi;
      }
      
      if(y0 < 0) y0 = 0;
      if(y1 >= h) y1 = h-1;
      
      while(y0 < y1){
        y0++;
        x0 = (int)Math.round(((double)y0)*m + b);
        if(x0 >= 0 && x0 < w) points.add(new Pixel(x0, y0, img[y0*w+x0]));
      }
    }
    
    return points;
  }

	/**
   * Get the points on the line between the given endpoints.  End points need not be in the image.
   *  @param img the image to draw to
   *  @param w the width of the image
   *  @param h the height of the image
   *  @param x0 the x-coordiante of the starting point of the segment
   *  @param y0 the y-coordiante of the starting point of the segment
   *  @param x1 the x-coordiante of the ending point of the segment
   *  @param y1 the y-coordiante of the ending point of the segment
   *  @param r the thickness of the line
   */
  public static Vector<Pixel> getLinePoints(int[] img, int w, int h, int x0, int y0, int x1, int y1, int r)
  {
  	Vector<Pixel> p0 = getLinePoints(img, w, h, x0, y0, x1, y1);
  	TreeSet<Pixel> p1 = new TreeSet<Pixel>();
  	Vector<Pixel> points = new Vector<Pixel>();
  	int minu, maxu, minv, maxv;
  	int x, y;
  	
  	for(int i=0; i<p0.size(); i++){
  		x = p0.get(i).x;
  		y = p0.get(i).y;
  		
  		minu = x - r;
  		maxu = x + r;
  		minv = y - r;
  		maxv = y + r;
  		
  		if(minu < 0) minu = 0;
  		if(maxu >= w) maxu = w-1;
  		if(minv < 0) minv = 0;
  		if(maxv >= h) maxv = h-1;
  		
  		for(int u=minu; u<=maxu; u++){
  			for(int v=minv; v<=maxv; v++){
  				p1.add(new Pixel(u, v, img[v*w+x]));
  			}
  		}
  	}
    
    Iterator<Pixel> itr = p1.iterator();
    
    while(itr.hasNext()){
    	points.add(itr.next());
    }
        
    return points;
  }

	/**
   * Get the points on the line between the given endpoints.  End points need not be in the image.
   *  @param img the image to draw to
   *  @param w the width of the image
   *  @param h the height of the image
   *  @param p0 the starting point of the segment
   *  @param p1 the ending point of the segment
   *  @param r the thickness of the line
   */
  public static Vector<Pixel> getLinePoints(int[] img, int w, int h, Pixel p0, Pixel p1, int r)
  {
  	return getLinePoints(img, w, h, p0.x, p0.y, p1.x, p1.y, r);
  }

	/**
   * Get the groups of nearby pixels within a binary image.
   *  @param mask the binary image whose pixels we wish to group
   *  @param r the distance with which to merge two groups
   *  @return the list of points within each group
   */
  public static Vector<Vector<Pixel>> getGroups(double[][] mask, int r)
  {
  	Vector<Vector<Pixel>> groups = new Vector<Vector<Pixel>>();
  	Vector<Pixel> group;
  	Stack<Pixel> stk = new Stack<Pixel>();  	
  	int w = mask[0].length;
  	int h = mask.length;
  	boolean[][] marked = new boolean[h][w];
  	int minu, maxu, minv, maxv;
  	Pixel p;
  	
  	for(int x=0; x<w; x++){
  		for(int y=0; y<h; y++){
  			if(mask[y][x] > 0 && !marked[y][x]){
  				group = new Vector<Pixel>();
  				stk.push(new Pixel(x, y));
  			  
  		  	while(!stk.isEmpty()){
  		  		p = stk.pop();
  		  		marked[p.y][p.x] = true;
  		  		group.add(p);
  		  	
  		      minu = p.x - r;
  		      maxu = p.x + r;
  		      minv = p.y - r;
  		      maxv = p.y + r;
  		      
  		      if(minu < 0) minu = 0;
  		      if(maxu >= w) maxu = w-1;
  		      if(minv < 0) minv = 0;
  		      if(maxv >= h) maxv = h-1;
  		      
  		      for(int u=minu; u<=maxu; u++){
  		      	for(int v=minv; v<=maxv; v++){
  		      		if(mask[v][u] > 0 && !marked[v][u]){
  		      			stk.push(new Pixel(u, v));
  		      		}
  		      	}
  		  		}
  		  	}
  		  	
  		  	groups.add(group);
  			}
  		}
  	}
  	
  	return groups;
  }

	/**
   * Group nearby regions in a black and white image.  Similar to matlab's bwlabel function.
   * @param Id a black and white image
   * @param w the width of the image
   * @param h the height of the image
   * @return the group labels of each pixel in the black and white image
   */
  public static double[] getGroups(double[] Id, int w, int h)
  {
  	Vector<Vector<Pixel>> groups = getGroups(MatrixUtility.to2D(h, w, Id), 1);
  	double[][] Itmp = MatrixUtility.matrix(h, w, -1);
  	int x, y;
  	
  	for(int i=0; i<groups.size(); i++){
  		for(int j=0; j<groups.get(i).size(); j++){
  			x = groups.get(i).get(j).x;
  			y = groups.get(i).get(j).y;
  			Itmp[y][x] = i+1;
  		}
  	}
  	
  	return MatrixUtility.to1D(Itmp);
  }

	/**
   * Get the pixels making up each label region in a segmented image.
   * @param Is a labeled image
   * @return the points assigned to each label
   */
  public static Vector<Vector<Pixel>> getRegions(int[][] Is)
  {
  	int h = Is.length;
  	int w = Is[0].length;
    Vector<Vector<Pixel>> regions = new Vector<Vector<Pixel>>();
    int n = 0;
  
    for(int x=0; x<w; x++){
      for(int y=0; y<h; y++){
        if(Is[y][x] > n) n = Is[y][x];
      }
    }
  
    for(int i=0; i<=n; i++){
    	regions.add(new Vector<Pixel>());
    }
  
    for(int x=0; x<w; x++){
      for(int y=0; y<h; y++){
        if(Is[y][x] >= 0){
          regions.get(Is[y][x]).add(new Pixel(x,y));
        }
      }
    }
  
    return regions;
  }

	/**
   * Get the mean color of the given segments.
   * @param Is the labeled segments within the color image
   * @param Irgb the color image
   * @return the mean color for each segment
   */
  public static Vector<Integer> getRegionColors(int[][] Is, int[][] Irgb)
  {
  	int h = Irgb.length;
  	int w = Irgb[0].length;
  	Vector<Integer> colors = new Vector<Integer>();
    int[] sum_r;
    int[] sum_g;
    int[] sum_b;
    int[] count;
    int rgb, r, g, b;
  
    //Find largest label
    int maxi = 0;
    
    for(int x=0; x<w; x++){
      for(int y=0; y<h; y++){
        if(Is[y][x] > maxi) maxi = Is[y][x];
      }
    }
  
    //Build colors from region means
    sum_r = new int[maxi+1];
    sum_g = new int[maxi+1];
    sum_b = new int[maxi+1];
    count = new int[maxi+1];
  
    for(int x=0; x<w; x++){
      for(int y=0; y<h; y++){
        if(Is[y][x] >= 0){
  				rgb = Irgb[y][x];
  				r = (rgb >> 16) & 0x000000ff;
  				g = (rgb >> 8) & 0x000000ff;
  				b = rgb & 0x000000ff;
  
          sum_r[Is[y][x]] += r;
          sum_g[Is[y][x]] += g;
          sum_b[Is[y][x]] += b;
          count[Is[y][x]]++;
        }
      }
    }
  
    for(int i=0; i<=maxi; i++){
    	r = sum_r[i] / count[i];
    	g = sum_g[i] / count[i];
    	b = sum_b[i] / count[i];
    	
    	colors.add(r << 16 | g << 8 | b);
    }
    
    return colors;
  }

	/**
   * Obtain a vector of polylines, one for each regions border in the image.  
   * Note, supports clipped and pinched polygons but thus far that part of the code has not been tested.
   *  @param img the grayscale image in row major order
   *  @param w the image width
   *  @param h the image height
   *  @return the vector of polylines
   */
  public static Vector<Vector<Pixel>> getRegionBorders(double[] img, int w, int h, boolean CLIP_BOUNDARIES)
  {
    Vector<Vector<Pixel>> borders = new Vector<Vector<Pixel>>();
    TreeSet<Pixel> points = new TreeSet<Pixel>();
    Vector<Pixel> path;
    Vector<Pixel> path_backtracked = new Vector<Pixel>();    
    Pixel tmpp;   
    Pixel[][] marks = new Pixel[h][w];
    boolean FOLLOW;
    boolean FLIPPED;    
    boolean FOUND;
    int u, v;
    int tmpx, tmpy;
    
    //Set neighbor direction array for identifying border pixels
    int[] nx = {0, 1, 0, -1};
    int[] ny = {1, 0, -1, 0};
    
    //Find all white pixels bordering black pixels
    for(int x=0; x<w; x++){
      for(int y=0; y<h; y++){
        if(img[y*w+x] > 0.5){
          FOUND = false;
          
          for(int i=0; i<nx.length; i++){
            u = x + nx[i];
            v = y + ny[i];
  
            if(u>=0 && u<w && v>=0 && v<h){
              if(img[v*w+u] < 0.5){
                FOUND = true;
                break;
              }
            }else if(CLIP_BOUNDARIES){
              FOUND = true;
            }
          }
            
          if(FOUND){
            tmpp = new Pixel(x, y);
            points.add(tmpp);
            marks[y][x] = tmpp;
          }
        }
      }
    }
    
    //Set search direction array (starting at (-1,1) and sweeping clockwise)
    int[] dx = {-1, 0, 1, 1, 1, 0, -1, -1};
    int[] dy = {1, 1, 1, 0, -1, -1, -1, 0};
    int d0, di;
    
    //Get polylines representing borders
    while(!points.isEmpty()){
      path = new Vector<Pixel>();
  
      //Select a new starting point (set is sorted so should be leftmost/topmost point)
      tmpp = points.first();
      points.remove(tmpp);      
      marks[tmpp.y][tmpp.x] = null;
      d0 = 0;       //Set initial originating direction
      FOLLOW = true;
      FLIPPED = false;
      
      //Follow border around region
      while(FOLLOW){
        path.add(tmpp);
        
        di = (d0+1) % 8;  //No need to search where we came from
        FOUND = false;
        
        while(di != d0){
          u = tmpp.x + dx[di];
          v = tmpp.y + dy[di];
          
          if(u>=0 && u<w && v>=0 && v<h){
            if(marks[v][u] != null){
              tmpp = marks[v][u];
              points.remove(tmpp);
              marks[v][u] = null;
              d0 = (di+4) % 8;    //Set next originating direction to opposite of current direction
              FOUND = true;
              break;
            }
          }
          
          di = (di+1) % 8;        //Try next direction
        }
        
        if(!FOUND){
          tmpx = Math.abs(path.firstElement().x - path.lastElement().x);
          tmpy = Math.abs(path.firstElement().y - path.lastElement().y);
  
          if(tmpx <= 1 && tmpy <= 1){                           //Closed polygon
            borders.add(path);
            FOLLOW = false;
          }else{
            tmpx = path.lastElement().x;
            tmpy = path.lastElement().y;
            
            if(CLIP_BOUNDARIES && (tmpx==0 || tmpx==w-1 || tmpy==0 || tmpy==h-1)){   //Clipped polygon
              if(FLIPPED){
                borders.add(path);
                FOLLOW = false;
              }else{
                Vector<Pixel> tmpv = new Vector<Pixel>();
                for(int i=path.size()-1; i>0; i--){
                  tmpv.add(path.get(i));
                }
                
                tmpp = path.get(0);
                d0 = 0;   //Same originating direction as last time!
                path = tmpv;
                FLIPPED = true;
              }
            }else{                                              //Pinched polygon
              FOUND = false;
              
              for(int i=path.size()-1; i>=0; i--){              //Backtrack to search for alternate paths
                for(di=0; di<8; di++){
                  u = path.get(i).x + dx[di];
                  v = path.get(i).y + dy[di];
                  
                  if(u>=0 && u<w && v>=0 && v<h){
                    if(marks[v][u] != null){
                      tmpp = marks[v][u];
                      points.remove(tmpp);
                      marks[v][u] = null;
                      d0 = (di+4) % 8;    //Set next originating direction to opposite of current direction
                      FOUND = true;
                      break;
                    }
                  }
                  
                  di = (di+1) % 8;        //Try next direction
                }
                
                if(FOUND){
                  break;
                }else{
                  path_backtracked.add(path.get(i));
                }
              }
              
              if(!FOUND) FOLLOW = false;
            }
          }
        }
      }
      
      //Return to marks any backtracked points
      for(int i=0; i<path_backtracked.size(); i++){
        marks[path_backtracked.get(i).y][path_backtracked.get(i).x] = path_backtracked.get(i);
      }
      
      path_backtracked.clear();
    }
    
    return borders;
  }

	/**
   * Calculate the maximum RGB values between two ARGB images.
   * @param img1 the first image
   * @param img2 the second image
   * @return the pixel-wise max of the two images
   */
  public static int[] getMaxImage(int[] img1, int[] img2)
  {
  	int n = img1.length;
  	int[] img_max = new int[n];
  	int c1, r1, g1, b1, c2, r2, g2, b2, rm, gm, bm;
  	
  	for(int i=0; i<n; i++){
  		c1 = img1[i];
  	  r1 = (c1 >> 16) & 0x000000ff;
      g1 = (c1 >> 8) & 0x000000ff;
      b1 = c1 & 0x000000ff;
      
  		c2 = img2[i];
  	  r2 = (c2 >> 16) & 0x000000ff;
      g2 = (c2 >> 8) & 0x000000ff;
      b2 = c2 & 0x000000ff;
      
      rm = (r1 > r2) ? r1 : r2;
      gm = (g1 > g2) ? g1 : g2;
      bm = (b1 > b2) ? b1 : b2;
      
      img_max[i] =  0xff000000 | (rm << 16) | (gm << 8) | bm;
  	}
  	
  	return img_max;
  }

	/**
   * An implementation of Raskar's Non-Photorealistic rendering algorithm based on images taken under
   * varying flash locations.  The method uses the differing flashes to extract occluding contours as opposed
   * to all edges.
   * @param filenames the names of the files to use
   * @return a binary image containing the occluding contours
   */
  public static double[][] getOccludingContours(Vector<String> filenames)
  {
  	Vector<int[]> I = new Vector<int[]>();
  	Vector<double[]> Ig = new Vector<double[]>();
  	Vector<Double> Imean = new Vector<Double>();
  	Vector<double[]> Ir = new Vector<double[]>();
  	Vector<double[]> Irf = new Vector<double[]>();
  	Vector<double[]> Is = new Vector<double[]>();
  	int[] I_max;
  	double[] Ig_max;
  	double[] Is_max;
  	double[] Ie = null;
  	double[][] ftop, fright, fbottom, fleft;
  	int[][] Itmp;
  	int w=0, h=0;		//Should be the same for all images
  	
  	ImageViewer viewer = new ImageViewer();
  	
  	if(filenames.size() != 3 && filenames.size() != 4){
  		System.out.println("Error: occludingContours method does not operate on " + filenames.size() + " images!");
  		return null;
  	}
  	
  	//Load data
  	for(int i=0; i<filenames.size(); i++){
  		Itmp = load(filenames.get(i));
  		w = Itmp[0].length;
  		h = Itmp.length;
  		
  		I.add(ImageUtility.to1D(Itmp));
  		Ig.add(ImageUtility.argb2g(I.get(i), w, h));
  		Imean.add(MatrixUtility.mean(Ig.get(i)));
  	}
  	
  	//Calculate total mean
  	double mean = 0;
  	
  	for(int i=0; i<Imean.size(); i++){
  		mean += Imean.get(i);
  	}
  	
  	mean /= Imean.size();
  	
  	//Normalize intensities
  	for(int i=0; i<Ig.size(); i++){
  		MatrixUtility.timesEquals(Ig.get(i), mean/Imean.get(i));
  		if(viewer != null) viewer.add(Ig.get(i), w, h, false);
  	}
  	
  	//Compute max images
  	I_max = I.get(0);
  	Ig_max = Ig.get(0);
  	
  	for(int i=1; i<I.size(); i++){
  		I_max = getMaxImage(I_max, I.get(i));
  		Ig_max = MatrixUtility.max(Ig_max, Ig.get(i));
  	}
  	
  	if(viewer != null) viewer.add(Ig_max, w, h, false);
  	
  	//Compute ratio images
  	for(int i=0; i<Ig.size(); i++){
  		Ig.set(i, MatrixUtility.plus(Ig.get(i),0.02));
  	}
  	
  	Ig_max = MatrixUtility.plus(Ig_max, 0.02);
  	
  	for(int i=0; i<Ig.size(); i++){
  	  Ir.add(MatrixUtility.divide(Ig.get(i), Ig_max));
    	if(viewer != null) viewer.add(Ir.get(i), w, h, false);
  	}
  	
  	//Compute confidence map (note: VERY strong assumptions as to where the dark shadows are in each image!)
  	if(Ir.size() == 4){
    	ftop = getFilter(Option.SOBEL, 0, 0, 0);
    	fleft = MatrixUtility.transpose(ftop);
     	fbottom = MatrixUtility.uminus(ftop); 	
    	fright = MatrixUtility.uminus(fleft);
    	
    	//Convolve with appropriate filter so that shadow edges will have large negative values (assuming they are darker!).
    	Irf.add(convolve(Ir.get(0), w, h, ftop));
    	Irf.add(convolve(Ir.get(1), w, h, fright));
    	Irf.add(convolve(Ir.get(2), w, h, fbottom));
    	Irf.add(convolve(Ir.get(3), w, h, fleft));
  	}else if(Ir.size() == 3){
  		ftop = getFilter(Option.SOBEL, 0, 0, 0);
  		fbottom = MatrixUtility.uminus(ftop); 
    	fright = getFilter(Option.SOBEL, 0, 0, 90);
    	fleft = getFilter(Option.SOBEL, 0, 0, -90);
    	
    	//Convolve with appropriate filter so that shadow edges will have large negative values (assuming they are darker!).
    	Irf.add(convolve(Ir.get(0), w, h, fright));
    	Irf.add(convolve(Ir.get(1), w, h, fbottom));
    	Irf.add(convolve(Ir.get(2), w, h, fleft));
  	}
  
  	//Get rid of false edges along shadows
  	for(int i=0; i<Irf.size(); i++){
  		Is.add(MatrixUtility.times(Irf.get(i), MatrixUtility.gt(Irf.get(i), 0)));
  		if(viewer != null) viewer.add(Irf.get(i), w, h, false);
  		if(viewer != null) viewer.add(Is.get(i), w, h, false);
  	}
  	
  	Is_max = MatrixUtility.copy(Is.get(0));
  	
  	for(int i=0; i<Is.size(); i++){
  		Is_max = MatrixUtility.max(Is_max, Is.get(i));
  	}
  	
  	if(viewer != null) viewer.add(Is_max, w, h, false);
  	
  	//Threshold and hysteresis
  	Ie = hysteresisThreshold(Is_max, w, h, 0.5, 1.0);
  	if(viewer != null) viewer.add(Ie, w, h, false);
  	
  	return MatrixUtility.to2D(h, w, Ie);
  	//return MatrixUtils.to2D(h, w, Is_max);
  }

	/**
   * Attempt to extract an imaged objects silhouette using multiple flash images and Raskar's occluding contour
   * algorithm.
   * @param filenames the image files
   * @return the silhouette mask
   */
  public static double[][] getSilhouetteMask(Vector<String> filenames)
  {
  	double[][] Ie = getOccludingContours(filenames);
  	int h = Ie.length;
  	int w = Ie[0].length;
  	int h_small = 120;
  	int w_small = 160;
  	double[][] Ie_small = resizeBicubic(Ie, w_small, h_small);
  	Ie_small = smooth(Ie_small, 4);
  	double[][] Im_small = MatrixUtility.lt(getGeodesicActiveContour(Ie_small, 0.1, 100, 0.5, 1500), 0);
  	double[][] Im = resize(Im_small, w, h);
  			
  	return Im;
  }

	/**
   * Construct weigted edges between pixels and their neighbors.
   * @param Irgb a color image
   * @return the weigted edges
   */
  private static Vector<WeightedEdge> getWeightedPixelEdges(int[][] Irgb)
  {
  	Vector<WeightedEdge> edges = new Vector<WeightedEdge>();
  	int h = Irgb.length;
  	int w = Irgb[0].length;
  	double[] Ihsv = argb2hsv(Irgb);
  	int minu, maxu, minv, maxv;
  	int atxy, atuv, at3xy, at3uv;
  	double dist, tmpd;
  	
  	for(int x=0; x<w; x++){
  		for(int y=0; y<h; y++){
  			atxy = y*w+x;
  			at3xy = 3*atxy;
  			
  			minu = x-1;
  			maxu = x+1;
  			minv = y-1;
  			maxv = y+1;
  			
  			if(minu < 0) minu = 0;
  			if(maxu >= w) maxu = w-1;
  			if(minv < 0) minv = 0;
  			if(maxv >= h) maxv = h-1;
  			
  			for(int u=minu; u<=maxu; u++){
  				for(int v=minv; v<=maxv; v++){
  					if(u != x || v != y){
  						atuv = v*w+u;
  						at3uv = 3*atuv;
  						dist = 0;
  						
  						for(int i=0; i<3; i++){
  							tmpd = Ihsv[at3xy+i] - Ihsv[at3uv+i];
  							dist += tmpd * tmpd;
  						}
  						
  						//dist = Math.sqrt(dist);
  						
  						edges.add(new WeightedEdge(atxy, atuv, dist));
  					}
  				}
  			}
  		}
  	}
  	
  	return edges;
  }

	/**
   * An implementation of Felzenswalb et al's fast superpixel method.
   * @param Irgb a color image
   * @param k a factor proportional to the size of the super pixels
   * @param MERGE_SMALL true if small regions should be merged together
   * @return an image containg numbered superpixels
   */
  public static int[][] getSuperPixels(int[][] Irgb, double k, boolean MERGE_SMALL)
  {
  	int h = Irgb.length;
  	int w = Irgb[0].length;
  	int n = w*h;
  	int[][] Is = new int[h][w];
  	Vector<WeightedEdge> edges = getWeightedPixelEdges(Irgb);
  	DisjointSets sets = new DisjointSets(n);
  	boolean[] found;
    int[] map;  	
  	int atxy, s1, s2, s1_hat, s2_hat, count;
  	double tmpd, tmpd1, tmpd2;
    
  	Collections.sort(edges);
  	
  	//Merge pixels
  	for(int i=0; i<edges.size(); i++){
  		s1 = sets.find(edges.get(i).p0);
  		s2 = sets.find(edges.get(i).p1);
  		
  		if(s1 != s2){
  			s1_hat = sets.find(s1);
  			s2_hat = sets.find(s2);
  			
  			tmpd1 = sets.getValue(s1_hat) + k/sets.getSize(s1_hat);
  			tmpd2 = sets.getValue(s2_hat) + k/sets.getSize(s2_hat);
  			tmpd = (tmpd1 < tmpd2) ? tmpd1 : tmpd2;
  			
  			if(edges.get(i).w < tmpd){
  				sets.unionFind(s1, s2, edges.get(i).w);
  			}
  		}
  	}
  	
  	//Merge small regions
  	if(MERGE_SMALL) sets.mergeSmallSets(edges, 0.001*w*h);
  	
  	//Recover labels from the disjoint sets
  	found = new boolean[n];
    
  	for(int x=0; x<w; x++){
  		for(int y=0; y<h; y++){
  			atxy = y*w+x;
  			s1 = sets.find(atxy);
  			found[s1] = true;
  			Is[y][x] = s1;
  		}
  	}
  	
    //Construct compressed label map
  	map = new int[n];
    count = 0;
    
    for(int i=0; i<n; i++){
      if(found[i]){
        map[i] = count;
        count++;
      }
    }
  
    //Apply compressed label map
    for(int x=0; x<w; x++){
      for(int y=0; y<h; y++){
        Is[y][x] = map[Is[y][x]];
      }
    }
  	
  	return Is;
  }
  
  /**
   * Get the neighoring regions for each labeled region in a segmented image.
   * @param Is a labeled image
   * @return the neighbors of each labeled region
   */
  public static Vector<Vector<Integer>> getNeighbors(int[][] Is)
  {
  	int h = Is.length;
  	int w = Is[0].length;
    Vector<Vector<Pixel>> regions = getRegions(Is);
    int n = regions.size();

    Vector<Vector<Integer>> neighbors = new Vector<Vector<Integer>>();
    boolean[] mark = new boolean[n];
    int minx, maxx, miny, maxy;
    int x, y;
    
    for(int i=0; i<n; i++){
      neighbors.add(new Vector<Integer>());
    }

    for(int i=0; i<regions.size(); i++){
      if(regions.get(i).size() > 0){
        for(int j=0; j<regions.get(i).size(); j++){		//Mark this regions neighbors
          x = regions.get(i).get(j).x;
          y = regions.get(i).get(j).y;

          minx = x - 1;
          maxx = x + 1;
          miny = y - 1;
          maxy = y + 1;

          if(minx < 0) minx = 0;
          if(maxx >= w) maxx = w-1;
          if(miny < 0) miny = 0;
          if(maxy >= h) maxy = h-1;

          for(int u=minx; u<=maxx; u++){
            for(int v=miny; v<=maxy; v++){
              mark[Is[v][u]] = true;
            }
          }
        }

        for(int j=0; j<n; j++){
          if(i!=j && mark[j]) neighbors.get(i).add(j);
          mark[j] = false;
        }
      }
    }

    return neighbors;
  }

	/**
   * The stopping function for the geodesic active contours.
   * @param Ie an edge map
   * @return the modified image
   */
  private static double[][] gStop(double[][] Ie)
  {
  	int h = Ie.length;
  	int w = Ie[0].length;
    double[][] g = MatrixUtility.ones(h, w);
    double fx, fy, tmpd;
    double epsilon = 1e-5;
  
    for(int x=1; x<w-1; x++){
      for(int y=1; y<h-1; y++){
      	fx = 0.5*(Ie[y][x+1]-Ie[y][x-1]);
      	fy = 0.5*(Ie[y+1][x]-Ie[y-1][x]);
        tmpd = Math.sqrt(fx*fx+fy*fy+epsilon);
        
        g[y][x] = 1.0/(1.0+tmpd);
        //g[y][x] = 1.0/(1.0+tmpd*tmpd);
      }
    }
  
    return g;
  }

	/**
   * Reinitialize a level set with the distance transform.
   * @param u the level set to reinitialize
   */
  private static void reinitializeLevelSet(double[][] u)
  {
  	int h = u.length;
  	int w = u[0].length;
    double[][] Isd = distanceTransform(getZeroLevelSet(u));
    int x, y;
  
    for(x=0; x<w; x++){
      for(y=0; y<h; y++){
        if(u[y][x] > 0){
          u[y][x] = -Isd[y][x];
        }else if(u[y][x] < 0){
          u[y][x] = Isd[y][x];
        }
      }
    }
  }

	/**
   * Mark as a contours all zero-crossings within a grayscale image.
   * @param Ig a grayscale image
   * @return the zero-crossings (i.e. contours)
   */
  public static double[][] getZeroLevelSet(double[][] Ig)
  {
  	int h = Ig.length;
  	int w = Ig[0].length;
  	double[][] C = new double[h][w];
  	boolean FOUND_NEG;
  
  	for(int x = 1; x < w - 1; x++){
  		for(int y = 1; y < h - 1; y++){
  			if(Ig[y][x] >= 0){
  				FOUND_NEG = false;
  
  				for(int u = -1; u <= 1; u++){
  					if(!FOUND_NEG){
  						for(int v = -1; v <= 1; v++){
  							if(Ig[y + v][x + u] < 0){
  								FOUND_NEG = true;
  								break;
  							}
  						}
  					}
  				}
  
  				if(FOUND_NEG){
  					C[y][x] = 1;
  				}else{
  					C[y][x] = 0;
  				}
  			}else{
  				C[y][x] = 0;
  			}
  		}
  	}
  
  	return C;
  }

	/**
   * An implementation of Casellas' geodesic active contours.
   * @param Ie an edge map
   * @param c the continuity factor (higher equals smoother)
   * @param gamma the attraction to edges
   * @param dt the delta for each iteration
   * @param iterations the number of iterations to evolve the curve
   * @return the resulting level set
   */
  public static double[][] getGeodesicActiveContour(double[][] Ie, double c, double gamma, double dt, int iterations)
  {
  	Ie = MatrixUtility.times(Ie, gamma);
  	int h = Ie.length;
  	int w = Ie[0].length;
  	double[][] gIe = gStop(Ie);
  	double[][] u;
  	double[][] u_new = new double[h][w];
  	ImageViewer viewer = new ImageViewer();
  
    //Initialize with border
    u = MatrixUtility.matrix(h, w, 0.5);
    int margin = 2;
    
    for(int x=margin; x<w-margin; x++){
      for(int y=margin; y<h-margin; y++){
        u[y][x] = -0.5;
      }
    }
    
    //Iterativley update
    double ux, uy, uxx, uyy, uxy;
    double norm_grad_u, K, gIex, gIey;
    double du1, du1c, du2;
    double epsilon = 1e-5;
    
    for(int i=0; i<iterations; i++){
      if(i%200==0) reinitializeLevelSet(u);
  
      for(int x=1; x<w-1; x++){
        for(int y=1; y<h-1; y++){
          ux  = 0.5*(u[y][x+1]-u[y][x-1]);
          uy  = 0.5*(u[y+1][x]-u[y-1][x]);
          uxx = u[y][x+1]+u[y][x-1]-2.0*u[y][x];
          uyy = u[y+1][x]+u[y-1][x]-2.0*u[y][x];
          uxy = 0.25*(u[y+1][x+1]+u[y-1][x-1]-u[y+1][x-1]-u[y-1][x+1]);
          norm_grad_u = Math.sqrt(ux*ux+uy*uy+epsilon);
          K = (uxx*uy*uy-2.0*uxy*ux*uy+uyy*ux*ux)/(norm_grad_u*norm_grad_u*norm_grad_u);
          gIex = 0.5*(gIe[y][x+1]-gIe[y][x-1]);
          gIey = 0.5*(gIe[y+1][x]-gIe[y-1][x]);
  
          //Boundary force
          du1 = gIe[y][x]*K*norm_grad_u;
          du1c = gIe[y][x]*(c+K)*norm_grad_u;
          du2 = ux*gIex+uy*gIey;
          
          //u_new[y][x] = u[y][x] + dt*(du1 + du2);														//Eq(5)
          //u_new[y][x] = u[y][x] + dt*du1c;																	//Eq(6)
          u_new[y][x] = u[y][x] + dt*(du1 + du2 + c*gIe[y][x]*norm_grad_u);		//Eq(7) 
        }
      }
  
      for(int x=0; x<w; x++){
        for(int y=0; y<h; y++){
          u[y][x] = u_new[y][x];
        }
      }
      
      if(viewer!=null && i%10==0){
      	viewer.set(getZeroLevelSet(u), w, h);
      }
    }
    
    return u;
  }

	/**
   * An implementation of Caselles' geodesic active contours.
   * @param Ie an edge map
   * @return the resulting level set
   */
  public static double[][] getGeodesicActiveContour(double[][] Ie)
  {
  	return getGeodesicActiveContour(Ie, 0.1, 100, 0.5, 3000);
  }
  
	/**
	 * Apply positive masks to an image (i.e. removing everything not covered).
	 * @param image the image to mask
	 * @param w the width of the image
	 * @param h the height of the image
	 * @param masks a vector of areas to preserve represented as (minx, miny, maxx, maxy)
	 * @return the masked image
	 */
	public static double[] applyPositiveMasks(double[] image, int w, int h, Vector<int[]> masks)
	{
		int n = image.length;
		double[] masked_image = new double[n];
		int[] mask;
		int minx, miny, maxx, maxy;
		int at;
		
		for(int i=0; i<masks.size(); i++){
			mask = masks.get(i);
			minx = mask[0];
			miny = mask[1];
			maxx = mask[2];
			maxy = mask[3];
			
			if(minx < 0) minx = 0;
			if(miny < 0) miny = 0;
			if(maxx >= w) maxx = w-1;
			if(maxy >= h) maxy = h-1;
			
			for(int x=minx; x<=maxx; x++){
				for(int y=miny; y<=maxy; y++){
					at = y*w+x;
					masked_image[at] = image[at];
				}
			}
		}
		
		return masked_image;
	}
	
	/**
	 * Apply positive masks to an image (i.e. removing everything not covered).
	 * @param image the image to mask
	 * @param w the width of the image
	 * @param h the height of the image
	 * @param masks a vector of areas to preserve represented as (minx, miny, maxx, maxy)
	 * @return the masked image
	 */
	public static int[] applyPositiveMasks(int[] image, int w, int h, Vector<int[]> masks)
	{
		int n = image.length;
		int[] masked_image = new int[n];
		int[] mask;
		int minx, miny, maxx, maxy;
		int at;
		
		for(int i=0; i<masks.size(); i++){
			mask = masks.get(i);
			minx = mask[0];
			miny = mask[1];
			maxx = mask[2];
			maxy = mask[3];
			
			if(minx < 0) minx = 0;
			if(miny < 0) miny = 0;
			if(maxx >= w) maxx = w-1;
			if(maxy >= h) maxy = h-1;
			
			for(int x=minx; x<=maxx; x++){
				for(int y=miny; y<=maxy; y++){
					at = y*w+x;
					masked_image[at] = image[at];
				}
			}
		}
		
		return masked_image;
	}
	
	/**
	 * Apply negative masks to an image (i.e. removing areas covered).
	 * @param image the image to mask
	 * @param w the width of the image
	 * @param h the height of the image
	 * @param masks a vector of areas to remove represented as (minx, miny, maxx, maxy)
	 * @return the masked image
	 */
	public static double[] applyNegativeMasks(double[] image, int w, int h, Vector<int[]> masks)
	{
		double[] masked_image = Arrays.copyOf(image, image.length);
		int[] mask;
		int minx, miny, maxx, maxy;
		
		for(int i=0; i<masks.size(); i++){
			mask = masks.get(i);
			minx = mask[0];
			miny = mask[1];
			maxx = mask[2];
			maxy = mask[3];
			
			if(minx < 0) minx = 0;
			if(miny < 0) miny = 0;
			if(maxx >= w) maxx = w-1;
			if(maxy >= h) maxy = h-1;
			
			for(int x=minx; x<=maxx; x++){
				for(int y=miny; y<=maxy; y++){
					masked_image[y*w+x] = 0;
				}
			}
		}
		
		return masked_image;
	}
	
	/**
	 * Apply negative masks to an image (i.e. removing areas covered).
	 * @param image the image to mask
	 * @param w the width of the image
	 * @param h the height of the image
	 * @param masks a vector of areas to remove represented as (minx, miny, maxx, maxy)
	 * @return the masked image
	 */
	public static int[] applyNegativeMasks(int[] image, int w, int h, Vector<int[]> masks)
	{
		int[] masked_image = Arrays.copyOf(image, image.length);
		int[] mask;
		int minx, miny, maxx, maxy;
		
		for(int i=0; i<masks.size(); i++){
			mask = masks.get(i);
			minx = mask[0];
			miny = mask[1];
			maxx = mask[2];
			maxy = mask[3];
			
			if(minx < 0) minx = 0;
			if(miny < 0) miny = 0;
			if(maxx >= w) maxx = w-1;
			if(maxy >= h) maxy = h-1;
			
			for(int x=minx; x<=maxx; x++){
				for(int y=miny; y<=maxy; y++){
					masked_image[y*w+x] = 0;
				}
			}
		}
		
		return masked_image;
	}
	
	/**
   * A simple main for debug purposes only.
   *  @param args arguments to the program
   */
  public static void main(String args[])
  {
  	if(false){			//Test screen capture
  		ImageViewer viewer = new ImageViewer();
  		int[][] Irgb = getScreen();
  		int h = Irgb.length;
  		int w = Irgb[0].length;
  		
  		viewer.add(Irgb, w, h, false);
  	}
  	
  	if(false){			//Test convolution
	  	ImageViewer viewer = new ImageViewer();
	  	int[][] img_tmp = load("C:/Users/kmchenry/Files/Data/Images/scar1.jpg");
	  	int h = img_tmp.length;
	  	int w = img_tmp[0].length;
	  	int[] img = to1D(img_tmp);
	  	double[] img_g = argb2g(img, w, h);
	  	double[][] F = getFilter(Option.EDGE, 2, 1, 45);	F = MatrixUtility.transpose(F);
	  	
	  	viewer.add(img, w, h, true);
	  	viewer.add(img_g, w, h, true);
	  	viewer.add(MatrixUtility.to1D(F), F[0].length, F.length, true);
	  	viewer.add(convolve(img_g, w, h, F), w, h, true);
  	}
  	
  	if(false){			//Test Felzenswalb's super pixel method
	  	ImageViewer viewer = new ImageViewer();
	  	int[][] Irgb = load("C:/Kenton/Data/Images/Temp/scar1.jpg");
	  	int h = Irgb.length;
	  	int w = Irgb[0].length;
	  	int[][] Is;
	  	
	  	viewer.add(Irgb, w, h, false);
	  	Is = getSuperPixels(Irgb, 0.1, true);
	  	viewer.add(n2argb(Is, Irgb), w, h, true);
  	}
  	
  	if(false){				//Test Raskar's occluding contour method and Caselles' GAC's
  		Vector<String> filenames = new Vector<String>();
  		filenames.add("C:/Kenton/Data/Images/MERL/person/up.bmp");
  		filenames.add("C:/Kenton/Data/Images/MERL/person/right.bmp");
  		filenames.add("C:/Kenton/Data/Images/MERL/person/down.bmp");
  		filenames.add("C:/Kenton/Data/Images/MERL/person/left.bmp");

  		if(false){
	  		double[][] Ie = getOccludingContours(filenames);
	  		int h = Ie.length;
	  		int w = Ie[0].length;
	  		int h_small = (int)Math.round(0.1*h);
	  		int w_small = (int)Math.round(0.1*w);
	  		double[][] Ie_small = resizeBicubic(Ie, w_small, h_small);
	  		Ie_small = smooth(Ie_small, 4);
	  		ImageViewer.show(Ie_small, w_small, h_small);
	  		getGeodesicActiveContour(Ie_small);
  		}else{
  			double[][] Im = getSilhouetteMask(filenames);
  			ImageViewer.show(Im, "Silhouette Mask");
  		}
  	}
  	
  	if(false){				//Test silhouette acquisition
  		Vector<String> filenames = new Vector<String>();
  		filenames.add("C:\\Kenton\\Data\\Images\\Temp\\basketball2\\right.jpg");
  		filenames.add("C:\\Kenton\\Data\\Images\\Temp\\basketball2\\down.jpg");
  		filenames.add("C:\\Kenton\\Data\\Images\\Temp\\basketball2\\left.jpg");

			ImageViewer.show(getSilhouetteMask(filenames), "Silhouette Mask");
  	}
  	  	
  	if(false){			//Test aura based segmentation
	  	ImageViewer viewer = new ImageViewer();
	  	int[][] Irgb = load("C:/Kenton/Data/Images/Temp/Aura1/a.jpg");
	  	int h = Irgb.length;
	  	int w = Irgb[0].length;
	  	int[][] Is;
	  	
	  	viewer.add(Irgb, w, h, false);
	  	Is = getSuperPixels(Irgb, 0.1, false);
	  	viewer.add(n2argb(Is, Irgb), w, h, true);
  	}
  	
  	if(false){			//Test image rotation
  		ImageViewer viewer = new ImageViewer();
	  	int[][] Irgb = load("C:/Users/kmchenry/Files/Data/Images/scar1.jpg");
	  	double[] Ig = argb2g(Irgb);
	  	double[] Ig_r;
	  	int h = Irgb.length;
	  	int w = Irgb[0].length;
	  	
	  	Ig_r = rotate(Ig, w, h, 5, w/2, h/2);
	  	
	  	viewer.add(Ig, w, h, false);
	  	viewer.add(Ig_r, w, h, true);
  	}
  	
  	if(true){				//Test icon extraction
  		saveIcon("C:/Users/kmchenry/Files/Temp/icon.jpg", "C:/Users/kmchenry/Desktop/Notes.txt.lnk");
  	}
  }
}
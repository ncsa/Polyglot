package kgm.image;
import java.util.*;
import ij.*;
import mpicbg.ij.*;
import mpicbg.imagefeatures.*;
import mpicbg.trakem2.align.Align;

/**
 * Image utilities utilizing the Image J library.
 * @author Kenton McHenry
 */
public class ImageJUtility
{
	/**
	 * Convert an ARGB image into an ImageJ image.
	 * @param Irgb an ARGB image
	 * @return an ImageJ image
	 */
	public static ImagePlus rgb2imagej(int[][] Irgb)
	{
		int h = Irgb.length;
		int w = Irgb[0].length;
		ImagePlus Ij = IJ.createImage("", "RGB black", w, h, 1);	
		int[] pixels = (int[])Ij.getProcessor().getPixels();
		
		for(int y=0; y<h; y++){
			for(int x=0; x<w; x++){
				pixels[y*w+x] = Irgb[y][x];
			}
		}
		
		Ij.getProcessor().setPixels(pixels);
		
		return Ij;
	}
	
	/**
	 * Extract SIFT features.
	 * @param Irgb a color image
	 */
	public static Vector<Feature> getSIFTFeatures(int[][] Irgb)
	{
		ImagePlus Ij = rgb2imagej(Irgb);
		Align.Param p = Align.param.clone();
		SIFT sift;
		Vector<Feature> features;
		
		if(true){	//Change parameters
			p.sift.steps = 4;
		}
		
		if(true){	//Display SIFT parameters
			System.out.println();
			System.out.println("Scale Invariant Interest Point Detector");
			System.out.println("  initial sigma: " + p.sift.initialSigma);
			System.out.println("  steps: " + p.sift.steps);
			System.out.println("  min. octave size: " + p.sift.minOctaveSize);
			System.out.println("  max. octave size: " + p.sift.maxOctaveSize);
			System.out.println("Feature Descriptor");
			System.out.println("  size: " + p.sift.fdSize);
			System.out.println("  orientation bins: " + p.sift.fdBins);
			System.out.println("  closest/next closest ratio: " + p.rod);
			System.out.println("Geometric Consensus Filter");
			//System.out.println("  filter matches by geometric consensus: " + p.useGeometricConsensusFilter);
			System.out.println("  max. alignment error: " + p.maxEpsilon);
			System.out.println("  min. inlier ratio: " + p.minInlierRatio);
			//System.out.println("  min. number of inliners: " + p.minNumInliers);
			//System.out.println("  expected transformation: " + Param.modelStrings[p.modelIndex]);
		}
		
		sift = new SIFT(new FloatArray2DSIFT(p.sift));
		features = new Vector<Feature>(sift.extractFeatures(Ij.getProcessor()));
		
		return features;
	}
	
	/**
	 * Draw features to an image.
	 * @param Irgb the image to draw to
	 * @param features the features to draw
	 * @param rgb the color of the features
	 */
	public static void drawFeatures(int[][] Irgb, Vector<Feature> features, int rgb)
	{
		Ellipse ellipse;
		
		for(int i=0; i<features.size(); i++){
			ellipse = new Ellipse(features.get(i).location[0], features.get(i).location[1], features.get(i).scale, features.get(i).scale, features.get(i).orientation);
			ImageUtility.drawEllipse(Irgb, ellipse, rgb);
		}
	}
	
	/**
	 * Obtain a transformation aligning the second image with the first image.
	 * @param Irgb_target
	 * @param Irgb
	 */
	public static void alignImage(int[][] Irgb_target, int[][] Irgb)
	{
		Vector<Feature> features_target = getSIFTFeatures(Irgb_target);
		Vector<Feature> features = getSIFTFeatures(Irgb);
		
		drawFeatures(Irgb_target, features_target, 0x00ffff00);
		ImageViewer.show(Irgb_target, "");
		
		drawFeatures(Irgb, features, 0x00ffff00);
		ImageViewer.show(Irgb, "").setLocation(600, 0);
	}
	
	/**
	 * A main for debug purposes.
	 * @param args the command line arguments
	 */
	public static void main(String args[])
	{
		if(false){		//Test extraction of SIFT features
			int[][] Irgb = ImageUtility.load("C:/Kenton/Data/Images/scar1.jpg");
			Vector<Feature> features = getSIFTFeatures(Irgb);
			
			drawFeatures(Irgb, features, 0x00ffff00);
			ImageViewer.show(Irgb, "");
		}
		
		if(true){		//Test alignment of two images
			int[][] Irgb_target = ImageUtility.load("C:/Kenton/Data/Images/Temp/ProjectedInterface0/target_desktop.jpg");
			int[][] Irgb = ImageUtility.load("C:/Kenton/Data/Images/Temp/ProjectedInterface0/target_camera.jpg");

			alignImage(Irgb_target, Irgb);
		}
	}
}
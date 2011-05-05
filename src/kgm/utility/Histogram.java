package kgm.utility;
import java.io.FileWriter;
import java.util.*;

/**
 * A histogram.
 * @param <T> the bin type
 * @author Kenton McHenry
 */
public class Histogram<T extends Comparable>
{
	private TreeMap<T,Integer> histogram = new TreeMap<T,Integer>();
	private Vector<T> bins;
	private Vector<T> values = new Vector<T>();
	private TreeMap<T,Vector<T>> bin_values = new TreeMap<T,Vector<T>>();
	
	/**
	 * Class constructor.
	 * @param bins the bins to use
	 */
	public Histogram(Vector<T> bins)
	{		
		Collections.sort(bins);
		this.bins = bins;
		
		for(int i=0; i<bins.size(); i++){
			histogram.put(bins.get(i), 0);
			bin_values.put(bins.get(i), new Vector<T>());
		}
	}
	
	/**
	 * Add a value to the histogram.
	 * @param value the value to add
	 */
	public void add(T value)
	{
		values.add(value);
		
		if(value.compareTo(bins.firstElement()) <= 0){
			histogram.put(bins.firstElement(), histogram.get(bins.firstElement()) + 1);
			bin_values.get(bins.firstElement()).add(value);
		}else if(value.compareTo(bins.lastElement()) >= 0){
			histogram.put(bins.lastElement(), histogram.get(bins.lastElement()) + 1);
			bin_values.get(bins.lastElement()).add(value);
		}else{
			for(int i=1; i<bins.size()-1; i++){
				if(value.compareTo(bins.get(i-1)) > 0 && value.compareTo(bins.get(i+1)) < 0){
					histogram.put(bins.get(i), histogram.get(bins.get(i)) + 1);
					bin_values.get(bins.get(i)).add(value);
				}
			}
		}
	}
	
	/**
	 * Get the value of the largest bin.
	 * @return the value
	 */
	public T getMax()
	{
		T value = null;
		int maxc = -1;
		int tmpi;
		
		for(int i=0; i<bins.size(); i++){
			tmpi = histogram.get(bins.get(i));
			
			if(tmpi > maxc){
				maxc = tmpi;
				value = bins.get(i);
			}
		}
		
		return value;
	}

	/**
	 * Get the bin for the given value.
	 * @param value the bin to get
	 * @return the bin count
	 */
	public int get(T value)
	{
		if(value.compareTo(bins.firstElement()) <= 0){
			return histogram.get(bins.firstElement());
		}else if(value.compareTo(bins.lastElement()) >= 0){
			return histogram.get(bins.lastElement());
		}else{
			for(int i=1; i<bins.size()-1; i++){
				if(value.compareTo(bins.get(i-1)) > 0 && value.compareTo(bins.get(i+1)) < 0){
					return histogram.get(bins.get(i));
				}
			}
		}
		
		return 0;
	}
	
	/**
	 * Get the values contained in the bin for the given value.
	 * @param value the value to retrieve bin values for
	 * @return the retrieved bin's values
	 */
	public Vector<T> getValues(T value)
	{
		if(value.compareTo(bins.firstElement()) <= 0){
			return bin_values.get(bins.firstElement());
		}else if(value.compareTo(bins.lastElement()) >= 0){
			return bin_values.get(bins.lastElement());
		}else{
			for(int i=1; i<bins.size()-1; i++){
				if(value.compareTo(bins.get(i-1)) > 0 && value.compareTo(bins.get(i+1)) < 0){
					return bin_values.get(bins.get(i));
				}
			}
		}
		
		return null;
	}

	/**
	 * Get an image of the histogram.
	 * @param forground_color the foreground color
	 * @param background_color the background color
	 * @return a color image of the histogram
	 */
	public int[][] getImage(int foreground_color, int background_color)
	{
		int w = bins.size();
		int h = histogram.get(getMax());
		int[][] image = new int[h][w];
		int tmpi;
		
		for(int x=0; x<bins.size(); x++){
			tmpi = h-histogram.get(bins.get(x));
			
			for(int y=h-1; y>=0; y--){
				if(y > tmpi){
					image[y][x] = foreground_color;
				}else{
					image[y][x] = background_color;
				}
			}
		}
		
		return image;
	}
	
	/**
	 * Get an image of the histogram.
	 * @return a color image of the histogram
	 */
	public int[][] getImage()
	{
		return getImage(0x000000ff, 0x00ffffff);
	}
	
	/**
	 * Save the histogram to the given filename.
	 * @param filename the filename to save to
	 */
	public void save(String filename)
	{	    
		try{
	    FileWriter outs = new FileWriter(filename);
	
	    for(int i=0; i<bins.size(); i++){
	      outs.write(bins.get(i) + " " + histogram.get(bins.get(i)) + "\n");
	    }
	    
	    outs.close();
	  }catch(Exception e) {e.printStackTrace();}
	}

	/**
	 * Create bins for double values.
	 * @param min the minimum value
	 * @param increment the increment between values
	 * @param max the maximum value
	 * @return the created bins
	 */
	public static Vector<Double> createDoubleBins(double min, double increment, double max)
	{
		Vector<Double> bins = new Vector<Double>();
		
		for(double b=min; b<=max; b+=increment){
			bins.add(b);
		}
		
		return bins;
	}
	
	/**
	 * Create bins for integer values.
	 * @param min the minimum value
	 * @param increment the increment between values
	 * @param max the maximum value
	 * @return the created bins
	 */
	public static Vector<Integer> createIntegerBins(int min, int increment, int max)
	{
		Vector<Integer> bins = new Vector<Integer>();
		
		for(int b=min; b<=max; b+=increment){
			bins.add(b);
		}
		
		return bins;
	}
	
	/**
	 * Calculate the mean value of a vector of values.
	 * @param values the vector of values
	 * @return the mean
	 */
	public static double mean(Vector<Double> values)
	{
		double sum = 0;
		
		for(int i=0; i<values.size(); i++){
			sum += values.get(i);
		}
		
		return sum / values.size();
	}
}
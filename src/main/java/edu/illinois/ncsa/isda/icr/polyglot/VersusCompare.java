package edu.illinois.ncsa.isda.icr.polyglot;
import edu.illinois.ncsa.versus.adapter.*;
import edu.illinois.ncsa.versus.adapter.impl.*;
import edu.illinois.ncsa.versus.descriptor.*;
import edu.illinois.ncsa.versus.extract.*;
import edu.illinois.ncsa.versus.extract.impl.*;
import edu.illinois.ncsa.versus.measure.*;
import edu.illinois.ncsa.versus.measure.impl.*;
import java.io.*;

/**
 * A small program to compare two files using the Versus library.
 * @author Kenton McHenry
 */
public class VersusCompare
{
	/**
	 * Get the data type of the given file based on its extension.
	 * @param filename a file name
	 * @return the corresponding data type
	 */
	public static String getType(String filename)
	{
		String type = "unknown";
		String ext;
		int tmpi;
		
		tmpi = filename.lastIndexOf('.');
		
		if(tmpi >= 0){
			ext = filename.substring(tmpi+1);
			
			if(ext.equals("jpg")){
				type = "image";
			}else if(ext.equals("pdf")){
				type = "document";
			}else if(ext.equals("obj") || ext.equals("stp")){
				type = "model";
			}
		}
		
		return type;
	}
	
	/**
	 * Compare two files.
	 * @param filename1 the name of the first file
	 * @param filename2 the name of the second file
	 * @param adapter_name the name of the desired adapter (can be null)
	 * @param extractor_name the name of the desired extractor (can be null)
	 * @param measure_name the name of the desired measure (can be null)
	 * @return the difference between the two files according to the chosen extractor
	 */
	public static Double compare(String filename1, String filename2, String adapter_name, String extractor_name, String measure_name)
	{
		String type1, type2;
		Adapter data1 = null, data2 = null;
		Extractor extractor = null;
		Descriptor signature1, signature2;
		Measure measure;
		Double result = null;
		
		type1 = getType(filename1);
		type2 = getType(filename2);
		
		if(type1.equals(type2)){
			try{
				//Set adapter
				if(adapter_name != null){
					adapter_name = "edu.illinois.ncsa.versus.adapter.impl." + adapter_name;
					data1 = (Adapter)Class.forName(adapter_name).newInstance();
					data2 = (Adapter)Class.forName(adapter_name).newInstance();
				}else{
					if(type1.equals("image")){
						data1 = new BufferedImageAdapter();
						data2 = new BufferedImageAdapter();
					}else if(type1.equals("model")){
						data1 = new MeshAdapter();
						data2 = new MeshAdapter();
					}
				}
				
				((FileLoader)data1).load(new File(filename1));			
				((FileLoader)data2).load(new File(filename2));
			
				//Set extractor
				if(extractor_name != null){
					extractor_name = "edu.illinois.ncsa.versus.extract.impl." + extractor_name;
					extractor = (Extractor)Class.forName(extractor_name).newInstance();
				}else{
					if(type1.equals("image")){
						extractor = new ArrayFeatureExtractor();
					}else if(type1.equals("model")){
						extractor = new StatisticsExtractor();
						//extractor = new SurfaceAreaExtractor();
						//extractor = new LightFieldExtractor();
					}
				}
				
				signature1 = extractor.extract(data1);
				signature2 = extractor.extract(data2);
				
				//Set measure
				if(measure_name != null){
					measure_name = "edu.illinois.ncsa.versus.measure.impl." + measure_name;
					measure = (Measure)Class.forName(measure_name).newInstance();
				}else{
					measure = new EuclideanDistanceMeasure();
				}
				
				result = measure.compare(signature1, signature2).getValue();
			}catch(Exception e) {e.printStackTrace();}
		}			
		
		return result;
	}
	
	/**
	 * Compare two files.
	 * @param filename1 the name of the first file
	 * @param filename2 the name of the second file
	 * @return the difference between the two files according to the chosen extractor
	 */
	public static double compare(String filename1, String filename2)
	{
		return compare(filename1, filename2, null, null, null);
	}
	
	/**
	 * Compare two files.
	 * @param args the names of the two files to compare
	 */
	public static void main(String[] args)
	{
		String filename1 = "";
		String filename2 = "";
		
		if(args.length == 2){
			filename1 = args[0];
			filename2 = args[1];
		}else{	//Debug values
			if(false){
				filename1 = "data/test_1.jpg";
				filename2 = "data/test_2.jpg";
			}else{
				filename1 = "data/dc10_wrl.obj";
				filename2 = "data/dc10.obj";
			}
		}
		
		System.out.println(compare(filename1, filename2));
	}
}
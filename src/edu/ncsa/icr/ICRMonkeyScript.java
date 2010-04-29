package edu.ncsa.icr;
import edu.ncsa.image.*;
import edu.ncsa.utility.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;

/**
 * A class representing an ICR monkey script.
 * @author Kenton McHenry
 */
public class ICRMonkeyScript
{	
	private String alias;
	private String operation;
	private Vector<String> lines = new Vector<String>();
	private Vector<int[][]> images = new Vector<int[][]>();
	private Vector<Vector<int[]>> positives = new Vector<Vector<int[]>>();
	private Vector<Vector<int[]>> negatives = new Vector<Vector<int[]>>();
	
	/**
	 * Class constructor.
	 */
	public ICRMonkeyScript()
	{
		lines.add("#MonkeyScript");
		
		//Allocate space for the next captured image
		positives.add(new Vector<int[]>());
		negatives.add(new Vector<int[]>());
	}
	
	/**
	 * Class constructor.
	 * @param filename the filename of the script to load
	 */
	public ICRMonkeyScript(String filename)
	{
		load(filename);
		
		//Allocate space for the next captured image
		positives.add(new Vector<int[]>());
		negatives.add(new Vector<int[]>());
	}
	
	/**
	 * Set the script alias.
	 * @param alias the script alias
	 */
	public void setAlias(String alias)
	{
		this.alias = alias;
	}
	
	/**
	 * Set the operation the script will perform.
	 * @param operation the script operation
	 */
	public void setOperation(String operation)
	{
		this.operation = operation;
	}
	
	/**
	 * Get the script alias.
	 * @return the script alias
	 */
	public String getAlias()
	{
		return alias;
	}
	
	/**
	 * Get the operation the script performs.
	 * @return the script operation
	 */
	public String getOperation()
	{
		return operation;
	}	
	
	/**
	 * Get the name of the script.
	 * @return the script name
	 */
	public String getName()
	{
		return alias + "_" + operation;
	}
	
	/**
	 * Get the number of lines in the script.
	 * @return the number of lines in the script
	 */
	public int length()
	{
		return lines.size();
	}

	/**
	 * Add a line to the script.
	 * @param line the line to add
	 */
	public void add(String line)
	{
		lines.add(line);
	}
	
	/**
	 * Add a character to the last line of the script.
	 * @param c the character to add
	 */
	public void addToLastLine(char c)
	{
		String line = lines.lastElement();
		
		if(c == '\n'){
			line += "\\n";
		}else{
			line += c;
		}
		
		lines.set(lines.size()-1, line);
	}

	/**
	 * Add an image to the script.
	 * @param image the image to add
	 */
	public void add(BufferedImage image)
	{		
		lines.add("Image:" + Utility.toString(images.size(), 3));
		images.add(ImageUtility.image2argb(image));

		//Allocate space for the next captured image
		positives.add(new Vector<int[]>());
		negatives.add(new Vector<int[]>());
	}
	
	/**
	 * Add a positive area for the NEXT captured desktop image!
	 * @param box the bounding box of the desired area as (minx, miny, maxx, maxy)
	 */
	public void addPositiveArea(int[] box)
	{
		positives.lastElement().add(box);
	}
	
	/**
	 * Add a negative area for the NEXT captured desktop image!
	 * @param box the bounding box of the desired area as (minx, miny, maxx, maxy)
	 */
	public void addNegativeArea(int[] box)
	{
		negatives.lastElement().add(box);
	}
	
	/**
	 * Add a mouse click.
	 * @param image the desktop before the click
	 * @param x the x-coordinate
	 * @param y the y-coordinate
	 */
	public void addClick(BufferedImage image, int x, int y)
	{
		add(image);
		add("Click:" + x + "," + y);
	}

	/**
	 * Upgrade the last single click to a double click.
	 * Note: Last action should be a "Click" if this was a true double click
	 */
	public void lastClickToDoubleClick()
	{
		String line = lastLine();
		
		if(line.startsWith("Click:")){
			set(lines.size()-1, "Double" + line);
		}
	}

	/**
	 * Add characters as input from the keyboard.
	 * @param c the character to add
	 */
	public void addKey(char c)
	{
		if(lastLine().startsWith("Type:")){		//Compact typed events
			addToLastLine(c);
		}else{
			add("Type:" + c);
		}
	}

	/**
	 * Set a line of the script.
	 * @param index the index of the line to set
	 * @param line the new line
	 */
	public void set(int index, String line)
	{
		lines.set(index, line);
	}

	/**
	 * Get the last line of the script.
	 * @return the last line of the script
	 */
	public String lastLine()
	{
		return lines.lastElement();
	}

	/**
	 * Get the current set of positive areas.
	 * @return the set of positive areas for the next captured image
	 */
	public Vector<int[]> getPositiveAreas()
	{
		return positives.lastElement();
	}
	
	/**
	 * Get the current set of negative areas.
	 * @return the set of negative areas for the next captured image
	 */
	public Vector<int[]> getNegativeAreas()
	{
		return negatives.lastElement();
	}
	
	/**
	 * Save the script to the given path.
	 * @param path the path to save the script to
	 */
	public void save(String path)
	{		
		String imagename;
		String buffer;
		int[] box;
		
		//Save images
		new File(path + getName()).mkdir();
		
		for(int i=0; i<images.size(); i++){
			imagename = path + getName() + "/" + Utility.toString(i,3);
			ImageUtility.save(imagename + ".jpg", images.get(i));
		
			//Save selection areas
			buffer = "";
			
			for(int j=0; j<positives.get(i).size(); j++){
				box = positives.get(i).get(j);
				buffer += "Positive:" + box[0] + "," + box[1] + "," + box[2] + "," + box[3] + "\n";
			}
			
			for(int j=0; j<negatives.get(i).size(); j++){
				box = negatives.get(i).get(j);
				buffer += "Negative:" + box[0] + "," + box[1] + "," + box[2] + "," + box[3] + "\n";
			}
			
			Utility.save(imagename + ".txt", buffer);
		}		
		
		//Save script
		Utility.save(path + getName() + ".ms", Utility.collapse(lines));
	}
	
	/**
	 * Load a script.
	 * @param filename the filename of the script
	 */
	public void load(String filename)
	{
		Vector<String> lines;
		String path, name;
		String imagename;
		String line, key, value;
		Vector<String> values;
		int tmpi;
		
		path = Utility.getFilenamePath(filename);
		name = Utility.getFilenameName(filename);	
		tmpi = name.indexOf('_');
		
		if(tmpi > 0){
			alias = name.substring(0, tmpi);
			operation = name.substring(tmpi+1);
		}
		
		//Load images (in correct order!)
		images.clear();
		positives.clear();
		negatives.clear();
		
		while(true){
			imagename = path + name + "/" + Utility.toString(images.size(), 3);
			
			if(Utility.exists(imagename + ".jpg")){
				images.add(ImageUtility.load(imagename + ".jpg"));
				
				//Load selection areas
				positives.add(new Vector<int[]>());
				negatives.add(new Vector<int[]>());
				
				lines = Utility.loadToStrings(imagename + ".txt");
				
				for(int i=0; i<lines.size(); i++){
					line = lines.get(i);
					tmpi = line.indexOf(':');
					key = line.substring(0, tmpi);
					value = line.substring(tmpi+1);
					
					if(key.equals("Positive")){
						values = Utility.split(value, ',', false);
						positives.lastElement().add(new int[]{Integer.valueOf(values.get(0)), Integer.valueOf(values.get(1)), Integer.valueOf(values.get(2)), Integer.valueOf(values.get(3))});
					}else if(key.equals("Negative")){
						values = Utility.split(value, ',', false);
						negatives.lastElement().add(new int[]{Integer.valueOf(values.get(0)), Integer.valueOf(values.get(1)), Integer.valueOf(values.get(2)), Integer.valueOf(values.get(3))});
					}
				}
			}else{
				break;
			}
		}
		
		//Load script
		this.lines = Utility.loadToStrings(filename);
	}
	
	/**
	 * Display this script.
	 */
	public void print()
	{
		int[] box;
		
		//Display image information
		for(int i=0; i<images.size(); i++){
			System.out.println("Image:" + Utility.toString(i, 3));
			
			for(int j=0; j<positives.get(i).size(); j++){
				box = positives.get(i).get(j);
				System.out.println("Positive:" + box[0] + "," + box[1] + "," + box[2] + "," + box[3]);
			}
			
			for(int j=0; j<negatives.get(i).size(); j++){
				box = negatives.get(i).get(j);
				System.out.println("Negative:" + box[0] + "," + box[1] + "," + box[2] + "," + box[3]);
			}
			
			System.out.println();
		}
		
		//Display the script
		for(int i=0; i<lines.size(); i++){
			System.out.println(lines.get(i));
		}
	}
	
	/**
	 * Execute the script.
	 * @param threshold the image threshold to use when comparing two images
	 */
	public void execute(double threshold)
	{
		Robot robot = null;
		String line, key, value;
		Vector<String> values;
		String text;
		int[][] desktop;
		double[] image_g = null;
		double[] desktop_g;
		double ssd;
		int image_index = 0;
		int image_width = 0;
		int image_height = 0;
		int desktop_width, desktop_height;
		int x, y, tmpi;
		
		try{
			robot = new Robot();
		}catch(Exception e) {e.printStackTrace();}

		//Go through each line of the script
		for(int i=0; i<lines.size(); i++){
			line = lines.get(i);
			
			if(line.charAt(0) != '#'){
				tmpi = line.indexOf(':');
				key = line.substring(0, tmpi);
				value = line.substring(tmpi+1);
				
				if(key.equals("Image")){
					image_index = Integer.valueOf(value);
					image_g = ImageUtility.argb2g(images.get(image_index));
					image_height = images.get(image_index).length;
					image_width = images.get(image_index)[0].length;
					
					//Apply masks to image
					if(!positives.get(image_index).isEmpty()){
						image_g = ImageUtility.applyPositiveMasks(image_g, image_width, image_height, positives.get(image_index));
					}else if(!negatives.get(image_index).isEmpty()){
						image_g = ImageUtility.applyNegativeMasks(image_g, image_width, image_height, negatives.get(image_index));
					}
					
					//ImageViewer.show(image_g, image_width, image_height);
					
					//Examine the desktop
					while(true){
						desktop = ImageUtility.getScreen();
						desktop_g = ImageUtility.argb2g(desktop);
						desktop_height = desktop.length;
						desktop_width = desktop[0].length;
						
						//Check image sizes
						if(desktop_width != image_width || desktop_height != image_height){
							System.out.println("\nScreen size is different from images in script!");
							System.exit(1);
						}	
						
						//Apply masks to desktop image
						if(!positives.get(image_index).isEmpty()){
							desktop_g = ImageUtility.applyPositiveMasks(desktop_g, desktop_width, desktop_height, positives.get(image_index));
						}else if(!negatives.get(image_index).isEmpty()){
							desktop_g = ImageUtility.applyNegativeMasks(desktop_g, desktop_width, desktop_height, negatives.get(image_index));
						}
						
						//Compare desktop to image
						ssd = ImageUtility.ssd(desktop_g, image_g);
						
						if(ssd > threshold){
							break;
						}else{
							Utility.pause(500);
						}
					}
				}else if(key.equals("Click") || key.equals("DoubleClick")){
					values = Utility.split(value, ',', false);
					x = Integer.valueOf(values.get(0));
					y = Integer.valueOf(values.get(1));
					
					robot.mouseMove(x, y);
					
					/*
					if(key.equals("Click")){
						robot.mousePress(InputEvent.BUTTON1_MASK);
						robot.mouseRelease(InputEvent.BUTTON1_MASK);
					}else if(key.equals("DoubleClick")){
						robot.mousePress(InputEvent.BUTTON1_MASK);
						robot.mouseRelease(InputEvent.BUTTON1_MASK);
						robot.mousePress(InputEvent.BUTTON1_MASK);
						robot.mouseRelease(InputEvent.BUTTON1_MASK);
					}
					*/
				}else if(key.equals("Type")){
					text = value;
				}
			}
		}
	}
	
	/**
	 * A main to execute an ICR monkey script from the command line.
	 * @param args command line arguments
	 */
	public static void main(String args[])
	{
		if(args.length > 1){
			ICRMonkeyScript script = new ICRMonkeyScript(args[1]);
			System.out.println(); script.print();
			script.execute(Double.valueOf(args[0]));
		}else{
			System.out.println("Usage: ICRMonkeyScript threshold script.ms");
		}
	}
}
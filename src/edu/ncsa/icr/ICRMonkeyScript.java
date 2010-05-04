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
	private Vector<int[][]> desktops = new Vector<int[][]>();
	private Vector<int[][]> targets = new Vector<int[][]>();
	private Vector<Vector<int[]>> positives = new Vector<Vector<int[]>>();
	private Vector<Vector<int[]>> negatives = new Vector<Vector<int[]>>();
	private boolean VERBOSE = true;
	
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
	public void addLine(String line)
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
	 * Add a desktop image to the script.
	 * @param image the image to add
	 */
	public void addDesktop(BufferedImage image)
	{		
		lines.add("Desktop:" + Utility.toString(desktops.size(), 3));
		desktops.add(ImageUtility.image2argb(image));

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
	 * Add a target image to the script.
	 * @param image the image to add
	 */
	public void addTarget(int[][] image)
	{
		lines.add("Target:" + Utility.toString(targets.size(), 3));
		targets.add(image);
	}

	/**
	 * Add a mouse click.
	 * @param image the desktop before the click
	 * @param x the x-coordinate
	 * @param y the y-coordinate
	 */
	public void addClick(BufferedImage image, int x, int y)
	{
		addDesktop(image);
		addLine("Click:" + x + "," + y);
	}

	/**
	 * Add a mouse double click.
	 * @param image the desktop before the click
	 * @param x the x-coordinate
	 * @param y the y-coordinate
	 */
	public void addDoubleClick(BufferedImage image, int x, int y)
	{
		addDesktop(image);
		addLine("DoubleClick:" + x + "," + y);
	}
	
	/**
	 * Upgrade the last single click to a double click.
	 * Note: Last action should be a "Click" if this was a double click event in Java
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
			addLine("Type:" + c);
		}
	}
	
	/**
	 * Add text input from the keyboard.
	 * @param text the text to add
	 */
	public void addText(String text)
	{
		addLine("Type:" + text);
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
		
		new File(path + getName()).mkdir();
				
		//Save desktop images
		for(int i=0; i<desktops.size(); i++){
			imagename = path + getName() + "/desktop_" + Utility.toString(i,3);
			ImageUtility.save(imagename + ".png", desktops.get(i));
		
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
		
		//Save target images		
		for(int i=0; i<targets.size(); i++){
			imagename = path + getName() + "/target_" + Utility.toString(i,3);
			ImageUtility.save(imagename + ".png", targets.get(i));
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
		
		//Load desktop images (in correct order!)
		desktops.clear();
		positives.clear();
		negatives.clear();
		
		while(true){
			imagename = path + name + "/desktop_" + Utility.toString(desktops.size(), 3);
			
			if(Utility.exists(imagename + ".png")){
				desktops.add(ImageUtility.load(imagename + ".png"));
				
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
		
		//Load target images (in correct order!)
		targets.clear();
		
		while(true){
			imagename = path + name + "/target_" + Utility.toString(targets.size(), 3);
			
			if(Utility.exists(imagename + ".png")){
				targets.add(ImageUtility.load(imagename + ".png"));
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
		System.out.println("******** Desktops ********");

		for(int i=0; i<desktops.size(); i++){
			if(i > 0) System.out.println();
			System.out.println("Desktop:" + Utility.toString(i, 3));
			
			for(int j=0; j<positives.get(i).size(); j++){
				box = positives.get(i).get(j);
				System.out.println("Positive:" + box[0] + "," + box[1] + "," + box[2] + "," + box[3]);
			}
			
			for(int j=0; j<negatives.get(i).size(); j++){
				box = negatives.get(i).get(j);
				System.out.println("Negative:" + box[0] + "," + box[1] + "," + box[2] + "," + box[3]);
			}
		}
		
		System.out.println("*************************");
		System.out.println();
		
		//Display target information
		System.out.println("******** Targets ********");

		for(int i=0; i<targets.size(); i++){
			System.out.println("Target:" + Utility.toString(i, 3));
		}
		
		System.out.println("*************************");
		System.out.println();
		
		//Display the script
		System.out.println("******** Script *********");

		for(int i=0; i<lines.size(); i++){
			System.out.println(lines.get(i));
		}
		
		System.out.println("*************************");

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
		int[][] current_desktop;
		int[] required_desktop_1d;
		int[] current_desktop_1d;
		double[] desktop_difference = null;
		int differences;		
		int image_index = 0;
		int image_width = 0;
		int image_height = 0;
		int desktop_width, desktop_height;
		int x, y, tmpi;
				
		ImageViewer viewer1 = null;
		ImageViewer viewer2 = null;
		Dimension screen_size = Toolkit.getDefaultToolkit().getScreenSize();
		int screens = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices().length;
		
		if(screens > 1){
			viewer1 = new ImageViewer();
	    viewer1.setLocation((int)screen_size.getWidth()+5, 5);
	    
	    viewer2 = new ImageViewer();
	    viewer2.setLocation((int)screen_size.getWidth()+viewer1.getWidth()+5+10, 5);
		}	
		
		try{
			robot = new Robot();
		}catch(Exception e) {e.printStackTrace();}
		
		if(VERBOSE){
			System.out.println(); 
			print(); 
			System.out.println();
		}
		
		//Go through each line of the script
		if(VERBOSE) System.out.println("Script executiong started...");
		
		for(int i=0; i<lines.size(); i++){
			line = lines.get(i);
			
			if(line.charAt(0) != '#'){
				tmpi = line.indexOf(':');
				key = line.substring(0, tmpi);
				value = line.substring(tmpi+1);
				
				if(key.equals("Desktop")){
					image_index = Integer.valueOf(value);
					required_desktop_1d = ImageUtility.to1D(desktops.get(image_index));
					image_height = desktops.get(image_index).length;
					image_width = desktops.get(image_index)[0].length;
					
					//Apply masks to image
					if(!positives.get(image_index).isEmpty()){
						required_desktop_1d = ImageUtility.applyPositiveMasks(required_desktop_1d, image_width, image_height, positives.get(image_index));
					}
					
					if(!negatives.get(image_index).isEmpty()){
						required_desktop_1d = ImageUtility.applyNegativeMasks(required_desktop_1d, image_width, image_height, negatives.get(image_index));
					}
										
					if(viewer1 != null) viewer1.set(required_desktop_1d, image_width, image_height);
					if(VERBOSE) System.out.println("Watching for desktop to match image: " + Utility.toString(image_index, 3) + "...");
					
					//Examine the desktop
					while(true){
						current_desktop = ImageUtility.getScreen();
						current_desktop_1d = ImageUtility.to1D(current_desktop);
						desktop_height = current_desktop.length;
						desktop_width = current_desktop[0].length;
						
						//Check image sizes
						if(desktop_width != image_width || desktop_height != image_height){
							System.out.println("\nScreen size is different from images in script!");
							System.exit(1);
						}	
						
						//Apply masks to desktop image
						if(!positives.get(image_index).isEmpty()){
							current_desktop_1d = ImageUtility.applyPositiveMasks(current_desktop_1d, desktop_width, desktop_height, positives.get(image_index));
						}
						
						if(!negatives.get(image_index).isEmpty()){
							current_desktop_1d = ImageUtility.applyNegativeMasks(current_desktop_1d, desktop_width, desktop_height, negatives.get(image_index));
						}
						
						//Check for image differences
						if(desktop_difference == null) desktop_difference = new double[current_desktop_1d.length];
						differences = ImageUtility.difference(desktop_difference, current_desktop_1d, required_desktop_1d, 0, false);
						
						if(viewer2 != null) viewer2.set(desktop_difference, desktop_width, desktop_height);
						if(VERBOSE) System.out.println("Difference: " + differences);
						
						if(differences < threshold){
							if(VERBOSE) System.out.println("Desktop matches image: " + Utility.toString(image_index, 3) + "!");
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

					if(key.equals("Click")){
						System.out.println("Clicking at " + x + "," + y);
						robot.mousePress(InputEvent.BUTTON1_MASK);
						robot.mouseRelease(InputEvent.BUTTON1_MASK);
					}else if(key.equals("DoubleClick")){
						System.out.println("Double clicking at " + x + "," + y);
						robot.mousePress(InputEvent.BUTTON1_MASK);
						robot.mouseRelease(InputEvent.BUTTON1_MASK);
						robot.mousePress(InputEvent.BUTTON1_MASK);
						robot.mouseRelease(InputEvent.BUTTON1_MASK);
					}
				}else if(key.equals("Type")){
					text = value;
				}
			}
		}
		
		if(VERBOSE) System.out.println("Script execution finished.");
	}
	
	/**
	 * A main to execute an ICR monkey script from the command line.
	 * @param args command line arguments
	 */
	public static void main(String args[])
	{
		args = new String[]{"5", "C:/Kenton/Data/Temp/ICRMonkey/001_open.ms"};
		
		if(args.length > 1){
			ICRMonkeyScript script = new ICRMonkeyScript(args[1]);
			script.execute(Double.valueOf(args[0]));
		}else{
			System.out.println("Usage: ICRMonkeyScript threshold script.ms");
		}
	}
}
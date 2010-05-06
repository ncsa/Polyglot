package edu.ncsa.icr;
import edu.ncsa.image.*;
import edu.ncsa.utility.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.swing.*;
import java.io.*;
import java.util.*;

/**
 * A class representing an ICR monkey script.
 * @author Kenton McHenry
 */
public class ICRMonkeyScript
{	
	private Robot robot = null;
	private String alias;
	private String operation;
	private Vector<String> lines = new Vector<String>();
	private Vector<int[][]> desktops = new Vector<int[][]>();
	private Vector<int[][]> targets = new Vector<int[][]>();
	private Vector<Vector<int[]>> positives = new Vector<Vector<int[]>>();
	private Vector<Vector<int[]>> negatives = new Vector<Vector<int[]>>();
	
	private int pixel_threshold = 0;
	private int image_threshold = 0;
	private JFrame focus_thief = null;
	private int focus_thief_delay = 5000;
	private int max_focus_thief_attempts = 2;
	private int max_watch_time = 30000;
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
		
		try{
			robot = new Robot();
		}catch(Exception e) {e.printStackTrace();}
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
		
		try{
			robot = new Robot();
		}catch(Exception e) {e.printStackTrace();}
	}
	
	/**
	 * Start up an iconified JFrame for the sole purpose of stealing screen focus when needed.
	 * @param focus_thief_delay the time in milliseconds to wait before trying to use the focus thief when executing a stalled script
	 */
	public void startFocusThief(int focus_thief_delay)
	{
		focus_thief = new JFrame();
		focus_thief.setState(JFrame.ICONIFIED);
		focus_thief.setVisible(true);
		this.focus_thief_delay = focus_thief_delay;
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
	 * Add a target mouse click.
	 * @param image the target with which the click is in reference to
	 * @param x0 the x-coordinate of the target location
	 * @param y0 the y-coordinate of the target locaiton
	 * @param x the x-coordinate of the click on the desktop
	 * @param y the y-coordinate of the click on the desktop
	 */
	public void addTargetClick(int[][] image, int x0, int y0, int x, int y)
	{
		addTarget(image);
		addLine("Click:" + (x-x0) + "," + (y-y0));
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
	 * Add text input from the keyboard (note this versions enforces a required desktop!).
	 * @param image the desktop before the click
	 * @param text the text to add
	 */
	public void addText(BufferedImage image, String text)
	{
		addDesktop(image);
		addText(text);
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
		
		System.out.println();
		
		//Display target information
		System.out.println("******** Targets ********");

		for(int i=0; i<targets.size(); i++){
			System.out.println("Target:" + Utility.toString(i, 3));
		}
		
		System.out.println();
		
		//Display the script
		System.out.println("******** Script *********");

		for(int i=0; i<lines.size(); i++){
			System.out.println(lines.get(i));
		}
		
		System.out.println();

	}
	
	/**
	 * Steal window focus as best as possible.
	 * @param desktop_index the index of the current required desktop image
	 * @return true if an attempt was made
	 */
	public boolean stealFocus(int desktop_index)
	{
		int[] box;
		
		if(focus_thief != null){
			if(VERBOSE) System.out.println("Trying focus thief...");
			focus_thief.toFront();
			
			//Try to also move mouse to the middle of a negative mask area
			if(desktop_index >= 0 && !negatives.get(desktop_index).isEmpty()){
				box = negatives.get(desktop_index).firstElement();
				robot.mouseMove((box[0]+box[2])/2, (box[1]+box[3])/2);
			}
			
			return true;
		}
		
		return false;
	}
	
	/**
	 * Execute the script.
	 */
	public void execute()
	{
		String line, key, value;
		Vector<String> values;
		String text;
		int[][] required_image;
		int[][] current_desktop;
		int[] requied_image_1d;
		int[] current_desktop_1d;
		double[] desktop_difference = null;
		Vector<Point> target_locations;
		Point target = null;
		long t0, t1, dt;
		int image_index = 0;
		int image_width = 0;
		int image_height = 0;
		int desktop_width, desktop_height;
		int differences;		
		int focus_thief_attempts;
		int key_code;
		int x, y, tmpi;
				
		ImageViewer viewer1 = null;
		ImageViewer viewer2 = null;
		Dimension screen_size = Toolkit.getDefaultToolkit().getScreenSize();
		int screens = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices().length;
		
		if(screens > 1){
			viewer1 = new ImageViewer("Required Image");
	    viewer1.setLocation((int)screen_size.getWidth(), 0);
	    
	    viewer2 = new ImageViewer("Current Image");
	    viewer2.setLocation((int)screen_size.getWidth()+viewer1.getWidth(), 0);
		}	
		
		//Go through each line of the script
		if(VERBOSE) System.out.println("Script execution started...");
		
		for(int i=0; i<lines.size(); i++){
			line = lines.get(i);
			
			if(line.charAt(0) != '#'){
				tmpi = line.indexOf(':');
				key = line.substring(0, tmpi);
				value = line.substring(tmpi+1);
				
				if(key.equals("Desktop")){
					image_index = Integer.valueOf(value);
					requied_image_1d = ImageUtility.to1D(desktops.get(image_index));
					image_height = desktops.get(image_index).length;
					image_width = desktops.get(image_index)[0].length;
					
					//Apply masks to image
					if(!positives.get(image_index).isEmpty()){
						requied_image_1d = ImageUtility.applyPositiveMasks(requied_image_1d, image_width, image_height, positives.get(image_index));
					}
					
					if(!negatives.get(image_index).isEmpty()){
						requied_image_1d = ImageUtility.applyNegativeMasks(requied_image_1d, image_width, image_height, negatives.get(image_index));
					}
										
					if(viewer1 != null) viewer1.set(requied_image_1d, image_width, image_height);
					if(VERBOSE) System.out.println("Watching for desktop to match image: " + Utility.toString(image_index, 3) + "...");
					
					//Examine the desktop
					t0 = System.currentTimeMillis();
					focus_thief_attempts = 0;
					
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
						differences = ImageUtility.difference(desktop_difference, current_desktop_1d, requied_image_1d, pixel_threshold, false);
						
						if(viewer2 != null) viewer2.set(desktop_difference, desktop_width, desktop_height);
						if(VERBOSE) System.out.println("Difference: " + differences);
						
						if(differences <= image_threshold){
							if(VERBOSE) System.out.println("Desktop matches image: " + Utility.toString(image_index, 3) + "!");
							break;
						}else{
							Utility.pause(500);
						}
						
						//Check progress
						t1 = System.currentTimeMillis();
						dt = t1 - t0;
						
						if(dt > focus_thief_delay && focus_thief != null && focus_thief_attempts < max_focus_thief_attempts){
							stealFocus(image_index);
							focus_thief_attempts++;
						}else if(dt > max_watch_time){
							System.out.println("Maximum watch time exceeded!");
							System.exit(1);
						}
					}
				}else if(key.equals("Target")){
					image_index = Integer.valueOf(value);
					required_image = targets.get(image_index);
					image_height = desktops.get(image_index).length;
					image_width = desktops.get(image_index)[0].length;
		
					if(viewer1 != null) viewer1.set(required_image, image_width, image_height);
					if(VERBOSE) System.out.println("Watching for target: " + Utility.toString(image_index, 3) + "...");
										
					//Examine the desktop
					t0 = System.currentTimeMillis();
					focus_thief_attempts = 0;
					
					while(true){
						current_desktop = ImageUtility.getScreen();
						desktop_height = current_desktop.length;
						desktop_width = current_desktop[0].length;
						
						if(viewer2 != null) viewer2.set(current_desktop, desktop_width, desktop_height);

						target_locations = ImageUtility.find(current_desktop, required_image, pixel_threshold, true);

						if(!target_locations.isEmpty()){
							target = target_locations.get(0);
							if(VERBOSE) System.out.println("Target found: " + Utility.toString(image_index, 3) + "!");
							break;
						}else{
							Utility.pause(500);
						}
						
						//Check progress
						t1 = System.currentTimeMillis();
						dt = t1 - t0;
						
						if(dt > focus_thief_delay && focus_thief != null && focus_thief_attempts < max_focus_thief_attempts){
							stealFocus(-1);
							focus_thief_attempts++;
						}else if(dt > max_watch_time){
							System.out.println("Maximum watch time exceeded!");
							System.exit(1);
						}
					}
				}else if(key.equals("Click") || key.equals("DoubleClick")){
					values = Utility.split(value, ',', false);
					x = Integer.valueOf(values.get(0));
					y = Integer.valueOf(values.get(1));
										
					if(target != null){
						x += target.x;
						y += target.y;
						target = null;	//This is all we need from the target so clear it when finished!
					}
					
					robot.mouseMove(x, y);

					if(key.equals("Click")){
						if(VERBOSE) System.out.println("Clicking at " + x + "," + y);
						robot.mousePress(InputEvent.BUTTON1_MASK);
						robot.mouseRelease(InputEvent.BUTTON1_MASK);
					}else if(key.equals("DoubleClick")){
						if(VERBOSE) System.out.println("Double clicking at " + x + "," + y);
						robot.mousePress(InputEvent.BUTTON1_MASK);
						robot.mouseRelease(InputEvent.BUTTON1_MASK);
						robot.mousePress(InputEvent.BUTTON1_MASK);
						robot.mouseRelease(InputEvent.BUTTON1_MASK);
					}
				}else if(key.equals("Type")){
					text = value;
					if(VERBOSE) System.out.println("Typing \"" + text + "\"");
					
					for(int j=0; j<text.length(); j++){
						key_code = KeyStroke.getKeyStroke(Character.toUpperCase(text.charAt(j)), 0).getKeyCode();
												
						if(Character.isUpperCase(text.charAt(j))){
							robot.keyPress(KeyEvent.VK_SHIFT);
							robot.keyPress(key_code);
							robot.keyRelease(key_code);
							robot.keyRelease(KeyEvent.VK_SHIFT);
						}else{
							robot.keyPress(key_code);
							robot.keyRelease(key_code);
						}
					}
				}else{
					System.out.println("Line " + (i+1) + ": unrecognized command!");
					System.exit(1);
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
		String filename = null;
		int pixel_threshold = 0;
		int image_threshold = 0;
		int focus_thief_delay = -1;
		boolean VERBOSE = false;
		boolean SHOW_HELP = false;
		
		if(args.length == 0){
			args = new String[]{"-it", "15", "-ft", "2500", "-v", "C:/Kenton/Data/Temp/ICRMonkey/007_open.ms"};
			//args = new String[]{"-it", "15", "-v", "C:/Kenton/Data/Temp/ICRMonkey/002_open.ms"};
		}
		
		//Parse arguments
		for(int i=0; i<args.length; i++){
			if(args[i].equals("-pt")){
				pixel_threshold = Integer.valueOf(args[++i]);
			}else if(args[i].equals("-it")){
				image_threshold = Integer.valueOf(args[++i]);
			}else if(args[i].equals("-ft")){
				focus_thief_delay = Integer.valueOf(args[++i]);
			}else if(args[i].equals("-v")){
				VERBOSE = true;
			}else if(args[i].equals("-?")){
				SHOW_HELP = true;
			}else{
				filename = args[i];
			}
		}
		
		//Display help
		if(args.length == 0 || SHOW_HELP){
			System.out.println("Usage: ICRMonkeyScript [options] file.ms\n");
			System.out.println("  -pt x: set the threshold to use when comparing two pixels");
			System.out.println("  -it x: set the threshold to use when comparing two images");
			System.out.println("  -ft x: set how long to wait before activating a focus thief");
			System.out.println("  -v: enable verbose mode");
			System.out.println("  -?: display this help\n");
		}
		
		//Run the script
		if(filename != null){		
			ICRMonkeyScript script = new ICRMonkeyScript(filename);
			//script.print(); 
			
			script.pixel_threshold = pixel_threshold;
			script.image_threshold = image_threshold;
			if(focus_thief_delay >= 0) script.startFocusThief(focus_thief_delay);
			script.VERBOSE = VERBOSE;
			
			script.execute();
		}
		
		//Force exit (JFrames will persist otherwise!)
		System.exit(0);		
	}
}
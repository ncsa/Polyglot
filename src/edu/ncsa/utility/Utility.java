package edu.ncsa.utility;
import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.util.zip.*;
import java.security.*;

/**
 * Utility functions.
 *  @author Kenton McHenry
 */
public class Utility
{
  /**
   * Check if a file exists.
   *  @param filename the file name of the file to check
   *  @return true if the file exists
   */
  public static boolean exists(String filename)
  {
    File tmpf = new File(filename);
    
    return tmpf.exists();
  }
  
  /**
   * Check if a directory.
   * @param filename the file name of the file to check
   * @return true if a directory
   */
  public static boolean isDirectory(String filename)
  {
    File tmpf = new File(filename);
    
    return tmpf.isDirectory();
  }
  
  /**
   * Wait until a file exists.
   * @param filename the name of the file to wait for
   */
  public static void waitUntilExists(String filename)
  {
  	while(!exists(filename)){
  		pause(1000);
  	}
  }
  
  /**
	 * If the file doesn't exists create it and in either case update it's modification time.
	 *  @param filename the file to touch
	 */
	public static void touch(String filename)
	{
		if(!exists(filename)){
			try{
		    BufferedWriter outs = new  BufferedWriter(new FileWriter(filename));
		    outs.close();
			}catch(Exception e) {}
		}
		
		File file = new File(filename);
		file.setLastModified(System.currentTimeMillis());
	}

	/**
	 * Execute the given command.
	 * @param command the command
	 * @param max_runtime the maximum allowed time to run (in milli-seconds, -1 indicates forever)
	 * @param HANDLE_OUTPUT true if the process output should be handled
	 * @param SHOW_OUTPUT true if the process output should be shown
	 * @return true if the operation completed within the given time frame
	 */
	public static boolean executeAndWait(String command, int max_runtime, boolean HANDLE_OUTPUT, boolean SHOW_OUTPUT)
	{
		Process process;
		TimedProcess timed_process;
		boolean COMPLETE = false;
		
  	if(!command.isEmpty()){
	  	try{
		  	process = Runtime.getRuntime().exec(command);
		  	
		  	if(max_runtime >= 0){
				  timed_process = new TimedProcess(process, HANDLE_OUTPUT, SHOW_OUTPUT);    
				  COMPLETE = timed_process.waitFor(max_runtime); System.out.println();
		  	}else{
		  		if(HANDLE_OUTPUT){
		  			Utility.handleProcessOutput(process, SHOW_OUTPUT);
		  		}else{
		        process.waitFor();
		  		}
		  		
		  		COMPLETE = true;
		  	}
	  	}catch(Exception e) {e.printStackTrace();}
  	}
  	
  	return COMPLETE;
	}
	
	/**
	 * Execute the given command.
	 * @param command the command
	 * @param max_runtime the maximum allowed time to run (in milli-seconds, -1 indicates forever)
	 * @return true if the operation completed within the given time frame
	 */
	public static boolean executeAndWait(String command, int max_runtime)
	{
		return executeAndWait(command, max_runtime, false, false);
	}
	
	/**
   * Handle the output of a process.
   * @param process the process
   * @param SHOW_OUTPUT true if the output should be printed
   */
  public static void handleProcessOutput(Process process, boolean SHOW_OUTPUT)
  {
		DataInputStream ins = new DataInputStream(process.getInputStream());
		String line;
		
		try{
			while((line = ins.readLine()) != null){
				if(SHOW_OUTPUT) System.out.println(line);
			}
		}catch(Exception e) {e.printStackTrace();}
  }
  
  /**
	 * Get the specified line from the file indicated.
	 *  @param filename the file to load from
	 *  @param n the line number (starts at 1!)
	 *  @return the line retrieved
	 */
	public static String getLine(String filename, int n)
	{
	  String line = "";
	  
	  try{
	    BufferedReader ins = new BufferedReader(new FileReader(filename));
	    int count = 0;
	    
	    while((line=ins.readLine()) != null){
	      count++;
	      if(count == n) break;
	    }
	    
	    ins.close();
	  }catch(Exception e) {}
	  
	  return line;
	}

	/**
	 * Append a string to a file.
	 *  @param filename the file to append to
	 *  @param string the text to append to the file
	 */
	public static void print(String filename, String string)
	{
	  try{
	    BufferedWriter outs = new BufferedWriter(new FileWriter(filename, true));
	    outs.write(string);
	    outs.close();
	  }catch(Exception e) {e.printStackTrace();}
	}

	/**
	 * Append a line to a file.
	 *  @param filename the file to append to
	 *  @param string the line to append to the file
	 */
	public static void println(String filename, String string)
	{
	  try{
	    BufferedWriter outs = new BufferedWriter(new FileWriter(filename, true));
	    outs.write(string);
	    outs.newLine();
	    outs.close();
	  }catch(Exception e) {e.printStackTrace();}
	}

	/**
   * Load a file's contents into a string.
   *  @param filename the file to load
   *  @return the resulting string
   */
  public static String loadToString(String filename)
  {
    StringBuffer buffer = new StringBuffer();
      
    try{
      FileReader ins = new FileReader(filename);
      int c;
      
      while((c=ins.read()) != -1){
        buffer.append((char)c);
      }
      
      ins.close();
    }catch(Exception e){}
    
    return buffer.toString();
  }
  
  /**
   * Load a file's contents into a vector of strings containing lines of text.
   * @param filename the file to load
   * @return the resulting strings
   */
  public static Vector<String> loadToStrings(String filename)
  {
  	Vector<String> lines = new Vector<String>();
  	
  	try{
	  	Scanner scanner = new Scanner(new File(filename));
	  	
	  	while(scanner.hasNextLine()){
	  		lines.add(scanner.nextLine());
	  	}
	  	
	  	scanner.close();
  	}catch(Exception e) {e.printStackTrace();}
  	
  	return lines;
  }
  
  /**
   * Save a string to a file.
   *  @param filename the file to save to
   *  @param buffer the string to save
   */
  public static void save(String filename, String buffer)
  {
    try{
      FileWriter outs = new FileWriter(filename);
      
      for(int i=0; i<buffer.length(); i++){
        outs.write(buffer.charAt(i));
      }
      
      outs.close();
    }catch(Exception e){}
  }
  
  /**
   * Load a file into a byte array using java NIO.
   * @param filename the file name
   * @return the data as an array of bytes
   */
  public static byte[] loadToBytes(String filename)
  {
  	byte[] bytes = null;
  	
		try{
			FileChannel ins = new FileInputStream(filename).getChannel();
			ByteBuffer buffer = ByteBuffer.allocate((int)ins.size());
			buffer.rewind();
			ins.read(buffer);
			buffer.rewind();
			
			bytes = new byte[(int)ins.size()];
			buffer.get(bytes);
		}catch(Exception e) {e.printStackTrace();}
		
		return bytes;
  }
  
  /**
   * Save bytes to a file using java NIO.
   * @param filename the file name
   * @param bytes the bytes to save
   */
  public static void save(String filename, byte[] bytes)
  {
    ByteBuffer buffer = ByteBuffer.allocate(bytes.length);
    buffer.put(bytes);
    buffer.flip();
    
    try{
    	FileChannel outs = new FileOutputStream(filename).getChannel();
    	outs.write(buffer);
    	outs.close();
    }catch(Exception e) {}
  }
  
  /**
   * Copy a file from one location to another.
   * @param source the source file location
   * @param destination the destination file location
   */
  public static void copyFile(String source, String destination)
  {
    try{
      FileChannel ins = new FileInputStream(source).getChannel();
      FileChannel outs = new FileOutputStream(destination).getChannel();
      outs.transferFrom(ins, 0, ins.size());
      ins.close();      
      outs.close();
    }catch(Exception e) {e.printStackTrace();}
  }
  
  /**
   * Convert all '\' to '/' so as to be consistent with Unix/Java paths.
   *  @param input the path to convert
   *  @return the converted path
   */
  public static String unixPath(String input)
  {
    String output = "";
    
    for(int i=0; i<input.length(); i++){
      if(input.charAt(i) == '\\'){
        output += '/';
      }else{
        output += input.charAt(i);
      }
    }
    
    return output;
  }
  
  /**
   * Trim, remove quotes, convert all '\' to '/' so as to be consistent with Unix/Java paths, and add an end slash.
   * @param path the path to convert
   * @return the converted path
   */
  public static String cleanUnixPath(String path)
  {
  	path = path.trim();
		if(path.charAt(0) == '"') path = path.substring(0, path.length()-2);
		path = Utility.unixPath(path);
		if(path.charAt(path.length()-1) != '/') path += '/';
		
		return path;
  }
  
  /**
   * Convert all '/' to '\' so as to be consistent with Windows paths.
   *  @param input the path to convert
   *  @return the converted path
   */
  public static String windowsPath(String input)
  {
    String output = "";
    
    for(int i=0; i<input.length(); i++){
      if(input.charAt(i) == '/'){
        output += '\\';
      }else{
        output += input.charAt(i);
      }
    }
    
    return output;
  }
  
  /**
   * Convert '/' to '\' if a windows drive path is found at the beginning.
   * @param input the path to convert
   * @return the converted path
   */
  public static String windowsDrivePath(String input)
  {
  	if(input.charAt(1) == ':' && input.charAt(2) == '/'){
  		return input.charAt(0) + ":\\" + input.substring(3);
  	}
    
    return input;
  }
  
  /**
   * Quote portions of the path that contain spaces (unix paths only!)
   * @param input the current path
   * @return the path with quoted portions containing spaces
   */
  public static String quotedPath(String input)
  {
  	String output = "";
  	String buffer = "";
  	
    for(int i=0; i<input.length(); i++){
      if(input.charAt(i) == '/'){
      	if(buffer.contains(" ")) buffer = "\"" + buffer + "\"";
        output += buffer + "/";
        buffer = "";
      }else{
        buffer += input.charAt(i);
      }
    }
    
    if(!buffer.isEmpty()){
    	if(buffer.contains(" ")) buffer = "\"" + buffer + "\"";
      output += buffer;
    }
  	
  	return output;
  }
  
  /**
   * Get the path one directory up (unix paths only!)
   * @param path the current path
   * @return the path one directory up
   */
  public static String pathDotDot(String path)
  {  	
  	if(path.charAt(path.length()-1) == '/') path = path.substring(0, path.length()-1);
  	path = path.substring(0, path.lastIndexOf('/'));
  	
  	return path;
  }
  
  /**
   * Convert an absolute path to a relative path.
   *  @param path the current path from which we want the relative path to the target file
   *  @param target the absolute name of the target file
   *  @return the relative path to the file
   */
  public static String relativePath(String path, String target)
  {
    String relative_path = "";
    String path_next = "";
    String target_next = "";
    
    Scanner sc_path = new Scanner(path);
    sc_path.useDelimiter("/");
    Scanner sc_target = new Scanner(target);
    sc_target.useDelimiter("/");
    
    //Skip all directories that are in common
    while(sc_path.hasNext() && sc_target.hasNext()){
      path_next = sc_path.next();
      target_next = sc_target.next();
      if(!path_next.equals(target_next)) break;  
    }
    
    //If we entered a different subtree, go back
    if(!path_next.equals(target_next)){
      relative_path += "../"; 
    }
    
    //Continue going back for each additional sub-directory
    while(sc_path.hasNext()){ 
      sc_path.next(); 
      relative_path += "../";
    } 
    
    //Add on the differing sub-directory we entered
    relative_path += target_next + "/";
    
    //Add on all addtional sub-directories in the new sub-tree
    while(sc_target.hasNext()){
      relative_path += sc_target.next() + "/";
    }
    
    return relative_path;
  }
  
  /**
	 * Make sure the given filename has an absolute path (using the current working directory).
	 * @param filename a file name
	 * @return a file name with an absolute path
	 */
	public static String absolutePath(String filename)
	{  	
		return new File(filename).getAbsolutePath();
	}

	/**
   * Create an input stream for a file.
   *  @param filename the file to open
   *  @return the stream for this file
   */
  public static InputStream getInputStream(String filename)
  {
    InputStream is = null;
    
    try{
    	if(filename.contains("http://")){
    		is = (new URL(null, filename)).openStream();
    	}else{
    		if(filename.contains("file:/")){
    			filename = filename.substring(6);
    		}
    		
	      if(filename.contains(".gz")){
	        is = new GZIPInputStream(new FileInputStream(filename));
	      }else if(filename.contains(".zip")){
	        is = new ZipInputStream(new FileInputStream(filename));
	        ((ZipInputStream)is).getNextEntry();
	      }else{
	        is = new FileInputStream(filename);
	      }
    	}
    }catch(Exception e){
      e.printStackTrace();
    }
    
    return is;
  }
  
  /**
   * Sleep for the specfied number of mill-seconds.
   *  @param ms number of milli-seconds to sleep
   */
  public static void pause(int ms)
  {
    try{
      Thread.sleep(ms);
    }catch(Exception e) {e.printStackTrace();}
  }
  
  /**
   * Read the contents of the specified URL and store it in a string.
   * @param url the URL to read
   * @param type the accepted content type
   * @return a string containing the URL contents
   */
  public static String readURL(String url, String type)
  {
    HttpURLConnection.setFollowRedirects(false);
    HttpURLConnection conn = null;
    BufferedReader ins;
    StringBuilder outs = new StringBuilder();
    char[] buffer = new char[1024];
    //String line;
    String contents = "";
    int tmpi;
    
    try{
      conn = (HttpURLConnection)new URL(url).openConnection();     
      if(type != null) conn.setRequestProperty("Accept", type);
      conn.connect();
      
      if(url.endsWith(".gz")){
      	ins = new BufferedReader(new InputStreamReader(new GZIPInputStream(conn.getInputStream())));
      }else{
      	ins = new BufferedReader(new InputStreamReader(conn.getInputStream()));
      }
     
      /*
      while((line = ins.readLine()) != null){
        contents += line + "\n";
      }
      */
      
      do{
        tmpi = ins.read(buffer, 0, buffer.length);
        if(tmpi>0) outs.append(buffer, 0, tmpi);
      }while(tmpi>=0);
      
      contents = outs.toString();
 
      conn.disconnect();
    }catch(Exception e){
      e.printStackTrace();
    }finally{
      if(conn != null) conn.disconnect();
    }
    
    return contents;
  }
  
  /**
   * Read the contents of the specified URL and store it in a string.
   * @param url the URL to read
   * @return a string containing the URL contents
   */
  public static String readURL(String url)
  {
  	return readURL(url, null);
  }
  
  /**
   * Download a file from the web.   
   * @param path the path to save the file to
   * @param the name to give the file (can be null)
   * @param url the URL to the file
   * @param VERBOSE true if method should be verbose
   * @return true if the download occurred without problems
   */
  public static boolean downloadFile(String path, String name, String url, boolean VERBOSE)
  {
  	String filename = Utility.getFilename(url);
  	byte[] buffer = new byte[1024];
  	int length;

  	//Override the filename
  	if(name != null){  	
  		String extension = Utility.getFilenameExtension(filename);

  		if(!extension.isEmpty()){
  			filename = name + "." + extension;
  		}else{
  			filename = name;
  		}
  	}
  	
  	//Download the file
  	try{
  		InputStream is = new URL(url).openStream();
  		DataInputStream dis = new DataInputStream(new BufferedInputStream(is));
  		FileOutputStream fos = new FileOutputStream(new File(path + filename));
  		
  		while((length = dis.read(buffer)) >= 0){
  			fos.write(buffer, 0, length);
  		}
  		
  		is.close();
  		fos.close();
  	}catch(Exception e){
  		if(VERBOSE) e.printStackTrace();
  		return false;
  	}
  	
  	return true;
  }
  
  /**
   * Download a file from the web.   
   * @param path the path to save the file to
   * @param the name to give the file (can be null)
   * @param url the URL to the file
   * @return true if the download occurred without problems
   */
  public static boolean downloadFile(String path, String name, String url)
  {
  	return downloadFile(path, name, url, false);
  }
  
  /**
   * Download a file from the web.   
   * @param path the location to save the file to
   * @param url the URL to the file
   * @return true if the download occurred without problems
   */
  public static boolean downloadFile(String path, String url)
  {
  	return downloadFile(path, null, url, false);
  }
  
  /**
   * Post a file to a URL.
   * @param url the URL to post to
   * @param filename the name of the file to post
   * @param type the accepted content type
   * @return a string the resulting URL contents
   */
  public static String postFile(String url, String filename, String type)
  {
    HttpURLConnection conn = null;
    OutputStream os;
    PrintWriter writer = null;
    BufferedInputStream bis;
    BufferedReader br;
    StringBuilder sb = new StringBuilder();
    String boundary = Long.toHexString(System.currentTimeMillis());
    String response = "";
    char[] char_buffer = new char[1024];
    byte[] byte_buffer = new byte[1024];
    int tmpi;
    
    try{
      conn = (HttpURLConnection)new URL(url).openConnection();
      conn.setDoInput(true);
      conn.setDoOutput(true);
      //conn.setRequestMethod("POST");
      conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
      if(type != null) conn.setRequestProperty("Accept", type);
      conn.connect();
            
      //Upload file
      os = conn.getOutputStream();
      writer = new PrintWriter(new OutputStreamWriter(os), true);
      writer.print("--" + boundary + "\r\n");
      writer.print("Content-Disposition: form-data; name=\"file\"; filename=\"" + getFilename(filename) + "\";\r\n");
      writer.print("Content-Type: " + URLConnection.guessContentTypeFromName(getFilename(filename)) + "\r\n");
      writer.print("Content-Transfer-Encoding: binary\r\n");
      writer.print("\r\n");
      writer.flush();
      
      bis = new BufferedInputStream(new FileInputStream(filename));

      do{
        tmpi = bis.read(byte_buffer, 0, byte_buffer.length);
        if(tmpi>0) os.write(byte_buffer, 0, tmpi);
      }while(tmpi>=0);
      
      os.flush();
      bis.close(); 

      writer.print("\r\n");
      writer.print("--" + boundary + "--\r\n");
      writer.flush();
      
      //Get response
      br = new BufferedReader(new InputStreamReader(conn.getInputStream()));

      do{
        tmpi = br.read(char_buffer, 0, char_buffer.length);
        if(tmpi>0) sb.append(char_buffer, 0, tmpi);
      }while(tmpi>=0);
      
      response = sb.toString();
 
      conn.disconnect();
    }catch(Exception e){
      e.printStackTrace();
    }finally{
      if(writer != null) writer.close();
      if(conn != null) conn.disconnect();
    }
    
    return response;
  }
  
  /**
   * Post a file to a URL.
   * @param url the URL to post to
   * @param filename the name of the file to post
   * @return a string the resulting URL contents
   */
  public static String postFile(String url, String filename)
  {
  	return postFile(url, filename, null);
  }
  
  /**
   * Post a file to a URL.
   * @param url the URL to post to
   * @param filename the name of the file 
   * @param is the file input stream
   * @param type the accepted content type
   * @return a string the resulting URL contents
   */
  public static String postFile(String url, String filename, InputStream is, String type)
  {
    HttpURLConnection conn = null;
    OutputStream os;
    PrintWriter writer = null;
    BufferedInputStream bis;
    BufferedReader br;
    StringBuilder sb = new StringBuilder();
    String boundary = Long.toHexString(System.currentTimeMillis());
    String response = "";
    char[] char_buffer = new char[1024];
    byte[] byte_buffer = new byte[1024];
    int tmpi;
    
    try{
      conn = (HttpURLConnection)new URL(url).openConnection();
      conn.setDoInput(true);
      conn.setDoOutput(true);
      //conn.setRequestMethod("POST");
      conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
      if(type != null) conn.setRequestProperty("Accept", type);
      conn.connect();
            
      //Upload file
      os = conn.getOutputStream();
      writer = new PrintWriter(new OutputStreamWriter(os), true);
      writer.print("--" + boundary + "\r\n");
      writer.print("Content-Disposition: form-data; name=\"file\"; filename=\"" + Utility.getFilename(filename) + "\";\r\n");
      writer.print("Content-Type: " + URLConnection.guessContentTypeFromName(Utility.getFilename(filename)) + "\r\n");
      writer.print("Content-Transfer-Encoding: binary\r\n");
      writer.print("\r\n");
      writer.flush();
      
      bis = new BufferedInputStream(is);

      do{
        tmpi = bis.read(byte_buffer, 0, byte_buffer.length);
        if(tmpi>0) os.write(byte_buffer, 0, tmpi);
      }while(tmpi>=0);
      
      os.flush();
      bis.close(); 

      writer.print("\r\n");
      writer.print("--" + boundary + "--\r\n");
      writer.flush();
      
      //Get response
      br = new BufferedReader(new InputStreamReader(conn.getInputStream()));

      do{
        tmpi = br.read(char_buffer, 0, char_buffer.length);
        if(tmpi>0) sb.append(char_buffer, 0, tmpi);
      }while(tmpi>=0);
      
      response = sb.toString();
 
      conn.disconnect();
    }catch(Exception e){
      e.printStackTrace();
    }finally{
      if(writer != null) writer.close();
      if(conn != null) conn.disconnect();
    }
    
    return response;
  }
  
  /**
   * Convert a string to an URL safe version (e.g. spaces -> %20).
   *  @param str the string to convert
   *  @return the URL safe version
   */
  public static String urlEncode(String str)
  {
    String str_new = "";
    
    for(int i=0; i<str.length(); i++){
      if(str.charAt(i) == ' '){
        str_new += "%20";
      }else if(str.charAt(i) == '#'){
        str_new += "%23";
      }else if(str.charAt(i) == ':'){
      	str_new += "%3A";
      }else if(str.charAt(i) == '/'){
      	str_new += "%2F";
      }else if(str.charAt(i) == '~'){
      	str_new += "%7E";
      }else{
        str_new += str.charAt(i);
      }
    }
    
    return str_new;
  }
  
  /**
   * Convert an integer representing some number of bytes into a human readable string.
   *  @param x the number of bytes
   *  @return the human readable string
   */
  public static String getBytes(int x)
  {
    String str = "";
    
    if(x < 1024){
      str = Integer.toString(x) + " B";
    }else{
      x /= 1024;
      
      if(x < 1024){
        str = Integer.toString(x) + " KB";
      }else{
        x /= 1024;
        str = Integer.toString(x) + " MB";
      }
    }

    return str;
  }
  
	/**
   * Capitalize the first letter of a given word.
   * @param word the word to capitalize
   * @return the capitalized word
   */
  public static String capitalize(String word)
  {
  	String tmp = "";
  	boolean CAPITALIZE = true;
  	char c;
  	
  	word = word.toLowerCase();
  	
  	for(int i=0; i<word.length(); i++){
  		c = word.charAt(i);
  		
  		if(c == ' ' || c == '_'){
  			tmp += ' ';
  			CAPITALIZE = true;
  		}else if(CAPITALIZE){
  			tmp += Character.toUpperCase(c);
  			CAPITALIZE = false;
  		}else{
  			tmp += c;
  		}
  	}
  	
  	return tmp;
  }
  
  /**
   * Check if the specified URL exists.
   *  @param url the URL to check
   *  @return true if the URL exists
   */
  public static boolean existsURL(String url)
  {
    HttpURLConnection.setFollowRedirects(false);
    HttpURLConnection conn = null;
    boolean SUCCESS = false;
    
    try{
      conn = (HttpURLConnection)new URL(url).openConnection();
      conn.connect();
      
      //Try to open the stream, if it doesn't exist it will cause an exception.
      DataInputStream ins = new DataInputStream(new BufferedInputStream(conn.getInputStream()));
      ins.close();
      
      conn.disconnect();
      SUCCESS = true;
    }catch(FileNotFoundException e){
    }catch(Exception e){
      e.printStackTrace();
    }finally{
      if(conn != null) conn.disconnect();
    }
    
    return SUCCESS;
  }
  
  /**
   * Check if the given host is reachable.
   * @param host the host to check
   * @param timeout the maximum time to spend checking (in milliseconds)
   * @return true if the host is reachable
   */
  public static boolean isHostReachable(String host, int timeout)
  {
		try{
			InetAddress address = InetAddress.getByName(host);
			
			return address.isReachable(timeout);
		}catch(Exception e) {e.printStackTrace();}
		
		return false;
  }
  
  /**
   * Convert an integer to a string padding it to occupy the desired number of characters.
   *  @param i the integer to convert
   *  @param n the number of characters the result should have
   *  @return the string representation of the integer
   */
  public static String toString(int i, int n)
  {
    String tmp = Integer.toString(i);
    
    while(tmp.length() < n){
      tmp = "0" + tmp;
    }
    
    return tmp;
  }
  
  /**
   * Round a double value to a string preserving n decimal places.
   *  @param a the double to round
   *  @param n the number of places to preserve
   *  @return the rounded string representation of the given double
   */
  public static String round(double a, int n)
  {
  	double b = Math.pow(10, n);
  	
  	a = Math.round(a*b);
  	
  	return Double.toString(a/b);
  }
  
  /**
   * Read lines from a stream until one is found that does not start with a '#'.
   *  @param ins the input stream
   *  @return a line from the stream
   */
  public static String nextUncommentedLine(BufferedReader ins)
  {
    String line;
    
    try{
      while((line=ins.readLine()) != null){
        if(line.charAt(0) != '#') return line;
      }
    }catch(Exception e){
      e.printStackTrace();
    }
    
    return null;
  }
  
  /**
   * Separate a string into a vector of strings (separated by the specified delimiter).
   * @param line the string to split
   * @param delimiter the delimiter to split on
   * @param DROP_EMPTY_STRINGS true if we should ignore empty strings
   * @param TRIM trim whitespace on ends of strings
   * @return the vector of strings separated from the given string
   */
  public static Vector<String> split(String line, char delimiter, boolean DROP_EMPTY_STRINGS, boolean TRIM)
  {
    Vector<String> strings = new Vector<String>();
    String string = "";
    
    for(int i=0; i<line.length(); i++){
      if(line.charAt(i) == delimiter){
        if(!string.isEmpty() || !DROP_EMPTY_STRINGS){
        	if(TRIM) string = string.trim();
          strings.add(string);
          string = "";
        }
      }else{
        string += line.charAt(i);
      }
    }
    
    if(!string.isEmpty()){
    	if(TRIM) string = string.trim();
    	strings.add(string);
    }
    
    return strings;
  }
  
  /**
   * Separate a string into a vector of strings (separated by the specified delimiter).
   * @param line the string to split
   * @param delimiter the delimiter to split on
   * @param DROP_EMPTY_STRINGS true if we should ignore empty strings
   * @return the vector of strings separated from the given string
   */
  public static Vector<String> split(String line, char delimiter, boolean DROP_EMPTY_STRINGS)
  {
  	return split(line, delimiter, DROP_EMPTY_STRINGS, false);
  }
  
  /**
   * Separate a string into a vector of strings (separated by the specified delimiter).
   * @param line the string to split
   * @param delimiter the delimiter to split on
   * @return the vector of strings separated from the given string
   */
  public static Vector<String> split(String line, char delimiter)
  {
  	return split(line, delimiter, false, false);
  }
  
	/**
	 * Check if the given array contains the given element.
	 * @param array the array to check
	 * @param element the element to look for
	 * @return true if the element was found
	 */
	public static boolean contains(Object[] array, Object element)
	{
		for(int i=0; i<array.length; i++){
			if(element.equals(array[i])){
				return true;
			}
		}
		
		return false;
	}
  
	/**
	 * Collapse a vector of strings into a single string.
	 * @param strings a vector of strings
	 * @return a single string
	 */
	public static String collapse(Vector<String> strings)
	{
		String string = "";
		
		for(int i=0; i<strings.size(); i++){
			if(i > 0) string += "\n";
			string += strings.get(i);
		}
		
		return string;
	}
	
  /**
   * Remove preceding and ending white space from a string
   *  @param str the string to trim
   *  @return the trimmed string
   */
  public static String trim(String str)
  {
    int left = 0;
    int right = str.length()-1;
    
    for(int i=0; i<str.length(); i++){
      if(str.charAt(i)!=' ' && str.charAt(i)!='\n') break;
      left++;
    }
    
    for(int i=str.length()-1; i>=0; i--){
      if(str.charAt(i) != ' ' && str.charAt(i) != '\n') break;
      right--;
    }
    
    return str.substring(left, right+1);
  }
  
  /**
   * Create a string filled with the specified number of spaces.
   * @param n the number of spaces
   * @return the created string of spaces
   */
  public static String spaces(int n)
  {
  	String string = "";
  	
  	for(int i=0; i<n; i++){
  		string += " ";
  	}
  	
  	return string;
  }
  
  /**
   * Increment an array's index so as to always keep it within bounds.
   *  @param index the current index
   *  @param inc the increment to the index (positive or negative)
   *  @param size the size of the array
   *  @return the new incremented index
   */
  public static int increment(int index, int inc, int size)
  {
    index += inc;
    
    while(index < 0){
      index = size + index;
    }
    
    index = index % size;
    
    return index;
  }
  
  /**
   * Determine if given double contains a valid value.
   *  @param d the double value to check
   *  @return true if not infinity or NaN
   */
  public static boolean isValid(double d)
  {
    if(Double.isNaN(d) || Double.isInfinite(d)){
      return false;
    }
    
    return true;
  }
  
  /**
   * Perform a deep copy of the given object.
   *  @param obj the object to copy
   *  @return the deep copy of the object
   */
  public static Object deepCopy(Object obj)
  {
  	Object obj_copy = null;
  	
  	try{
	  	ByteArrayOutputStream baos = new ByteArrayOutputStream();
	  	ObjectOutputStream oos = new ObjectOutputStream(baos);
	  	oos.writeObject(obj);
	  	
	  	ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
	  	ObjectInputStream ois = new ObjectInputStream(bais);
  	  obj_copy =  ois.readObject();
  	}catch(Exception e){
  		e.printStackTrace();
  	}
  	
  	return obj_copy;
  }
  
  /**
   * Get the name of a file (i.e. no path)
   *  @param filename the absolute file name
   *  @return the name of the file (with extension)
   */
  public static String getFilename(String filename)
  {
  	String name = filename;
  	int tmpi;
  	
  	//Remove path
  	tmpi = filename.lastIndexOf('/');
  	
  	if(tmpi >= 0){
  	  name = filename.substring(tmpi+1);
  	}
  	
  	return name;
  }
  
  /**
   * Get the name of a file (i.e. no path and no extension)
   *  @param filename the absolute file name
   *  @return the name of the file
   */
  public static String getFilenameName(String filename)
  {
  	String name = filename;
  	int tmpi;
  	
  	//Remove path
  	tmpi = filename.lastIndexOf('/');
  	
  	if(tmpi >= 0){
  	  name = filename.substring(tmpi+1);
  	}
  	
  	//Remove extension
  	tmpi = name.lastIndexOf('.');
  	  
	  if(tmpi >= 0){
	  	name = name.substring(0, tmpi);
	  }
  	
  	return name;
  }
  
  /**
   * Get the extension of a file
   *  @param filename the absolute file name
   *  @return the extension of the file
   */
  public static String getFilenameExtension(String filename)
  {
  	String ext = "";
  	int tmpi = filename.lastIndexOf('.');
  	
  	if(tmpi >= 0){
  	  ext = filename.substring(tmpi+1);
  	}
  	
  	return ext;
  }
  
  /**
   * Get the path of a file
   *  @param filename the absolute file name
   *  @return the path to the file
   */
  public static String getFilenamePath(String filename)
  {
  	String path = "";
  	int tmpi = filename.lastIndexOf('/');
  	
  	if(tmpi >= 0){
  	  path = filename.substring(0, tmpi+1);
  	}
  	
  	return path;
  }
  
	/**
	 * Get the MD5 checksum for the given file.
	 * @param filename the name of the file
	 * @return a string containing the MD5 checksum
	 */
	public static String getMD5Checksum(String filename)
	{
		String checksum = null;
		byte[] digest = null;
		
		try{  		
			MessageDigest md = MessageDigest.getInstance("MD5");
			InputStream is = new FileInputStream(filename);
			byte[] buffer = new byte[1024];
			int n;
			
			while((n=is.read(buffer)) != -1){
				md.update(buffer, 0, n);
			}
			
			is.close();
			digest = md.digest();
		}catch(Exception e) {e.printStackTrace();}
		
		if(digest != null){
			checksum = "";
			
			for(int i=0; i<digest.length; i++){
				checksum += Integer.toString((digest[i]&0xff)+0x100, 16).substring(1);
			}
		}
		
		return checksum;
	}
	
	/**
	 * Get the host from a connection specified as host:port.
	 * @param connection the connection specified as host:port
	 * @return the host
	 */
	public static String getHost(String connection)
	{
		String host = null;
   	int tmpi = connection.lastIndexOf(':');
		
		if(tmpi != -1){
			host = connection.substring(0, tmpi);
		}
		
		return host;
	}
	
	/**
	 * Get the host name from a connection specified as host:port.
	 * @param connection the connection specified as host:port
	 * @return the host name
	 */
	public static String getHostName(String connection)
	{
		String host = null;
   	int tmpi = connection.lastIndexOf(':');
		
		if(tmpi != -1){
			try{
				host = InetAddress.getByName(connection.substring(0, tmpi)).getHostName();
			}catch(Exception e) {e.printStackTrace();}			
		}
		
		return host;
	}
	
	/**
	 * Get the host address from a connection specified as host:port.
	 * @param connection the connection specified as host:port
	 * @return the host address
	 */
	public static String getHostAddress(String connection)
	{
		String host = null;
   	int tmpi = connection.lastIndexOf(':');
		
		if(tmpi != -1){
			try{
				host = InetAddress.getByName(connection.substring(0, tmpi)).getHostAddress();
			}catch(Exception e) {e.printStackTrace();}			
		}
		
		return host;
	}
	
	/**
	 * Get the port number from a connection specified as server:port.
	 * @param connection the connection specified as server:port
	 * @return the port number
	 */
	public static int getPort(String connection)
	{
		int port = -1;
   	int tmpi = connection.lastIndexOf(':');
		
		if(tmpi != -1){
			port = Integer.valueOf(connection.substring(tmpi+1));
		}
		
		return port;
	}

	/**
	 * Get the target of a Windows shortcut.
	 * @param shortcut the shortcut (*.lnk) file to parse
	 * @return the target of the shortcut
	 */
	public static String getShortcutTarget(String shortcut)
	{
		String target = null;
		byte[] bytes = null;
		
		//Read file into a byte buffer
    try{
	    FileInputStream fis = new FileInputStream(new File(shortcut));
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    byte[] buff = new byte[256];
	    int n;
	    
	    while(true){
	      n = fis.read(buff);
	      if(n == -1) break;
	      baos.write(buff, 0, n);
	    }
	    
	    fis.close();
	    bytes = baos.toByteArray();
    }catch(Exception e) {e.printStackTrace();}
    
    //Extract information
    byte flags = bytes[0x14];
    byte attributes = bytes[0x18];
    boolean DIRECTORY = (attributes & (byte)0x10) > 0;
    int shell_len = 0;

    if((flags & (byte)0x01) > 0){    						//If the shell settings are present, skip them
    	shell_len = bytes2short(bytes, 0x4c) + 2;  //The plus 2 accounts for the length marker itself
    }

    int file_start = 0x4c + shell_len;
    int finalname_offset = bytes[file_start + 0x18] + file_start;
    String finalname = getNullDelimitedString(bytes, finalname_offset);
        
    //int file_location_info_flag = bytes[file_start + 0x08];
    //boolean LOCAL = (file_location_info_flag & 2) == 0;
    
    //if(!LOCAL){      
    //	int networkvolumetable_offset = bytes[file_start + 0x14] + file_start;
    //  int sharename_offset = bytes[networkvolumetable_offset + 0x08] + networkvolumetable_offset;
    //  String sharename = getNullDelimitedString(bytes, sharename_offset);
      
    //  target = sharename + "\\" + finalname;
    //}else{
      int basename_offset = bytes[file_start + 0x10] + file_start;
      String basename = getNullDelimitedString(bytes, basename_offset);
      
      target = basename + finalname;
    //}
    
		return target;
	}
	
	/**
	 * Convert a series of bytes to a short.
	 * @param bytes a byte array
	 * @param off an offset within the byte array
	 * @return a short
	 */
  private static int bytes2short(byte[] bytes, int off)
  {
    int low = bytes[off]<0 ? bytes[off]+256 : bytes[off];
    int high = (bytes[off+1]<0 ? bytes[off+1]+256 : bytes[off+1])<<8;
    
    return 0 | low | high;
  }
  
  /**
   * Extract a string from a byte array.
   * @param bytes a byte array
   * @param off an offset within the byte array
   * @return a string
   */
  private static String getNullDelimitedString(byte[] bytes, int off)
  {
    int len = 0;
    
    while(true){	//Count bytes until the null character
      if(bytes[off + len] == 0) break;
      len++;
    }
    
    return new String(bytes, off, len);
  }

	/**
	 * Search the given directory for files containing the given string.
	 * @param path the path to search
	 * @param string the string to match
	 * @return the matches found
	 */
	public static Vector<String> search(String path, String string)
	{
		Vector<String> matches = new Vector<String>();
		Stack<File> directories = new Stack<File>();
		File directory;
		File[] files;
		
		string = string.toLowerCase();	//Ignore case
		
		if(exists(path)){
			directories.add(new File(path));
			
			while(!directories.isEmpty()){
				directory = directories.pop();
				files = directory.listFiles();
				
				if(files != null){
					for(int i=0; i<files.length; i++){
						if(files[i].isDirectory()){
							directories.add(files[i]);
						}else if(files[i].getName().toLowerCase().contains(string)){
							matches.add(files[i].getAbsolutePath());
						}
					}
				}
			}
		}
		
		return matches;
	}
  
  /**
   * Return the union of unique elements within two vectors.
   * @param vector1 a vector of elements
   * @param vector2 a vector of elements
   * @return the union of the two vectors
   */
  public static Vector union(Vector vector1, Vector vector2)
  {
  	Vector vector = new Vector();
  	TreeSet set = new TreeSet();
  	
  	if(vector1 != null) set.addAll(vector1);
  	if(vector2 != null) set.addAll(vector2);
  	
  	vector.addAll(set);
  	
  	return vector;
  }
  
  /**
   * Calculate the mean of the given vector's values
   * @param vector the vector
   * @return the mean value
   */
  public static double mean(Vector<Double> vector)
  {
  	double mean = 0;
  	
  	for(int i=0; i<vector.size(); i++){
  		mean += vector.get(i);
  	}
  	
  	if(!vector.isEmpty()) mean /= vector.size();
  	
  	return mean;
  }
  
  /**
   * Calculate the standard deviation of the given vector's values
   * @param vector the vector
   * @param mean the mean value
   * @return the standard deviation
   */
  public static double std(Vector<Double> vector, double mean)
  {
  	double std = 0;
  	double tmpd;
  	
  	for(int i=0; i<vector.size(); i++){
  		tmpd = vector.get(i) - mean;
  		std += tmpd*tmpd;
  	}
  	
  	if(!vector.isEmpty()) std /= vector.size();
  	std = Math.sqrt(std);
  	
  	return std;
  }
  
	/**
	 * Convert an integer into an array of 4 bytes.
	 * @param integer the integer to convert
	 * @param BIG_ENDIAN true if the bytes should be stored in big endian order
	 * @return the array of bytes representing the integer
	 */
	public static byte[] intToBytes(int integer, boolean BIG_ENDIAN)
	{
		byte[] bytes = new byte[4];
		
		if(BIG_ENDIAN){
			bytes[0] = (byte)((integer >> 24) & 0x000000ff);
			bytes[1] = (byte)((integer >> 16) & 0x000000ff);
			bytes[2] = (byte)((integer >> 8) & 0x000000ff);
			bytes[3] = (byte)(integer & 0x000000ff);
		}else{
			bytes[0] = (byte)(integer & 0x000000ff);
			bytes[1] = (byte)((integer >> 8) & 0x000000ff);
			bytes[2] = (byte)((integer >> 16) & 0x000000ff);
			bytes[3] = (byte)((integer >> 24)& 0x000000ff);
		}

		return bytes;
	}
	
	/**
	 * Convert an unsigned byte to an int.
	 * @param b the byte to convert
	 * @return the integer representation of the unsigned bytes value
	 */
  public static int byteToInt(byte b)
  {
    return (int)b & 0xff;
  }	
  
	/**
	 * Convert 4 bytes into an integer.
	 * @param b0 the first byte representing the the integer
	 * @param b1 the second byte representing the the integer
	 * @param b2 the third byte representing the the integer
	 * @param b3 the fourth byte representing the the integer
	 * @param BIG_ENDIAN true if the bytes should be stored in big endian order
	 * @return the integer represented by the given 4 bytes
	 */
	public static int bytesToInt(byte b0, byte b1, byte b2, byte b3, boolean BIG_ENDIAN)
	{
		int i0 = byteToInt(b0);
		int i1 = byteToInt(b1);
		int i2 = byteToInt(b2);
		int i3 = byteToInt(b3);
		
		if(BIG_ENDIAN){
			return (i0<<24) | i1<<16 | i2<<8 | i3;
		}else{
			return (i3<<24) | i2<<16 | i1<<8 | i0;
		}
	}
	
	/**
	 * Convert 2 bytes into a short.
	 * @param b0 the first byte representing the the integer
	 * @param b1 the second byte representing the the integer
	 * @param BIG_ENDIAN true if the bytes should be stored in big endian order
	 * @return the short represented by the given 2 bytes
	 */
	public static short bytesToShort(byte b0, byte b1, boolean BIG_ENDIAN)
	{
		int i0 = byteToInt(b0);
		int i1 = byteToInt(b1);
		
		if(BIG_ENDIAN){
			return (short)((i0<<8) | i1);
		}else{
			return (short)(i1<<8 | i0);
		}
	}
	
	/**
	 * Retrieve a sub-array from a larger array.
	 * @param array the larger array from which we want a sub-array
	 * @param i0 the starting index of the sub-array
	 * @param i1 the ending index of the sub-array
	 * @return the sub-array
	 */
	public static byte[] subArray(byte[] array, int i0, int i1)
	{
		int n = i1 - i0 + 1;
		byte[] sub_array = new byte[n];
		
		for(int i=0; i<n; i++){
			sub_array[i] = array[i+i0];
		}
		
		return sub_array;
	}
	
	/**
	 * Convert a serializable object into an array of bytes.
	 * @param object the object
	 * @return an array of bytes representing that object
	 */
	public static byte[] objectToBytes(Object object)
	{
		byte[] bytes = null;
		
  	try{
	  	ByteArrayOutputStream baos = new ByteArrayOutputStream();
	  	ObjectOutputStream oos = new ObjectOutputStream(baos);
	  	oos.writeObject(object);
	  	oos.close();
	  	
	  	bytes = baos.toByteArray();
  	}catch(Exception e) {e.printStackTrace();}
  	
  	return bytes;
	}
	
	/**
	 * Convert an array of bytes into an object.
	 * @param bytes an array of bytes
	 * @return an object represented by the given array of bytes
	 */
	public static Object bytesToObject(byte[] bytes)
	{
		Object object = null;
		
  	try{
	  	ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
	  	ObjectInputStream ois = new ObjectInputStream(bais);
  	  object =  ois.readObject();
  	}catch(EOFException e){
  	}catch(Exception e) {e.printStackTrace();}
  	
  	return object;
	}
	
	/**
	 * Write an array of bytes to an output stream.
	 * @param outs the output stream
	 * @param bytes the array of bytes
	 */
	public static void writeArray(OutputStream outs, byte[] bytes) throws Exception
	{
		outs.write(intToBytes(bytes.length, false));
		outs.write(bytes);
	}
	
	/**
	 * Read an array of bytes from an input stream.
	 * @param ins the input stream
	 * @return the array of bytes
	 */
	public static byte[] readArray(InputStream ins) throws Exception
	{
		byte[] bytes = null;
		int length, total, tmpi;
		
		//Get length
		bytes = new byte[4];
		ins.read(bytes, 0, bytes.length);
		length = bytesToInt(bytes[0], bytes[1], bytes[2], bytes[3], false);
		
		//Get data
		bytes = new byte[length];
		total = 0;
		
		while(total < length){
			tmpi = ins.read(bytes, total, length-total);
			total += tmpi;
		}
		
		return bytes;
	}
	
	/**
	 * Write an object as bytes to an output stream.
	 * @param outs the output stream
	 * @param object the object to write
	 */
	public static void writeObject(OutputStream outs, Object object) throws Exception
	{
		writeArray(outs, Utility.objectToBytes(object));
	}
	
	/**
	 * Read an object as bytes from an input stream.
	 * @param ins the input stream
	 * @return the object
	 */
	public static Object readObject(InputStream ins) throws Exception
	{
		return Utility.bytesToObject(Utility.readArray(ins));
	}
	
  /**
   * Acquire a file lock.
   * @param filename the file name to use as a lock
   * @return the file lock
   */
  public static FileLock lock(String filename)
  {
  	FileLock lock = null;
  	
  	//Create a lock file if it doesn't exist
  	if(!Utility.exists(filename)){
      try{
        BufferedWriter outs = new BufferedWriter(new FileWriter(filename));
        outs.close();
      }catch(Exception e) {e.printStackTrace();}
  	}
  	
  	//Grab the lock file  	
  	try{
  		lock = (new RandomAccessFile(filename, "rw").getChannel()).lock();
  	}catch(Exception e) {e.printStackTrace();}
  	
  	return lock;
  }
  
  /**
   * Release the lock if currently held.
   * @param lock the file lock
   */
  public static void unlock(FileLock lock)
  {
    if(lock != null){
    	try{
    		lock.release();
    		lock.channel().close();
    	}catch(Exception e) {e.printStackTrace();}
    }
  }
  
	/**
	 * Unzip a zip file into the given path.
	 * @param path the path to unzip to
	 * @param filename the name of the zip file
	 */
	public static void unzip(String path, String filename)
	{
		byte[] buffer = new byte[1024];
		int length;
		
		if(Utility.getFilenameExtension(filename).equals("zip")){
			try{
				ZipFile zf = new ZipFile(filename);
				Enumeration entries = zf.entries();
				
				while(entries.hasMoreElements()){
					ZipEntry entry = (ZipEntry)entries.nextElement();
					
					if(entry.isDirectory()){
						(new File(path + entry.getName())).mkdir();
					}else{						
						InputStream ins = zf.getInputStream(entry);
						OutputStream outs = new BufferedOutputStream(new FileOutputStream(path + entry.getName()));
						
						while((length = ins.read(buffer)) >= 0){
							outs.write(buffer, 0, length);
						}
						
						ins.close();
						outs.close();
					}
				}
				
				zf.close();
			}catch(Exception e) {e.printStackTrace();}
		}
	}
	
	/**
	 * Zip a given set of files.
	 * @param output the output zip file
	 * @param files a vector of files
	 */
	public static void zip(String output, Vector<String> files)
	{
		try{
			ZipOutputStream outs = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(output)));
			
			for(int i=0; i<files.size(); i++){
				zip(outs, Utility.getFilenamePath(files.get(i)), Utility.getFilename(files.get(i)));
			}
			
			outs.close();
		}catch(Exception e) {e.printStackTrace();}
	}
	
	/**
	 * Add a file to the given zip file output stream.
	 * @param outs the zip file output stream
	 * @param path the path to the file (minus the portion of the path that should be stored in the zip file)
	 * @param filename the name of the file to add (plus the portion of the path that should be stored in the zip file)
	 */
	public static void zip(ZipOutputStream outs, String path, String filename)
	{
		File file = new File(path + filename);
		try{		
			if(file.isDirectory()){
				File[] files = file.listFiles();
				
				outs.putNextEntry(new ZipEntry(filename + "/"));
	
				for(int i=0; i<files.length; i++){
					zip(outs, path, file.getName() + "/" + files[i].getName());
				}
			}else{	
				BufferedInputStream ins = new BufferedInputStream(new FileInputStream(file));
				byte[] buffer = new byte[1024];
				int length;
				
				outs.putNextEntry(new ZipEntry(filename));
				
				while((length = ins.read(buffer, 0, 1024)) != -1){
					outs.write(buffer, 0, length);
				}
				
				ins.close();
			}
		}catch(Exception e) {e.printStackTrace();}
	}
	
	/**
   * A main for debug purposes.
   * @param args command line arguments
   */
  public static void main(String args[])
  {
  	if(false){
  		Utility.downloadFile("C:/users/kmchenry/Desktop/", "test", "http://isda.ncsa.uiuc.edu/~kmchenry/images/scifi/1701da.jpg");
  	}
  	
  	if(false){
  		Vector<String> files = new Vector<String>();
  		files.add("C:/Kenton/Data/Temp/ICRMonkey/000_open.ms");
  		files.add("C:/Kenton/Data/Temp/ICRMonkey/000_open");
  		Utility.zip("C:/Kenton/Data/Temp/ICRMonkey/000_open.zip", files);
  	}
  	
  	if(false){
  		Utility.unzip("C:/Kenton/Data/Temp/ICRMonkey/tmp/", "C:/Kenton/Data/Temp/ICRMonkey/000_open.zip");
  	}
  	
  	if(false){
  		Vector<String> matches = Utility.search("C:/Program Files (x86)", "convert.exe");
  		
  		for(int i=0; i<matches.size(); i++){
  			System.out.println(matches.get(i));
  		}
  	}
  	
  	if(false){
  		//System.out.println(Utility.getMD5Checksum("C:/Users/kmchenry/Files/Data/Images/scar1.jpg"));
  		System.out.println(Utility.windowsDrivePath("C:/Users/kmchenry/Files/Data/Images/scar1.jpg"));
  	}
  	
  	if(false){
  		System.out.println("Shortcut target: " + Utility.getShortcutTarget("C:/Users/kmchenry/Desktop/Notes.txt.lnk"));
  		//System.out.println("Shorcut target: " + Utility.getShortcutTarget("C:/Users/kmchenry/Desktop/Microsoft Office Word 2007.lnk"));
  	}
  	
  	if(true){
  		//System.out.println(readURL("http://localhost:8182/software/A3DReviewer/convert/igs/http%3A%2F%2Fisda.ncsa.uiuc.edu%2Fkmchenry%2Ftmp%2FPolyglot2%2FPolyglotDemo%2Fpump.stp", "text/plain"));
  		System.out.println(postFile("http://localhost:8182/software/A3DReviewer/convert/igs/", "C:/Users/kmchenry/Files/Data/NARA/DataSets/PolyglotDemo/pump.stp", "text/plain"));
  	}
  }
}
package edu.illinois.ncsa.isda.softwareserver.polyglot;
import edu.illinois.ncsa.isda.softwareserver.*;
import kgm.utility.Utility;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.servlet.*;
import org.restlet.*;
import org.restlet.resource.*;
import org.restlet.data.*;
import org.restlet.representation.*;
import org.restlet.routing.*;
import org.restlet.security.*;
import org.restlet.ext.fileupload.*;
import org.apache.commons.fileupload.*;
import org.apache.commons.fileupload.disk.*;

/**
 * A restful interface for a polyglot steward.
 * Think of this as an extended polyglot steward.
 * @author Kenton McHenry
 */
public class PolyglotRestlet extends ServerResource
{
  private static PolyglotSteward polyglot = new PolyglotSteward();
	private static int steward_port = -1;
	private static int port = 8184;
	private static boolean RETURN_URL = false;
	private static String root_path = "./";
	private static String temp_path = root_path + "Temp";
	private static String public_path = root_path + "Public";
	private static Component component;
	
	/**
	 * Set whether or not to return a URL as the result (as opposed to the file itself)
	 * @param value true if the URL to the resulting file should be returned
	 */
	public void setReturnURL(boolean value)
	{
		RETURN_URL = value;
	}
	
	/**
	 * Get a web form interface for this restful service.
	 * @param POST_UPLOADS true if this form should use POST rather than GET for uploading files
	 * @param selected_output the default output format
	 * @return the form
	 */
	public String getForm(boolean POST_UPLOADS, String selected_output)
	{
		String buffer = "";
		String output;
		boolean FIRST_BLOCK;
		boolean FIRST_VALUE;
		
		buffer += "<script type=\"text/javascript\">\n";
		buffer += "function setInputs(){\n";
		buffer += "  var inputs = document.getElementById('inputs');\n";
		buffer += "  var outputs = document.getElementById('output');\n";
		buffer += "  var output = outputs.options[outputs.selectedIndex].value;\n";
		buffer += "  \n";
		buffer += "  inputs.innerHTML = \"\";\n";
		buffer += "  \n";
		
		FIRST_BLOCK = true;
		
		for(Iterator<String> itr1=polyglot.getOutputs().iterator(); itr1.hasNext();){
			output = itr1.next();
			if(FIRST_BLOCK) FIRST_BLOCK = false; else	buffer += "\n";
			buffer += "  if(output == \"" + output + "\"){\n";	
			buffer += "    inputs.innerHTML = \"";
			FIRST_VALUE = true;
			
			for(Iterator<String> itr2=polyglot.getInputs(output).iterator(); itr2.hasNext();){
				if(FIRST_VALUE) FIRST_VALUE = false; else buffer += ", ";
				buffer += itr2.next();
			}
			
			buffer += "\";\n";
			buffer += "  }\n";		
		}
		
		buffer += "}\n";
		buffer += "\n";
		buffer += "function setAPICall(){\n";
		buffer += "  var file = document.getElementById('file').value;\n";
		buffer += "  var outputs = document.getElementById('output');\n";
		buffer += "  var output = outputs.options[outputs.selectedIndex].value;\n";
		
		if(!POST_UPLOADS){
			buffer += "  var api_url = \"http://\" + location.host + \"/convert/\" + output + \"/\" + encodeURIComponent(file);\n";
			buffer += "  var api_html = \"http://\" + location.host + \"/convert/<font color=\\\"#7777ff\\\">\" + output + \"</font>/<font color=\\\"#777777\\\">\" + encodeURIComponent(file) + \"</font>\";\n";
		}else{
			buffer += "  var api_url = \"http://\" + location.host + \"/convert/\" + output + \"/\";\n";
			buffer += "  var api_html = \"http://\" + location.host + \"/convert/<font color=\\\"#7777ff\\\">\" + output + \"</font>/\";\n";
		}
		
		buffer += "  \n";
		buffer += "  api.innerHTML = \"<i><b><font color=\\\"#777777\\\">REST API call</font></b><br><br><a href=\\\"\" + api_url + \"\\\"style=\\\"text-decoration:none; color:#777777\\\">\" + api_html + \"</a></i>\";\n";
		buffer += "  setTimeout('setAPICall()', 500);\n";
		buffer += "}\n";
		buffer += "</script>\n";
		buffer += "\n";
		buffer += "<center>\n";
		
		if(!POST_UPLOADS){
			buffer += "<form name=\"conversion\" action=\"\" method=\"get\">\n";
		}else{
			buffer += "<form enctype=\"multipart/form-data\" name=\"conversion\" action=\"\" method=\"post\">\n";
		}
		
		buffer += "<table>\n";
		buffer += "<tr><td><td width=\"100\"><i><font size=\"-1\"><div id=\"inputs\"></div></font></i></td></tr>\n";
		
		if(!POST_UPLOADS){
			buffer += "<tr><td><b>File URL:</b></td><td><input type=\"text\" name=\"file\" id=\"file\" size=\"100\"></td></tr>\n";
		}else{
			buffer += "<tr><td><b>File:</b></td><td><input type=\"file\" name=\"file\" id=\"file\" size=\"100\"></td></tr>\n";
		}
		
		buffer += "<tr><td><b>Output:</b></td>\n";
		buffer += "<td><select name=\"output\" id=\"output\" onchange=\"setInputs();\">\n";
		
		for(Iterator<String> itr=polyglot.getOutputs().iterator(); itr.hasNext();){
			output = itr.next();
			buffer += "<option value=\"" + output + "\"";
			
			if(selected_output != null && selected_output.equals(output)){
				buffer += " selected";
			}
			
			buffer += ">" + output + "</option>\n";
		}
		
		buffer += "</select></td></tr>\n";		
		buffer += "<tr><td></td><td><input type=\"submit\" value=\"Submit\"></td></tr>\n";
		buffer += "<tr><td height=\"25\"></td><td></td></tr>\n";
		buffer += "<tr><td></td><td align=\"center\"><div name=\"api\" id=\"api\"></div></td></tr>\n";
		buffer += "</table>\n";
		buffer += "</form>\n";
		buffer += "</center>\n";
		buffer += "\n";		
		buffer += "<script type=\"text/javascript\">setInputs();</script>\n";
		buffer += "<script type=\"text/javascript\">setAPICall();</script>\n";
		
		return buffer;
	}
	
	/**
	 * Get a web form interface for this restful service.
	 * @return the form
	 */
	public String getForm()
	{
		return getForm(false, null);
	}	
	
	/**
	 * Handle HTTP GET requests.
	 */
	@Get
	public Representation httpGetHandler()
	{
		Vector<String> parts = Utility.split(getReference().getRemainingPart(), '/', true);
		String part0 = (parts.size() > 0) ? parts.get(0) : "";
		String part1 = (parts.size() > 1) ? parts.get(1) : "";
		String part2 = (parts.size() > 2) ? parts.get(2) : "";
		String file = null, output = null, result_file, result_url, url, type;
		String buffer;
		Form form;
		Parameter p;
		
		if(part0.equals("convert")){
			if(part1.isEmpty()){
				if(SoftwareServerRestlet.isPlainRequest(Request.getCurrent())){
					return new StringRepresentation(toString(polyglot.getOutputs()), MediaType.TEXT_PLAIN);
				}else{
					return new StringRepresentation(SoftwareServerRestlet.createHTMLList(toString(polyglot.getOutputs()), Utility.endSlash(getReference().toString()), true, "Outputs"), MediaType.TEXT_HTML);
				}
			}else{
				if(part2.isEmpty()){
					return new StringRepresentation("error: invalid endpoint", MediaType.TEXT_PLAIN);
				}else{
					output = part1;
					file = URLDecoder.decode(part2);
					
					System.out.print("[" + getClientInfo().getAddress() + "]: " + Utility.getFilename(file) + " -> " + output + "...");
					
					Utility.downloadFile(temp_path, file);
					polyglot.convert(temp_path + Utility.getFilename(file), public_path, output);
					result_file = Utility.getFilenameName(file) + "." + output;
					result_url = Utility.endSlash(getReference().getBaseRef().toString()) + "file/" + result_file;

					if(Utility.existsAndNotEmpty(public_path + result_file)){
						System.out.println(" [Success]");
					}else{
						System.out.println(" [Failed]");
					}
					
					if(SoftwareServerRestlet.isPlainRequest(Request.getCurrent())){
						return new StringRepresentation(result_url, MediaType.TEXT_PLAIN);
					}else{
						return new StringRepresentation("<a href=" + result_url + ">" + result_url + "</a>", MediaType.TEXT_HTML);
					}
				}
			}
		}else if(part0.equals("form")){
			if(part1.isEmpty()){
				buffer = "";
				buffer += "get\n";
				buffer += "post\n";
				
				if(SoftwareServerRestlet.isPlainRequest(Request.getCurrent())){
					return new StringRepresentation(buffer, MediaType.TEXT_PLAIN);
				}else{
					return new StringRepresentation(SoftwareServerRestlet.createHTMLList(buffer, Utility.endSlash(getReference().toString()), true, "Forms"), MediaType.TEXT_HTML);
				}
			}else{
				form = getRequest().getResourceRef().getQueryAsForm();
				p = form.getFirst("file"); if(p != null) file = p.getValue();
				p = form.getFirst("output"); if(p != null) output = p.getValue();
								
				if(file != null && output != null){
					url = getRootRef() + "convert/" + output + "/" + URLEncoder.encode(file);
	
					return new StringRepresentation("<html><head><meta http-equiv=\"refresh\" content=\"1; url=" + url + "\"></head</html>", MediaType.TEXT_HTML);
				}else{
					if(part1.startsWith("get")){
						return new StringRepresentation(getForm(false, output), MediaType.TEXT_HTML);
					}else if(part1.startsWith("post")){
						return new StringRepresentation(getForm(true, output), MediaType.TEXT_HTML);
					}else{
						return new StringRepresentation("error: invalid endpoint", MediaType.TEXT_PLAIN);
					}
				}
			}
		}else if(part0.equals("file")){
			if(!part1.isEmpty()){	
				file = public_path + part1;
				
				if(Utility.exists(file)){
					return new FileRepresentation(file, MediaType.MULTIPART_ALL);
				}else{
					setStatus(Status.CLIENT_ERROR_NOT_FOUND);
					return new StringRepresentation("File doesn't exist", MediaType.TEXT_PLAIN);
				}
			}else{
				return new StringRepresentation("error: invalid endpoint", MediaType.TEXT_PLAIN);
			}
		}else if(part0.equals("image")){
			if(!part1.isEmpty()){	
				file = "images/" + Utility.getFilename(Utility.unixPath(part1));
				
				if(Utility.exists(file)){
					type = Utility.getFilenameExtension(file);
					
					if(type.equals("png")){
						return new FileRepresentation(file, MediaType.IMAGE_PNG);
					}else if(type.equals("gif")){
						return new FileRepresentation(file, MediaType.IMAGE_GIF);
					}else{
						return new FileRepresentation(file, MediaType.IMAGE_JPEG);
					}
				}else{
					return new StringRepresentation("Image doesn't exist", MediaType.TEXT_PLAIN);
				}
			}else{
				return new StringRepresentation("error: invalid endpoint", MediaType.TEXT_PLAIN);
			}
		}else if(part0.equals("servers")){				
			return new StringRepresentation(toString(polyglot.getServers()), MediaType.TEXT_PLAIN);
		}else if(part0.equals("alive")){
			return new StringRepresentation("yes", MediaType.TEXT_PLAIN);
		}else{
			return new StringRepresentation("error: invalid endpoint", MediaType.TEXT_PLAIN);
		}
	}
	
	/**
	 * Handle HTTP POST requests.
	 * @param entity the entity
	 */
	@Post
	public Representation httpPostHandler(Representation entity)
	{
		Vector<String> parts = Utility.split(getReference().getRemainingPart(), '/', true);
		String part0 = (parts.size() > 0) ? parts.get(0) : "";
		String part1 = (parts.size() > 1) ? parts.get(1) : "";
		String part2 = (parts.size() > 2) ? parts.get(2) : "";
		boolean FORM_POST = !part0.isEmpty() && part0.equals("form");
		boolean TASK_POST = !part1.isEmpty();
		TreeMap<String,String> parameters = new TreeMap<String,String>();
		String file=null, output = null, result_file, result_url;
		
		if(FORM_POST || TASK_POST){
			if(MediaType.MULTIPART_FORM_DATA.equals(entity.getMediaType(), true)){
				DiskFileItemFactory factory = new DiskFileItemFactory();
				factory.setSizeThreshold(1000240);
				RestletFileUpload upload = new RestletFileUpload(factory);
				List<FileItem> items;
				FileItem fi;
				
				try{
					items = upload.parseRequest(getRequest());
					
					for(Iterator<FileItem> itr = items.iterator(); itr.hasNext();){
						fi = itr.next();
						
						if(fi.getName() == null){
							parameters.put(fi.getFieldName(), new String(fi.get(), "UTF-8"));
						}else{
							file = temp_path + fi.getName();
							fi.write(new File(file));
						}
					}
				}catch(Exception e) {e.printStackTrace();}
			}
			
			if(FORM_POST){
				output = parameters.get("output");
			}else if(TASK_POST){
				output = part1;				
			}
			
			if(file != null && output != null){	
				System.out.print("[" + getClientInfo().getAddress() + "]: " + Utility.getFilename(file) + " -> " + output + "...");
				
				polyglot.convert(file, public_path, output);
				result_file = Utility.getFilenameName(file) + "." + output;
				result_url = Utility.endSlash(getReference().getBaseRef().toString()) + "file/" + result_file;

				if(Utility.existsAndNotEmpty(public_path + result_file)){
					System.out.println(" [Success]");
				}else{
					System.out.println(" [Failed]");
				}
				
				if(RETURN_URL){		//Return URL of file
					if(SoftwareServerRestlet.isPlainRequest(Request.getCurrent())){
						return new StringRepresentation(result_url, MediaType.TEXT_PLAIN);
					}else{
						return new StringRepresentation("<a href=" + result_url + ">" + result_url + "</a>", MediaType.TEXT_HTML);
					}
				}else{						//Return the file
					result_file = public_path + result_file;
					
					if(Utility.exists(result_file)){
						return new FileRepresentation(result_file, MediaType.MULTIPART_ALL);
					}else{
						setStatus(Status.CLIENT_ERROR_NOT_FOUND);
						return new StringRepresentation("File doesn't exist", MediaType.TEXT_PLAIN);
					}
				}
			}
		}
		
		return httpGetHandler();
	}
	
	/**
	 * Stop the REST interface.
	 */
	public void stop()
	{		
		try{
			component.stop();
		}catch(Exception e) {e.printStackTrace();}
		
		polyglot.stop();
	}
	
	/**
	 * Convert a Collection of strings to a line separated list of strings.
	 * @param strings a Collection of strings
	 * @return the resulting string representation
	 */
	public static String toString(Collection<String> strings)
	{
		String string = "";
		
		for(Iterator<String> itr=strings.iterator(); itr.hasNext();){
			string += itr.next() + "\n";
		}
		
		return string;
	}
	
	/**
	 * Start the Polyglot REST interface.
	 * @param args command line arguments
	 */
	public static void main(String[] args)
	{		
		//Load configuration file
	  try{
	    BufferedReader ins = new BufferedReader(new FileReader("PolyglotRestlet.conf"));
	    String line, key, value;
	    String server;
	    int tmpi;
	    
	    while((line=ins.readLine()) != null){
	      if(line.contains("=")){
	        key = line.substring(0, line.indexOf('='));
	        value = line.substring(line.indexOf('=')+1);
	        
	        if(key.charAt(0) != '#'){
	        	if(key.equals("RootPath")){
	        		//root_path = value + "/";
	        		root_path = Utility.unixPath(Utility.absolutePath(value)) + "/";
	        		
	        		if(!Utility.exists(root_path)){
	        			System.out.println("Root path doesn't exist!");
	        			System.exit(1);
	        		}
	        		
	        		tmpi = 0;
	        		
	        		while(Utility.exists(root_path + "Temp" + Utility.toString(tmpi,3))){
	        			tmpi++;
	        		}
	        		
	        		temp_path = root_path + "Temp" + Utility.toString(tmpi,3) + "/";
	        		new File(temp_path).mkdir();
	        		tmpi = 0;
	        		
	        		while(Utility.exists(root_path + "Public" + Utility.toString(tmpi,3))){
	        			tmpi++;
	        		}
	        		
	        		public_path = root_path + "Public" + Utility.toString(tmpi,3) + "/";
	        		new File(public_path).mkdir();
	        	}else if(key.equals("Port")){
	        		port = Integer.valueOf(value);
	          }else if(key.equals("StewardPort")){
	          	steward_port = Integer.valueOf(value);
	          	
	        	  if(steward_port >= 0){
	        	  	polyglot.listen(steward_port);
	        	  }
	          }else if(key.equals("SoftwareServer")){
          		tmpi = value.lastIndexOf(':');
	        		
	        		if(tmpi != -1){
	        			server = value.substring(0, tmpi);
	        			port = Integer.valueOf(value.substring(tmpi+1));
	        			polyglot.add(server, port);
	        		}
	          }else if(key.equals("ReturnURL")){
	          	RETURN_URL = Boolean.valueOf(value);
	          }
	        }
	      }
	    }
	    
	    ins.close();
	  }catch(Exception e) {e.printStackTrace();}
		
	  //Start the service		
		try{			
			component = new Component();
			component.getServers().add(Protocol.HTTP, port);
			component.getClients().add(Protocol.HTTP);
			component.getLogService().setEnabled(false);
			
			org.restlet.Application application = new org.restlet.Application(){
				@Override
				public Restlet createInboundRoot(){
					Router router = new Router(getContext());
					router.attachDefault(PolyglotRestlet.class);
					return router;
				}
			};
			
			component.getDefaultHost().attach("/", application);
			component.start();
		}catch(Exception e) {e.printStackTrace();}
  	
		System.out.println("\nPolyglot restlet is running...\n");
	}
}
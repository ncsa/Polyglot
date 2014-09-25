package edu.illinois.ncsa.isda.softwareserver.polyglot;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import org.apache.commons.io.FilenameUtils;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import javax.ws.rs.core.MultivaluedMap;
import org.json.JSONArray;
import kgm.utility.Utility;
import edu.illinois.ncsa.isda.softwareserver.ResteasyResources;
import edu.illinois.ncsa.isda.softwareserver.polyglot.PolyglotAuxiliary.RequestInformation;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.TreeMap;
import java.util.Map;
import java.util.List;
import java.io.InputStream;
import edu.illinois.ncsa.isda.softwareserver.SoftwareServerAuxiliary.Application;


public class PolyglotResteasyResources extends PolyglotResteasy implements PolyglotResteasyResourcesInterface
{
	private static PolyglotSteward polyglot = new PolyglotSteward();
	private static int port = 8184;
	private static String root_path = "./";
	private static String temp_path = root_path + "Temp";
	private static String public_path = root_path + "Public";
	private static int steward_port = -1;
	private static boolean RETURN_URL = false;
	private static boolean MONGO_LOGGING = false;
	private static int mongo_update_interval = 300;
	private static boolean SOFTWARE_SERVER_REST_INTERFACE = false;
	private static String download_method = "";
	private static TreeMap<String,Application> alias_map = new TreeMap<String,Application>();
	

	//Logs
	private static long start_time;
  private static ArrayList<RequestInformation> requests = new ArrayList<RequestInformation>();
  private static MongoClient mongo;
	private static DB db;


	
	public PolyglotResteasyResources()
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
	          }else if(key.equals("MongoLogging")){
	          	MONGO_LOGGING = Boolean.valueOf(value);
	          }else if(key.equals("MongoUpdateInterval")){
	          	mongo_update_interval = Integer.valueOf(value) * 1000;
	          }else if(key.equals("SoftwareServerRESTInterface")){
	          	SOFTWARE_SERVER_REST_INTERFACE = Boolean.valueOf(value);
	          }else if(key.equals("SoftwareServerRESTPort")){
							polyglot.setSoftwareServerRESTPort(Integer.valueOf(value));
	          }else if(key.equals("DownloadMethod")){
							download_method = value;
	          }else if(key.equals("MaxTaskTime")){
							polyglot.setMaxTaskTime(Integer.valueOf(value));
	          }
	        }
	      }
	    }
	    
	    ins.close();
	  }catch(Exception e) {e.printStackTrace();}

	 	if(MONGO_LOGGING){
			//Setup database connection
			try{
	    	Properties properties = new Properties();
	    	properties.load(new FileInputStream("mongo.properties"));
	    	mongo = new MongoClient(properties.getProperty("server"));
	    	db = mongo.getDB(properties.getProperty("database"));
	    	//db.authenticate(properties.getProperty("username"), properties.getProperty("password").toCharArray());
	    	//DBCollection collection = db.getCollection(properties.getProperty("collection"));
	    	db.getLastError();		//Test the connection, will cause an exception if not connected.
			}catch(Exception e){
				System.out.println("\nMongo database not found, disabiling Mongo logging...");
				MONGO_LOGGING = false;
			}

			if(MONGO_LOGGING){
	 			System.out.println("\nStarting Mongo information update thread...");

	  		new Thread(){
	  			public void run(){		  			
	  				while(true){
	  					updateMongo();
	  					Utility.pause(mongo_update_interval);
	  				}
	  			}
	  		}.start();
			}
	 	}
		
	  //Start the service
	  start_time = System.currentTimeMillis();
		System.out.println("\nPolyglot resteasy is running...");
	} // end of Constructor //
	
	
	public Response Endpoints(UriInfo uriInfo, String accept)
	{
		String buffer = "";
		buffer += "convert\n";
		buffer += "form\n";
		buffer += "alive\n";
		buffer += "servers\n";
		buffer += "software\n";
		buffer += "inputs\n";
		buffer += "outputs\n";
		buffer += "requests\n";
		if(accept.equals("text/plain")){
			return Response.ok(buffer).build();
		} else {
			String result = ResteasyResources.createHTMLList(buffer, uriInfo.getAbsolutePath().toString().replaceAll("/$", "")+ "/", true, "Endpoints");
			return Response.ok(result, "text/html").build();		
		}
	} // end of Endpoints() //

	public Response Convert(UriInfo uriInfo, String accept)
	{
    String temp =toString(polyglot.getOutputs()); 		
		if(accept.equals("text/plain")){
			if (  temp.equals("")) {
  			return Response.noContent().build();
			} else {
				return Response.ok(temp, accept).build();
			}
		} else {
			String result = ResteasyResources.createHTMLList(temp, uriInfo.getAbsolutePath().toString().replaceAll("/$", "")+ "/", true, "Outputs");
			return Response.ok(result, "text/html").build();		
		}
	} // end of Convert() //
	
	public Response ConvertFmt(String outFmt)
	{		
		return Response.ok(toString(polyglot.getInputs(outFmt))).build();
	} // end of ConvertFmt() //

	public Response ConvertFile(UriInfo uriInfo, String accept, HttpServletRequest req,  String outFmt, String file) throws IOException
	{		
		RequestInformation request;
		String client = req.getRemoteAddr();
		String  result_file, result_url;
		String media_type = null;
		
		try{
			file = URLDecoder.decode(file, "UTF-8");
		}catch(UnsupportedEncodingException e){
			e.printStackTrace();
		}	
		
		
		System.out.print("[" + client + "]: " + Utility.getFilename(file) + " -> " + outFmt + "...");
		
		if(download_method.equals("wget")){
			try{
				String[] command = {"wget", "-O", temp_path +  Utility.getFilenameName(file) + "." + ResteasyResources.removeParameters(Utility.getFilenameExtension(file)),  file  };
				Runtime.getRuntime().exec(command).waitFor();
			}catch(Exception e){e.printStackTrace();}
		}else if(download_method.equals("nio")){
			try{
				URL website = new URL(file);
				ReadableByteChannel rbc = Channels.newChannel(website.openStream());
				FileOutputStream fos = new FileOutputStream(temp_path + "/" + Utility.getFilenameName(file) + "." + ResteasyResources.removeParameters(Utility.getFilenameExtension(file)));
				fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
				fos.close();
			}catch(Exception e){e.printStackTrace();}
		}else{
			//Utility.downloadFile(temp_path, file);
			Utility.downloadFile(temp_path, Utility.getFilenameName(file) + "." + ResteasyResources.removeParameters(Utility.getFilenameExtension(file)), file);
		}
		
		//file = temp_path + Utility.getFilename(file);
		file = temp_path + ResteasyResources.removeParameters(Utility.getFilename(file));
		
		request = new RequestInformation(client, file, outFmt);
		if(SOFTWARE_SERVER_REST_INTERFACE){
			polyglot.convertOverREST(file, public_path, outFmt);
		}else{
			polyglot.convert(file, public_path, outFmt);
		}
		

		result_file = Utility.getFilenameName(file) + "." + outFmt;
		result_url = uriInfo.getBaseUri().toString() + "file/" + result_file;

		if(Utility.existsAndNotEmpty(public_path + result_file)){
			request.setEndOfRequest(true);
			System.out.println(" [Success]");
		}else{
			request.setEndOfRequest(false);
			System.out.println(" [Failed]");
		}
		
		requests.add(request);
		if(MONGO_LOGGING) updateMongo(request);
		
	
		
		if(RETURN_URL){		//Return URL of file
			if( accept.equals("text/plain") ){
				return Response.ok(result_url,"text/plain").build();
			}else{
				return Response.ok("<a href=" + result_url + ">" + result_url + "</a>","text/html").build();
			}
		}else{						//Return the file
			result_file = public_path + result_file;
			File fileOrDir = new File(result_file);
			
			Path path = Paths.get(result_file);
			media_type = Files.probeContentType(path);
			if (Files.size(path) > 0 ) {
				if(media_type != null){
					return Response.ok(fileOrDir, media_type).build();
				}else{
					return Response.ok(fileOrDir).build();
				}
			} else {
				return Response.status(Status.NOT_FOUND).entity("File doesn't exist").type("text/plain").build();
				//return Response.status(Status.NO_CONTENT).type(media_type).build();
			}
		}
	} // end of ConvertFile() //
		

	
	
	public Response Form(UriInfo uriInfo, String accept)
	{
		String buffer = "get\npost\n";
		if(accept.equals("text/plain")){
			return Response.ok(buffer, "text/plain").build();
		} else {
			String result = ResteasyResources.createHTMLList(buffer, uriInfo.getAbsolutePath().toString().replaceAll("/$", "")+ "/", true, "Forms");
			return Response.ok(result, "text/html").build();		
		}
	} // end of Form() //

	
	public Response Forms(UriInfo uri, String output, String file, String action)
	{
		String url = null;
		if(output.equals("")) output = null;
		if(file.equals("")) file = null;

		if( file != null && output != null){
				try{
					url = uri.getBaseUri().toString() + "convert/" + output + "/" + URLEncoder.encode(file, "UTF-8");
				}catch(UnsupportedEncodingException e){
					e.printStackTrace();
				}				
			return Response.ok(
					"<html><head><meta http-equiv=\"refresh\" content=\"1; url=" + url
							+ "\"></head></html>").build();
		}else{
			if(action.equals("get")){
				return Response.ok(getForm(false, output)).build();
			}else if(action.equals("post")){
				return Response.ok(getForm(true, output)).build();
			}else{
				return printErrorMessage(action);
			}
		}
	} // end of Forms() //

	
	public Response File(UriInfo uri, String fileName) throws IOException {
		String media_type = null;
		File file = new File(public_path + fileName);
		Path path = Paths.get(public_path + fileName);
		
		media_type = Files.probeContentType(path);
		if(file.exists()){
		
			if (Files.size(path) > 0 ) {
				// uncomment the if statement to directly open the file in browser for valid media types
				//if(media_type != null){
				//	return Response.ok(file, media_type).build();
				//}else{
					return Response.ok(file).build();
				//}
			} else {
				return Response.status(Status.NO_CONTENT).type(media_type).build();
			}
		} else {
			return Response.status(Status.NOT_FOUND).entity("File doesn't exist").type("text/plain").build();
		}
	} // end of File() //
	
	public Response appIcons(String fileName)
	{
		// String imgDefault = "images/polyglot.png";
		String imgDefault = "images/browndog-small.gif";
		String file = FilenameUtils.removeExtension(fileName);
		if(alias_map.containsKey(file)){
			file = alias_map.get(file).icon;
			if(file != null) return Response.ok((Object)new File(file)).build();
		}
		return Response.ok((Object)new File(imgDefault)).build();
	} // end of appIcons() //

	
	
	public Response Alive()
	{		
	  return Response.ok("yes").build();		
	} // end of Alive() //
	
	public Response Servers()
	{		
	  return Response.ok(toString(polyglot.getServers())).build();		
	} // end of Servers() //
	
	public Response Software()
	{		
	  return Response.ok(toString(polyglot.getSoftware())).build();		
	} // end of Software() //


	public Response Inputs(UriInfo uriInfo, String accept)
	{
		if(accept.equals("text/plain")){
			return Response.ok(toString(polyglot.getInputs()), accept).build();
		} else {
			String result = ResteasyResources.createHTMLList(toString(polyglot.getInputs()), uriInfo.getAbsolutePath().toString().replaceAll("/$", "")+ "/", true, "Inputs");
			return Response.ok(result, "text/html").build();		
		}
	} // end of Inputs() //

	public Response InputsOut(UriInfo uriInfo, String accept, String out)
	{
		
    if(accept !=null && accept.equals("application/json"))  {
			return Response.ok(new JSONArray(polyglot.getOutputs(out)).toString()).build();
		} else {
			return Response.ok(toString(polyglot.getOutputs(out)), "text/plain").build();
		}
	
	} // end of InputsOut() //
	
	public Response Outputs()
	{		
	  return Response.ok(toString(polyglot.getOutputs())).build();		
	} // end of Outputs() //
	

	public Response Requests()
	{		
		String buffer;
		buffer = Long.toString(start_time) + "\n";
		buffer += System.currentTimeMillis() + "\n";
		
		for(Iterator<RequestInformation> itr=requests.iterator(); itr.hasNext();){
			buffer += itr.next().toString() + "\n";
		}
		return Response.ok(buffer).build();
	} // end of Requests() //

	
	public Response printErrorMessage(String msg)
	{
		String result = "PolyglotResteasy message : " + msg
				+ " is an invalid endpoint.";
		return Response.status(Status.BAD_REQUEST).entity(result).type("text/plain").build();
	} // end of printErrorMessage() //

	
	public Response formPost(MultipartFormDataInput input, String mediaType,
			String accept, UriInfo uri, HttpServletRequest req, String  output) 	{
		return processPost(input, mediaType, accept, uri,req, output, true, false);
	}

	public Response taskPost(MultipartFormDataInput input, String mediaType,String accept, UriInfo uri, HttpServletRequest req, String output) 

	{
		return processPost (input, mediaType, accept, uri, req, output,false,true);
	}

	
	
	public Response processPost(MultipartFormDataInput input, String mediaType,
			String accept, UriInfo uri,  HttpServletRequest req, String output, boolean formPost, boolean taskPost) 
	{
		String client, file=null, result_file, result_url;
		String searchFor = null;
		String fileName;
		RequestInformation request;


		if(output.equals("")) output = null;


		Map<String,List<InputPart>> formParts = input.getFormDataMap();
		TreeMap<String,String> parameters = new TreeMap<String,String>();

		if(mediaType.startsWith("multipart/form-data")){
			for(Map.Entry<String,List<InputPart>> entry:formParts.entrySet()){
				searchFor = entry.getKey();
				List<InputPart> inPart = formParts.get(searchFor);
				for(InputPart inputPart:inPart){
					MultivaluedMap<String,String> headers = inputPart.getHeaders();
					String[] contentDispositionHeader = headers.getFirst("Content-Disposition").split(";");
					if(contentDispositionHeader[1].equals(" name=\"file\"")){
						fileName = contentDispositionHeader[2].substring(11,contentDispositionHeader[2].length() - 1);
						file = temp_path + fileName;
						// Handle the body of that part with an InputStream
						try{
							InputStream istream = inputPart.getBody(InputStream.class, null);
							saveFile(istream, file);
						}catch(IOException e){
							e.printStackTrace();
						}
					}else{
						try{
							String body = inputPart.getBodyAsString();
							parameters.put(searchFor, body);
						}catch(IOException e){
							e.printStackTrace();
						}
					}
				}
			}
		}

		if(formPost){
			output = parameters.get("output");
		}

		
		//Do the conversion
		if(file != null && output != null){	
			client = req.getRemoteAddr();

			System.out.print("[" + client + "]: " + Utility.getFilename(file) + " -> " + output + "...");
			
			request = new RequestInformation(client, file, output);

			if(SOFTWARE_SERVER_REST_INTERFACE){
				polyglot.convertOverREST(file, public_path, output);
			}else{
				polyglot.convert(file, public_path, output);
			}

			result_file = Utility.getFilenameName(file) + "." + output;
			//result_url = Utility.endSlash(getReference().getBaseRef().toString()) + "file/" + result_file;
			result_url = uri.getBaseUri().toString() + "file/" + result_file;
			if(Utility.existsAndNotEmpty(public_path + result_file)){
				request.setEndOfRequest(true);
				System.out.println(" [Success]");
			}else{
				request.setEndOfRequest(false);
				System.out.println(" [Failed]");
			}
			
			requests.add(request);
			if(MONGO_LOGGING) updateMongo(request);
			
			if(RETURN_URL){		//Return URL of file
				if( accept.equals("text/plain") ){
					return Response.ok(result_url,"text/plain").build();
					//return new StringRepresentation(result_url, MediaType.TEXT_PLAIN);
				}else{
					return Response.ok("<a href=" + result_url + ">" + result_url + "</a>","text/html").build();
				}
			}else{						//Return the file
				result_file = public_path + result_file;
				File myFile = new File(result_file);
				Path path = Paths.get(result_file);
				String media_type;
				try{
					media_type = Files.probeContentType(path);
					if (Files.size(path) > 0 ) {
						if(media_type != null){
							return Response.ok(myFile, media_type).build();
						}else{
							return Response.ok(myFile).build();
						}
					}else{
						return Response.status(Status.NOT_FOUND).entity("File doesn't exist").type("text/plain").build();
					}					
				}catch(IOException e){
					e.printStackTrace();
				}
			}
		}
		return null;
	}
	

	private void saveFile(InputStream uploadedInputStream, String serverLocation)
	{
		try{
			OutputStream outpuStream = new FileOutputStream(new File(serverLocation));
			int read = 0;
			byte[] bytes = new byte[1024];

			outpuStream = new FileOutputStream(new File(serverLocation));
			while((read = uploadedInputStream.read(bytes)) != -1){
				outpuStream.write(bytes, 0, read);
			}
			outpuStream.flush();
			outpuStream.close();
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
	
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++


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

	if(!POST_UPLOADS){
		buffer += "<tr><td><b>File URL:</b></td><td><input type=\"text\" name=\"file\" id=\"file\" size=\"100\"></td></tr>\n";
	}else{
		buffer += "<tr><td><b>File:</b></td><td><input type=\"file\" name=\"file\" id=\"file\" size=\"100\"></td></tr>\n";
	}
	
	buffer += "<tr><td></td><td width=\"625\"><i><font size=\"-1\"><div id=\"inputs\"></div></font></i></td></tr>\n";
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
 * Push logged information to mongo.
 */
public static void updateMongo()
{
  DBCollection collection;
	
	//Update active software servers
	collection = db.getCollection("servers");
	collection.drop();

	for(Iterator<String> itr=polyglot.getServers().iterator(); itr.hasNext();){
   	BasicDBObject document = new BasicDBObject("host", itr.next());
   	collection.insert(document);
  }

	//Update available software
	collection = db.getCollection("software");
	collection.drop();

	for(Iterator<String> itr=polyglot.getSoftware().iterator(); itr.hasNext();){
   	BasicDBObject document = new BasicDBObject("name", itr.next());
   	collection.insert(document);
  }

	//Update allowed inputs	
	collection = db.getCollection("inputs");
	collection.drop();

	for(Iterator<String> itr=polyglot.getInputs().iterator(); itr.hasNext();){
   	BasicDBObject document = new BasicDBObject("extension", itr.next());
   	collection.insert(document);
  }

	//Update allowed outputs
	collection = db.getCollection("outputs");
	collection.drop();

	for(Iterator<String> itr=polyglot.getOutputs().iterator(); itr.hasNext();){
   	BasicDBObject document = new BasicDBObject("extension", itr.next());
   	collection.insert(document);
  }
}

/**
 * Push request information to mongo.
 */
public static void updateMongo(RequestInformation request)
{
  DBCollection collection = db.getCollection("requests");
	
	BasicDBObject document = new BasicDBObject();
	document.append("address", request.address);
	document.append("filename", request.filename);
	document.append("filesize", request.filesize);
	document.append("input", request.input);
	document.append("output", request.output);
	document.append("start_time", request.start_time);
	document.append("end_time", request.end_time);
	document.append("success", request.success);
	collection.insert(document);
}


} // end of class PolyglotResteasyResources //

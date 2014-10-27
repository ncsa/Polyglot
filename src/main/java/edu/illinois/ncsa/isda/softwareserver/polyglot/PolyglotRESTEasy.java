package edu.illinois.ncsa.isda.softwareserver.polyglot;
import edu.illinois.ncsa.isda.softwareserver.polyglot.PolyglotAuxiliary.*;
import edu.illinois.ncsa.isda.softwareserver.SoftwareServerRESTUtilities;
import edu.illinois.ncsa.isda.softwareserver.SoftwareServerAuxiliary.Application;
import kgm.utility.Utility;
import java.io.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.util.*;
import java.net.*;
import org.apache.commons.io.FilenameUtils;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.server.tjws.TJWSEmbeddedJaxrsServer;
import org.json.JSONArray;
import com.mongodb.*;

/**
 * A restful interface for a polyglot steward.
 * Think of this as an extended polyglot steward.
 * @author Edgar Black
 */
public class PolyglotRESTEasy implements PolyglotRESTEasyInterface
{
	protected static TJWSEmbeddedJaxrsServer tjws;
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
		
	/**
	 * Class constructor.
	 */
	public PolyglotRESTEasy()
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
	  					PolyglotRESTUtilities.updateMongo(db, polyglot);
	  					Utility.pause(mongo_update_interval);
	  				}
	  			}
	  		}.start();
			}
	 	}
		
	  //Start the service
	  start_time = System.currentTimeMillis();
		System.out.println("\nPolyglot resteasy is running...");
	}
	
	/**
	 * Used to list the of endpoints in the polyglot server
	 * @param uriInfo Basic URL information
	 * @param accept content type accepted by client
	 * @return list of endpoints in the polyglot server
	 */
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
			String result = SoftwareServerRESTUtilities.createHTMLList(buffer, uriInfo.getAbsolutePath().toString().replaceAll("/$", "")+ "/", true, "Endpoints");
			return Response.ok(result, "text/html").build();		
		}
	}

	/**
	 * Used to list all the output formats produced by the polyglot server
	 * @param uriInfo Basic URL information
	 * @param accept content type accepted by client
	 * @return list all the output formats produced by the polyglot server
	 */
	public Response Convert(UriInfo uriInfo, String accept)
	{
    String temp =PolyglotRESTUtilities.toString(polyglot.getOutputs());
    
		if(accept.equals("text/plain")){
			if (  temp.equals("")) {
  			return Response.noContent().build();
			} else {
				return Response.ok(temp, accept).build();
			}
		} else {
			String result = SoftwareServerRESTUtilities.createHTMLList(temp, uriInfo.getAbsolutePath().toString().replaceAll("/$", "")+ "/", true, "Outputs");
			return Response.ok(result, "text/html").build();		
		}
	}
	
	/**
	 * Used to obtain a list the input formats associated with the output format
	 * @param outFmt the output format
	 * @return list the input formats associated with the output format
	 */
	public Response ConvertFmt(String outFmt)
	{		
		return Response.ok(PolyglotRESTUtilities.toString(polyglot.getInputs(outFmt))).build();
	}

	/**
	 * Used to post a conversion request to the softwareserver
	 * @param uriInfo Basic URL information
	 * @param accept content type accepted by client
	 * @param req used to obtain the ip address of the client
	 * @param outFmt the output format
	 * @param file input file to be processed 
	 * @return the produced file
	 */
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
				String[] command = {"wget", "-O", temp_path +  Utility.getFilenameName(file) + "." + SoftwareServerRESTUtilities.removeParameters(Utility.getFilenameExtension(file)),  file  };
				Runtime.getRuntime().exec(command).waitFor();
			}catch(Exception e){e.printStackTrace();}
		}else if(download_method.equals("nio")){
			try{
				URL website = new URL(file);
				ReadableByteChannel rbc = Channels.newChannel(website.openStream());
				FileOutputStream fos = new FileOutputStream(temp_path + "/" + Utility.getFilenameName(file) + "." + SoftwareServerRESTUtilities.removeParameters(Utility.getFilenameExtension(file)));
				fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
				fos.close();
			}catch(Exception e){e.printStackTrace();}
		}else{
			//Utility.downloadFile(temp_path, file);
			Utility.downloadFile(temp_path, Utility.getFilenameName(file) + "." + SoftwareServerRESTUtilities.removeParameters(Utility.getFilenameExtension(file)), file);
		}
		
		//file = temp_path + Utility.getFilename(file);
		file = temp_path + SoftwareServerRESTUtilities.removeParameters(Utility.getFilename(file));
		
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
		if(MONGO_LOGGING) PolyglotRESTUtilities.updateMongo(db,request);
		
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
	}
	
	/**
	 * Used to obtain a list of actions that can be performed (get, post)
	 * @param uriInfo Basic URL information
	 * @param accept content type accepted by client
	 * @return list of actions that can be performed
	 */
	public Response Form(UriInfo uriInfo, String accept)
	{
		String buffer = "get\npost\n";
		if(accept.equals("text/plain")){
			return Response.ok(buffer, "text/plain").build();
		} else {
			String result = SoftwareServerRESTUtilities.createHTMLList(buffer, uriInfo.getAbsolutePath().toString().replaceAll("/$", "")+ "/", true, "Forms");
			return Response.ok(result, "text/html").build();		
		}
	}
	
	/**
	 * Return a web form to interact with the Software Server
	 * @param uriInfo basic URL information
	 * @param output the default output format
	 * @param file input file to be processed 
	 * @param action either get or post
	 * @return a response containing either a link to the form or an error message
	 */
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
				return Response.ok(PolyglotRESTUtilities.getForm(polyglot,false, output)).build();
			}else if(action.equals("post")){
				return Response.ok(PolyglotRESTUtilities.getForm(polyglot,true, output)).build();
			}else{
				return printErrorMessage(action);
			}
		}
	}
	
	/**
	 * Used to download a file form polyglot server
	 * @param uri Basic URL information
	 * @param fileName file to be downloaded polyglot server
	 * @return file form polyglot server
	 */
	public Response File(UriInfo uri, String fileName) throws IOException
	{
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
	}
	
	/**
	 * Returns the requested icon file if available. If not, a default icon file is return instead.
	 * @param fileName file name of icon
	 * @return the requested icon file if available, default icon icon if not
	 */
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
	}
	
	/**
	 * Used to verify if the server is responding
	 * @return yes if the server is alive 
	 */
	public Response Alive()
	{		
	  return Response.ok("yes").build();		
	}
	
	/**
	 * Used to list the software servers connected to polyglot server
	 * @return list the software servers connected to polyglot server
	 */
	public Response Servers()
	{		
	  return Response.ok(PolyglotRESTUtilities.toString(polyglot.getServers())).build();		
	}
	
	/**
	 * Used to list the applications available in polyglot server
	 * @return list of applications available in software server
	 */
	public Response Software()
	{		
	  return Response.ok(PolyglotRESTUtilities.toString(polyglot.getSoftware())).build();		
	}

	/**
	 * Used to list all the input formats the polyglot server is able to serve
	 * @return list of input formats 
	 */
	public Response Inputs(UriInfo uriInfo, String accept)
	{
		if(accept.equals("text/plain")){
			return Response.ok(PolyglotRESTUtilities.toString(polyglot.getInputs()), accept).build();
		} else {
			String result = SoftwareServerRESTUtilities.createHTMLList(PolyglotRESTUtilities.toString(polyglot.getInputs()), uriInfo.getAbsolutePath().toString().replaceAll("/$", "")+ "/", true, "Inputs");
			return Response.ok(result, "text/html").build();		
		}
	}

	/**
	 * Used to list all the input formats that can be converted to output format
	 * @param uriInfo Basic URL information
	 * @param accept content type accepted by client
	 * @param out output format
	 * @return list all the input formats that can be converted to output format
	 */
	public Response InputsOut(UriInfo uriInfo, String accept, String out)
	{
    if(accept !=null && accept.equals("application/json")) {
			return Response.ok(new JSONArray(polyglot.getOutputs(out)).toString()).build();
		} else {
			return Response.ok(PolyglotRESTUtilities.toString(polyglot.getOutputs(out)), "text/plain").build();
		}
	}
	
	/**
	 * Used to list all the file formats the polyglot server is able to produce
	 * @return list of output formats 
	 */
	public Response Outputs()
	{		
	  return Response.ok(PolyglotRESTUtilities.toString(polyglot.getOutputs())).build();		
	}

	/**
	 * Used to obtain of a list of the processed task since startup
	 * @return list of the processed task since startup
	 */
	public Response Requests()
	{		
		String buffer;
		buffer = Long.toString(start_time) + "\n";
		buffer += System.currentTimeMillis() + "\n";
		
		for(Iterator<RequestInformation> itr=requests.iterator(); itr.hasNext();){
			buffer += itr.next().toString() + "\n";
		}
		return Response.ok(buffer).build();
	}
	
	/**
	 * Returns an error message 
	 * @param msg String to be returned within the error message
	 * @return error message
	 */
	public Response printErrorMessage(String msg)
	{
		String result = "PolyglotResteasy message : " + msg + " is an invalid endpoint.";
		return Response.status(Status.BAD_REQUEST).entity(result).type("text/plain").build();
	}
	
	/**
	 * Process a file submitted via the web form and return a link to the produced file
	 * @param input Form containing Input Data
	 * @param mediaType media type being processed 
	 * @param accept content type accepted by client
	 * @param uri Basic URL information
	 * @param req used to obtain the ip address of the client
	 * @param output the output format
	 * @return a link to the produced file
	 */
	public Response formPost(MultipartFormDataInput input, String mediaType, String accept, UriInfo uri, HttpServletRequest req, String  output)
	{
		return processPost(input, mediaType, accept, uri,req, output, true);
	}

	/**
	 * Process a file submitted to Polyglot and return a link to the produced file
	 * @param input Form containing Input Data
	 * @param mediaType media type being processed 
	 * @param accept content type accepted by client
	 * @param uri Basic URL information
	 * @param req used to obtain the ip address of the client
	 * @param output the output format
	 * @return a link to the produced file
	 */
	public Response taskPost(MultipartFormDataInput input, String mediaType, String accept, UriInfo uri, HttpServletRequest req, String output) 
	{
		return processPost(input, mediaType, accept, uri, req, output, false);
	}

	/**
	 * This is an auxiliary method working for the formPost() and the taskPost() methods. Returns the produced file
	 * @param input Form containing Input Data
	 * @param mediaType media type being processed 
	 * @param accept content type accepted by client
	 * @param uri Basic URL information
	 * @param req used to obtain the ip address of the client
	 * @param output the output format
	 * @param fromPost true if message comes from formPost; false otherwise
	 * @return the produced file
	 */
	public Response processPost(MultipartFormDataInput input, String mediaType, String accept, UriInfo uri,  HttpServletRequest req, String output, boolean formPost) 
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
			if(MONGO_LOGGING) PolyglotRESTUtilities.updateMongo(db,request);
			
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

	/**
	 * Save an uploaded file to the local file system.
	 * @param uploadedInputStream the uploaded file stream
	 * @param serverLocation the location to save the file
	 */
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
	
	/**
	 * Stop the REST interface.
	 */
	public void stop()
	{
		try{
			tjws.stop();
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	/**
	 * Start the Polyglot REST interface.
	 * @param args command line arguments
	 */
	public static void main(String[] args)
	{
		tjws = new TJWSEmbeddedJaxrsServer();
		tjws.setPort(8184);
		tjws.start();
		// tjws.getDeployment().getRegistry().addPerRequestResource(ResteasyResources.class);
		tjws.getDeployment().getRegistry().addSingletonResource(new PolyglotRESTEasy());
	}
}

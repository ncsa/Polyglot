package edu.illinois.ncsa.isda.softwareserver.polyglot;
import edu.illinois.ncsa.isda.softwareserver.*;
import edu.illinois.ncsa.isda.softwareserver.SoftwareServerRESTUtilities.*;
import edu.illinois.ncsa.isda.softwareserver.polyglot.PolyglotAuxiliary.*;
import kgm.utility.Utility;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.net.*;
import java.lang.*;
import java.util.*;
import javax.servlet.*;
import org.restlet.*;
import org.restlet.resource.*;
import org.restlet.data.*;
import org.restlet.representation.*;
import org.restlet.routing.*;
import org.restlet.security.*;
import org.restlet.ext.fileupload.*;
import org.restlet.ext.json.*;
import org.json.*;
import org.restlet.service.*;
import org.apache.commons.fileupload.*;
import org.apache.commons.fileupload.disk.*;
import com.mongodb.*;

/**
 * A restful interface for a polyglot steward.
 * Think of this as an extended polyglot steward.
 * @author Kenton McHenry
 */
public class PolyglotRestlet extends ServerResource
{
  private static Polyglot polyglot = new PolyglotStewardAMQ();
	private static int steward_port = -1;
	private static int port = 8184;
	private static boolean RETURN_URL = false;
	private static boolean MONGO_LOGGING = false;
	private static int mongo_update_interval = 300;
	private static boolean SOFTWARE_SERVER_REST_INTERFACE = false;
	private static String download_method = "";
	private static boolean HOST_POSTED_FILES = false;
	private static String root_path = "./";
	private static String temp_path = root_path + "Temp";
	private static String public_path = root_path + "Public";
	private static Component component;
	
	//Logs
	private static long start_time;
	private static ArrayList<RequestInformation> requests = new ArrayList<RequestInformation>();
  private static MongoClient mongo;
	private static DB db;
	
	/**
	 * Set whether or not to return a URL as the result (as opposed to the file itself).
	 * @param value true if the URL to the resulting file should be returned
	 */
	public void setReturnURL(boolean value)
	{
		RETURN_URL = value;
	}

	/**
	 * Set whether or not to access Software Servers via their REST interface.
	 * @param value true if a Software Servers REST interface should be used
	 */
	public void setSoftwareServerRESTInterface(boolean value)
	{
		SOFTWARE_SERVER_REST_INTERFACE = value;
	}
	
	/**
	 * Get a web form interface for this restful service.
	 * @param POST_UPLOADS true if this form should use POST rather than GET for uploading files
	 * @param selected_output the default output format
	 * @return the form
	 */
	public String getForm(boolean POST_UPLOADS, String selected_output)
	{
		return PolyglotRESTUtilities.getForm(polyglot, POST_UPLOADS, selected_output);
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
	 * @return a response
	 */
	@Get
	public Representation httpGetHandler()
	{
		RequestInformation request;
		Vector<String> parts = Utility.split(getReference().getRemainingPart(), '/', true);
		String part0 = (parts.size() > 0) ? parts.get(0) : "";
		String part1 = (parts.size() > 1) ? parts.get(1) : "";
		String part2 = (parts.size() > 2) ? parts.get(2) : "";
		String client, file = null, output = null, result_file = null, result_url, url, type;
		String buffer;
		Form form;
		Parameter p;
		
		if(part0.equals("convert")){
			if(part1.isEmpty()){
				if(SoftwareServerRestlet.isPlainRequest(Request.getCurrent())){
					return new StringRepresentation(PolyglotRESTUtilities.toString(polyglot.getOutputs()), MediaType.TEXT_PLAIN);
				}else{
					return new StringRepresentation(SoftwareServerRESTUtilities.createHTMLList(PolyglotRESTUtilities.toString(polyglot.getOutputs()), Utility.endSlash(getReference().toString()), true, "Outputs"), MediaType.TEXT_HTML);
				}
			}else{
				if(part2.isEmpty()){
					return new StringRepresentation(PolyglotRESTUtilities.toString(polyglot.getInputs(part1)), MediaType.TEXT_PLAIN);
				}else{
					client = getClientInfo().getAddress();
					output = part1;
					file = URLDecoder.decode(part2);
					
					//Do the conversion
					System.out.print("[" + client + "]: " + Utility.getFilename(file) + " -> " + output + "...");
					
					//Download URLs
					if(!(polyglot instanceof PolyglotStewardAMQ)){
						if(download_method.equals("wget")){
							try{
								Runtime.getRuntime().exec("wget -O " + temp_path + "/" + Utility.getFilenameName(file) + "." + SoftwareServerRESTUtilities.removeParameters(Utility.getFilenameExtension(file)) + " " + file).waitFor();
							}catch(Exception e){e.printStackTrace();}
						}else if(download_method.equals("nio")){
							try{
								URL website = new URL(file);
								ReadableByteChannel rbc = Channels.newChannel(website.openStream());
								FileOutputStream fos = new FileOutputStream(temp_path + "/" + Utility.getFilenameName(file) + "." + SoftwareServerRESTUtilities.removeParameters(Utility.getFilenameExtension(file)));
								fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
							}catch(Exception e){e.printStackTrace();}
						}else{
							//Utility.downloadFile(temp_path, file);
							Utility.downloadFile(temp_path, Utility.getFilenameName(file) + "." + SoftwareServerRESTUtilities.removeParameters(Utility.getFilenameExtension(file)), file);
						}
	
						//file = temp_path + Utility.getFilename(file);
						file = temp_path + SoftwareServerRESTUtilities.removeParameters(Utility.getFilename(file));
					}
					
					request = new RequestInformation(client, file, output);
					
					if(polyglot instanceof PolyglotSteward && SOFTWARE_SERVER_REST_INTERFACE){
						((PolyglotSteward)polyglot).convertOverREST(file, public_path, output);
					}else{
						result_file = polyglot.convert(file, public_path, output);
					}

					if(result_file == null) result_file = Utility.getFilenameName(file) + "." + output;		//If a name wasn't suggested assume this.
					result_url = Utility.endSlash(getReference().getBaseRef().toString()) + "file/" + result_file;

					if(Utility.existsAndNotEmpty(public_path + result_file) || Utility.existsAndNotEmpty(public_path + result_file + ".url")){
						request.setEndOfRequest(true);
						System.out.println(" [Success]");
					}else{
						request.setEndOfRequest(false);
						System.out.println(" [Failed]");
					}
					
					requests.add(request);
					if(MONGO_LOGGING) PolyglotRESTUtilities.updateMongo(db, request);
					
					if(RETURN_URL){		//Return URL of file
						if(SoftwareServerRestlet.isPlainRequest(Request.getCurrent())){
							return new StringRepresentation(result_url, MediaType.TEXT_PLAIN);
						}else{
							return new StringRepresentation("<a href=" + result_url + ">" + result_url + "</a>", MediaType.TEXT_HTML);
						}
					}else{						//Return the file
						result_file = public_path + result_file;
					
						if(Utility.exists(result_file)){
							MetadataService metadata_service = new MetadataService();
							MediaType media_type = metadata_service.getMediaType(output);
							
							if(media_type == null) media_type = MediaType.MULTIPART_ALL;
							
							FileRepresentation file_representation = new FileRepresentation(result_file, media_type);
							file_representation.getDisposition().setType(Disposition.TYPE_INLINE);
							
							return file_representation;
						}else{
							setStatus(Status.CLIENT_ERROR_NOT_FOUND);
							return new StringRepresentation("File doesn't exist", MediaType.TEXT_PLAIN);
						}
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
					return new StringRepresentation(SoftwareServerRESTUtilities.createHTMLList(buffer, Utility.endSlash(getReference().toString()), true, "Forms"), MediaType.TEXT_HTML);
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
					if(Utility.exists(file + ".url")){
						result_url = Utility.getLine(file + ".url", 2).substring(4);		//Link is on 2nd line after "URL="
						
						if(!result_url.isEmpty()){
							this.getResponse().redirectTemporary(result_url);
							return new StringRepresentation("Redirecting...", MediaType.TEXT_PLAIN);
						}else{
							setStatus(Status.CLIENT_ERROR_NOT_FOUND);
							return new StringRepresentation("File doesn't exist", MediaType.TEXT_PLAIN);
						}
					}else{
						setStatus(Status.CLIENT_ERROR_NOT_FOUND);
						return new StringRepresentation("File doesn't exist", MediaType.TEXT_PLAIN);
					}
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
		}else if(part0.equals("alive")){
			return new StringRepresentation("yes", MediaType.TEXT_PLAIN);
		}else if(part0.equals("checkin") && polyglot instanceof PolyglotStewardAMQ){
			if(!part1.isEmpty()){
				try{
					int job_id = Integer.parseInt(part1);
				
					if(!part2.isEmpty()){
						return new StringRepresentation(((PolyglotStewardAMQ)polyglot).checkin(getClientInfo().getAddress(), job_id, part2), MediaType.TEXT_PLAIN);
					}else{
						return new StringRepresentation("error: invalid file", MediaType.TEXT_PLAIN);
					}
				}catch(NumberFormatException e){
					return new StringRepresentation("error: invalid job id", MediaType.TEXT_PLAIN);
				}
			}else{
				return new StringRepresentation("error: invalid job id", MediaType.TEXT_PLAIN);
			}
		}else if(part0.equals("servers")){				
			return new StringRepresentation(PolyglotRESTUtilities.toString(polyglot.getServers()), MediaType.TEXT_PLAIN);
		}else if(part0.equals("software")){
			return new StringRepresentation(PolyglotRESTUtilities.toString(polyglot.getSoftware()), MediaType.TEXT_PLAIN);
		}else if(part0.equals("inputs")){
			if(part1.isEmpty()){
				if(SoftwareServerRestlet.isPlainRequest(Request.getCurrent())){
					return new StringRepresentation(PolyglotRESTUtilities.toString(polyglot.getInputs()), MediaType.TEXT_PLAIN);
				}else{
					return new StringRepresentation(SoftwareServerRESTUtilities.createHTMLList(PolyglotRESTUtilities.toString(polyglot.getInputs()), Utility.endSlash(getReference().toString()), true, "Inputs"), MediaType.TEXT_HTML);
				}
			}else{
				if(SoftwareServerRestlet.isJSONRequest(Request.getCurrent())){
					return new JsonRepresentation(new JSONArray(polyglot.getOutputs(part1)));
				}else{
					return new StringRepresentation(PolyglotRESTUtilities.toString(polyglot.getOutputs(part1)), MediaType.TEXT_PLAIN);
				}
			}
		}else if(part0.equals("outputs")){				
			return new StringRepresentation(PolyglotRESTUtilities.toString(polyglot.getOutputs()), MediaType.TEXT_PLAIN);
		}else if(part0.equals("requests")){
			buffer = Long.toString(start_time) + "\n";
			buffer += System.currentTimeMillis() + "\n";
			
			for(Iterator<RequestInformation> itr=requests.iterator(); itr.hasNext();){
				buffer += itr.next().toString() + "\n";
			}
			
			return new StringRepresentation(buffer, MediaType.TEXT_PLAIN);
		}else{
			buffer = "";
			buffer += "convert\n";
			buffer += "form\n";
			buffer += "alive\n";
			buffer += "servers\n";
			buffer += "software\n";
			buffer += "inputs\n";
			buffer += "outputs\n";
			buffer += "requests\n";
			
			if(SoftwareServerRestlet.isPlainRequest(Request.getCurrent())){
				return new StringRepresentation(buffer, MediaType.TEXT_PLAIN);
			}else{
				return new StringRepresentation(SoftwareServerRESTUtilities.createHTMLList(buffer, Utility.endSlash(getReference().toString()), true, "Endpoints"), MediaType.TEXT_HTML);
			}
		}
	}
	
	/**
	 * Handle HTTP POST requests.
	 * @param entity the entity
	 * @return a response
	 */
	@Post
	public Representation httpPostHandler(Representation entity)
	{
		RequestInformation request;
		Vector<String> parts = Utility.split(getReference().getRemainingPart(), '/', true);
		String part0 = (parts.size() > 0) ? parts.get(0) : "";
		String part1 = (parts.size() > 1) ? parts.get(1) : "";
		String part2 = (parts.size() > 2) ? parts.get(2) : "";
		boolean FORM_POST = !part0.isEmpty() && part0.equals("form");
		boolean TASK_POST = !part1.isEmpty();
		TreeMap<String,String> parameters = new TreeMap<String,String>();
		String file=null, output = null, result_file = null, result_url;
		String client = getClientInfo().getAddress();
		
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
							if(HOST_POSTED_FILES){
								file = public_path + fi.getName();
								fi.write(new File(file));
								file = "http://" + InetAddress.getLocalHost().getHostAddress() + ":8184/file/" + Utility.getFilename(file);
								System.out.println("[" + client + "]: Temporarily hosting file \"" + Utility.getFilename(file) + "\", " + file);
							}else{
								file = temp_path + fi.getName();
								fi.write(new File(file));
							}
						}
					}
				}catch(Exception e) {e.printStackTrace();}
			}
			
			if(FORM_POST){
				output = parameters.get("output");
			}else if(TASK_POST){
				output = part1;				
			}
							
			//Do the conversion
			if(file != null && output != null){	
				System.out.print("[" + client + "]: " + Utility.getFilename(file) + " -> " + output + "...");
				
				request = new RequestInformation(client, file, output);

				if(polyglot instanceof PolyglotSteward && SOFTWARE_SERVER_REST_INTERFACE){
					((PolyglotSteward)polyglot).convertOverREST(file, public_path, output);
				}else{
					result_file = polyglot.convert(file, public_path, output);
				}

				if(result_file == null) result_file = Utility.getFilenameName(file) + "." + output;
				result_url = Utility.endSlash(getReference().getBaseRef().toString()) + "file/" + result_file;

				if(Utility.existsAndNotEmpty(public_path + result_file) || Utility.existsAndNotEmpty(public_path + result_file + ".url")){
					request.setEndOfRequest(true);
					System.out.println(" [Success]");
				}else{
					request.setEndOfRequest(false);
					System.out.println(" [Failed]");
				}
				
				requests.add(request);
				if(MONGO_LOGGING) PolyglotRESTUtilities.updateMongo(db, request);
				
				if(RETURN_URL){		//Return URL of file
					if(SoftwareServerRestlet.isPlainRequest(Request.getCurrent())){
						return new StringRepresentation(result_url, MediaType.TEXT_PLAIN);
					}else{
						return new StringRepresentation("<a href=" + result_url + ">" + result_url + "</a>", MediaType.TEXT_HTML);
					}
				}else{						//Return the file
					result_file = public_path + result_file;
					
					if(Utility.exists(result_file)){
						MetadataService metadata_service = new MetadataService();
						MediaType media_type = metadata_service.getMediaType(output);
						
						if(media_type == null) media_type = MediaType.MULTIPART_ALL;
						
						FileRepresentation file_representation = new FileRepresentation(result_file, media_type);
						file_representation.getDisposition().setType(Disposition.TYPE_INLINE);
						
						return file_representation;
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
		
		polyglot.close();
	}
	
	/**
	 * Start the Polyglot REST interface.
	 * @param args command line arguments
	 */
	public static void main(String[] args)
	{		
		TreeMap<String,String> accounts = new TreeMap<String,String>();	

		//Load configuration file
	  try{
	    BufferedReader ins = new BufferedReader(new FileReader("PolyglotRestlet.conf"));
	    String line, key, value;
	    String server, username, password;
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
	          }else if(key.equals("StewardPort") && polyglot instanceof PolyglotSteward){
	          	steward_port = Integer.valueOf(value);
	          	
	        	  if(steward_port >= 0){
	        	  	((PolyglotSteward)polyglot).listen(steward_port);
	        	  }
	          }else if(key.equals("SoftwareServer") && polyglot instanceof PolyglotSteward){
          		tmpi = value.lastIndexOf(':');
	        		
	        		if(tmpi != -1){
	        			server = value.substring(0, tmpi);
	        			port = Integer.valueOf(value.substring(tmpi+1));
	        			((PolyglotSteward)polyglot).add(server, port);
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
							if(polyglot instanceof PolyglotSteward) ((PolyglotSteward)polyglot).setSoftwareServerRESTPort(Integer.valueOf(value));
	          }else if(key.equals("DownloadMethod")){
							download_method = value;
	          }else if(key.equals("HostPostedFile")){
							HOST_POSTED_FILES = Boolean.valueOf(value);
	          }else if(key.equals("MaxTaskTime")){
							if(polyglot instanceof PolyglotSteward) ((PolyglotSteward)polyglot).setMaxTaskTime(Integer.valueOf(value));
	          }else if(key.equals("Authentication")){
	  	        username = value.substring(0, value.indexOf(':')).trim();
	  	        password = value.substring(value.indexOf(':')+1).trim();
	  	        accounts.put(username, password);
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
			
			if(!accounts.isEmpty()){
				ChallengeAuthenticator guard = new ChallengeAuthenticator(null, ChallengeScheme.HTTP_BASIC, "realm-NCSA");
				MapVerifier verifier = new MapVerifier();
				boolean FOUND_ADMIN = false;
				boolean FOUND_USER = false;
				
				for(String username : accounts.keySet()){
					if(username.toLowerCase().startsWith("admin")){
						FOUND_ADMIN = true;
					}else{
						FOUND_USER = true;
					}
						
					verifier.getLocalSecrets().put(username, accounts.get(username).toCharArray());
				}
					
				if(FOUND_ADMIN && !FOUND_USER) guard.setOptional(true);
					
				guard.setVerifier(verifier);
				guard.setNext(application);
				component.getDefaultHost().attachDefault(guard);
			}else{
				component.getDefaultHost().attach("/", application);
			}
			
			component.start();
		}catch(Exception e) {e.printStackTrace();}
  	
		System.out.println("\nPolyglot restlet is running...");
	}
}

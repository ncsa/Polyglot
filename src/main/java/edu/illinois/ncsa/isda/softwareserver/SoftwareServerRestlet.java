package edu.illinois.ncsa.isda.softwareserver;
import edu.illinois.ncsa.isda.softwareserver.SoftwareServerAuxiliary.*;
import edu.illinois.ncsa.isda.softwareserver.SoftwareServerAuxiliary.Application;
import edu.illinois.ncsa.isda.softwareserver.SoftwareServerRESTUtilities.*;
import kgm.image.ImageUtility;
import kgm.utility.*;
import java.util.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.net.*;
import java.lang.management.*;
import java.net.*;
import javax.servlet.*;
import org.restlet.*;
import org.restlet.resource.*;
import org.restlet.data.*;
import org.restlet.representation.*;
import org.restlet.routing.*;
import org.restlet.security.*;
import org.restlet.ext.fileupload.*;
import org.restlet.service.*;
import org.apache.commons.fileupload.*;
import org.apache.commons.fileupload.disk.*;
import org.apache.commons.io.*;

/**
 * A restful interface for a software server.
 * Think of this as an extended software server.
 * @author Kenton McHenry
 */
public class SoftwareServerRestlet extends ServerResource
{
	private static SoftwareServer server;
	private static Vector<Application> applications;
	private static Vector<TreeSet<TaskInfo>> application_tasks;
	private static TreeMap<String,Application> alias_map = new TreeMap<String,Application>();
	private static String public_path = "./";
	private static boolean ADMINISTRATORS_ENABLED = false;
	private static String download_method = "";
	private static Component component;
	
	/**
	 * Initialize.
	 */
	public static void initialize()
	{
		server = new SoftwareServer("SoftwareServer.conf");
		applications = server.getApplications();
		application_tasks = Task.getApplicationTasks(applications);
		
		//Build alias map
		for(int i=0; i<applications.size(); i++){
			alias_map.put(applications.get(i).alias, applications.get(i));
		}
		
		//Create public folder for results
		public_path = server.getTempPath() + "public/";
		
		if(!Utility.exists(public_path)){
			new File(public_path).mkdir();
		}
	}

	/**
	 * Get the applications available.
	 * @return the applications
	 */
	public String getApplications()
	{
		String buffer = "";

		for(int i=0; i<applications.size(); i++){
			buffer += applications.get(i).alias + " (" + applications.get(i).name + ")\n";
		}
		
		return buffer;
	}
	
	/**
	 * Get the tasks available for the given application.
	 * @param alias the application alias
	 * @return the application tasks
	 */
	public String getApplicationTasks(String alias)
	{
		String buffer = "";

		for(int i=0; i<applications.size(); i++){
			if(applications.get(i).alias.equals(alias)){
				for(Iterator<TaskInfo> itr=application_tasks.get(i).iterator(); itr.hasNext();){
					buffer += itr.next() + "\n";
				}
				
				break;
			}
		}
		
		return buffer;
	}
	
	/**
	 * Get the output formats supported by the given task.
	 * @param alias the application alias
	 * @param task the application task
	 * @return the output formats supported
	 */
	public String getApplicationTaskOutputs(String alias, String task)
	{
		TaskInfo task_info;
		String buffer = "";
	
		for(int i=0; i<applications.size(); i++){
			if(applications.get(i).alias.equals(alias)){
				for(Iterator<TaskInfo> itr1=application_tasks.get(i).iterator(); itr1.hasNext();){
					task_info = itr1.next();
					
					if(task_info.name.equals(task)){	
						for(Iterator<String> itr2=task_info.outputs.iterator(); itr2.hasNext();){
							buffer += itr2.next() + "\n";
						}
					}
				}
				
				break;
			}
		}
		
		return buffer;
	}

	/**
	 * Get the input formats supported by the given task.
	 * @param alias the application alias
	 * @param task the application task
	 * @return the input formats supported
	 */
	public String getApplicationTaskInputs(String alias, String task)
	{
		TaskInfo task_info;
		String buffer = "";

		for(int i=0; i<applications.size(); i++){
			if(applications.get(i).alias.equals(alias)){
				for(Iterator<TaskInfo> itr1=application_tasks.get(i).iterator(); itr1.hasNext();){
					task_info = itr1.next();
					
					if(task_info.name.equals(task)){	
						for(Iterator<String> itr2=task_info.inputs.iterator(); itr2.hasNext();){
							buffer += itr2.next() + "\n";
						}
					}
				}
				
				break;
			}
		}
		
		return buffer;
	}
	
	/**
	 * Get the input and output formats supported by the given task.
	 * @param alias the application alias
	 * @param task the application task
	 * @return the input and output formats supported
	 */
	public String getApplicationTaskInputsOutputs(String alias, String task)
	{
		TaskInfo task_info;
		String buffer = "";
		boolean FIRST_VALUE;
		
		for(int i=0; i<applications.size(); i++){
			if(applications.get(i).alias.equals(alias)){
				for(Iterator<TaskInfo> itr1=application_tasks.get(i).iterator(); itr1.hasNext();){
					task_info = itr1.next();
					
					if(task_info.name.equals(task)){
						FIRST_VALUE = true;
						
						for(Iterator<String> itr2=task_info.inputs.iterator(); itr2.hasNext();){
							if(FIRST_VALUE) FIRST_VALUE = false; else buffer += ", ";
							buffer += itr2.next();
						}
						
						buffer += "\n";
						FIRST_VALUE = true;
						
						for(Iterator<String> itr2=task_info.outputs.iterator(); itr2.hasNext();){
							if(FIRST_VALUE) FIRST_VALUE = false; else buffer += ", ";
							buffer += itr2.next();
						}
					}
				}
				
				break;
			}
		}
		
		return buffer;
	}
	
	/**
	 * Get an icon representation of the available software.
	 * @return the HTML for the icon representation
	 */
	public String getApplicationStack()
	{
		return SoftwareServerRESTUtilities.getApplicationStack(applications);
	}
	
	/**
	 * Get a web form interface for this restful service.
	 * @param POST_UPLOADS true if this form should use POST rather than GET for uploading files
	 * @param selected_application the default application
	 * @param HIDE_APPLICATIONS true if applications menu should be hidden
	 * @return the form
	 */
	public String getForm(boolean POST_UPLOADS, String selected_application, boolean HIDE_APPLICATIONS)
	{
		return SoftwareServerRESTUtilities.getForm(applications, application_tasks, alias_map, POST_UPLOADS, selected_application, HIDE_APPLICATIONS);
	}
	
	/**
	 * Get a web form interface for this restful service.
	 * @return the form
	 */
	public String getForm()
	{
		return getForm(false, null, false);
	}

	/**
	 * Get a web form interface for converting via this restful service.
	 * @return the form
	 */
	public String getConvertForm()
	{
		return SoftwareServerRESTUtilities.getConvertForm(applications, application_tasks);
	}
	
	/**
	 * Get the task involved in using the given applications to convert the given file to the specified output format.
	 * @param application_alias the alias of the application to use.
	 * @param task_string the task to perform
	 * @param filename the file name of the cached file to convert
	 * @param output_format the output format
	 * @return the task to perform the conversion
	 */
	public Vector<Subtask> getTask(String application_alias, String task_string, String filename, String output_format)
	{
		Task task = new Task(applications);
		task.addSubtasks(task.getApplicationString(application_alias), task_string, new CachedFileData(filename), new CachedFileData(filename, output_format));
		
		return task.getSubtasks();
	}
	
	/**
	 * Execute a task.
	 * @param session the session id to use while executing the task
	 * @param application_alias the application to use
	 * @param task_string the task to perform (nothing assumed to be a conversion)
	 * @param file the URL of the file to convert
	 * @param format the output format
	 */
	public synchronized void executeTask(int session, String application_alias, String task_string, String file, String format)
	{
		Vector<Subtask> task;
		String result;
		
		if(session >= 0){
			if(file.startsWith(Utility.endSlash(getReference().getBaseRef().toString()) + "file/")){	//Remove session id from filenames of locally cached files
				file = SoftwareServer.getFilename(Utility.getFilename(file));
			}else{																											//Download remote files
				if(download_method.equals("wget")){
					try{
						Runtime.getRuntime().exec("wget -O " + server.getCachePath() + "/" + session + "_" + Utility.getFilenameName(file) + "." + SoftwareServerRESTUtilities.removeParameters(Utility.getFilenameExtension(file)) + " " + file).waitFor();
					}catch(Exception e){e.printStackTrace();}
				}else if(download_method.equals("nio")){
					try{
						URL website = new URL(file);
						ReadableByteChannel rbc = Channels.newChannel(website.openStream());
						FileOutputStream fos = new FileOutputStream(server.getCachePath() + "/" + session + "_" + Utility.getFilenameName(file) + "." + SoftwareServerRESTUtilities.removeParameters(Utility.getFilenameExtension(file)));
						fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
					}catch(Exception e){e.printStackTrace();}
				}else{
					Utility.downloadFile(server.getCachePath(), session + "_" + Utility.getFilenameName(file) + "." + SoftwareServerRESTUtilities.removeParameters(Utility.getFilenameExtension(file)), file);
				}

				file = SoftwareServerRESTUtilities.removeParameters(Utility.getFilename(file));
			}
		
			task = getTask(application_alias, task_string, file, format);
			//Task.print(task, applications);
			
			result = server.executeTaskAtomically("localhost", session, task);
			
			//Create empty output if not created (e.g. when no conversion path was found)
			if(result == null){
				result = server.getCachePath() + session + "_" + Utility.getFilenameName(file) + "." + format;
				Utility.touch(result);
			}

			//Move result to public folder
			if(!Utility.isDirectory(result)){
				Utility.copyFile(result, public_path + Utility.getFilename(result));
			}else{
				try{
					FileUtils.copyDirectory(new File(result), new File(public_path + Utility.getFilename(result)));
				}catch(Exception e) {e.printStackTrace();}

				//Attach directory as a new endpoint
				Directory directory = new Directory(getContext(), "file://" + Utility.absolutePath(public_path + Utility.getFilename(result)));
				directory.setListingAllowed(true);
				component.getDefaultHost().attach("/file/" + Utility.getFilename(result) + "/", directory);
			}
		}
	}
	
	/**
	 * Execute a task asynchronously.
	 * @param session the session id to use while executing the task
	 * @param application_alias the application to use
	 * @param task_string the task to perform (nothing assumed to be a conversion)
	 * @param file the URL of the input file
	 * @param format the output format
	 */
	public void executeTaskLater(int session, String application_alias, String task_string, String file, String format)
	{
		final int session_final = session;
		final String application_alias_final = application_alias;
		final String task_string_final = task_string;
		final String file_final = file;
		final String format_final = format;
		
		new Thread(){
			public void run(){
				executeTask(session_final, application_alias_final, task_string_final, file_final, format_final);
			}
		}.start();
	}
	
	/**
	 * Check if the given request is authenticated by an administrator.
	 * @param request the request
	 * @return true if authenticated as an administrator
	 */
	private boolean isAdministrator(Request request)
	{		
		if(request.getChallengeResponse() != null){
			String identifier = request.getChallengeResponse().getIdentifier();
			
			if(identifier != null && identifier.toLowerCase().startsWith("admin")){
				return true;
			}
		}
		
		return false;
	}

	@Get
	/**
	 * Handle HTTP GET requests.
	 */
	public Representation httpGetHandler()
	{
		Vector<String> parts = Utility.split(getReference().getRemainingPart(), '/', true);
		String part0 = (parts.size() > 0) ? parts.get(0) : "";
		String part1 = (parts.size() > 1) ? parts.get(1) : "";
		String part2 = (parts.size() > 2) ? parts.get(2) : "";
		String part3 = (parts.size() > 3) ? parts.get(3) : "";
		String part4 = (parts.size() > 4) ? parts.get(4) : "";
		String application = null, task = null, file = null, format = null, result, url;
		String buffer;
		Form form;
		Parameter p;
		int session;
				
		if(part0.isEmpty()){
			return new StringRepresentation(getApplicationStack(), MediaType.TEXT_HTML);
		}else if(part0.equals("software")){
			if(part1.isEmpty()){
				if(isPlainRequest(Request.getCurrent())){
					return new StringRepresentation(getApplications(), MediaType.TEXT_PLAIN);
				}else{
					return new StringRepresentation(SoftwareServerRESTUtilities.createHTMLList(getApplications(), Utility.endSlash(getReference().toString()), true, "Software"), MediaType.TEXT_HTML);
				}
			}else{
				if(part2.isEmpty()){
					if(isPlainRequest(Request.getCurrent())){
						return new StringRepresentation(getApplicationTasks(part1), MediaType.TEXT_PLAIN);
					}else{
						return new StringRepresentation(SoftwareServerRESTUtilities.createHTMLList(getApplicationTasks(part1), Utility.endSlash(getReference().toString()), true, "software/" + part1), MediaType.TEXT_HTML);
					}
				}else{
					if(part3.isEmpty()){
						if(isPlainRequest(Request.getCurrent())){
							return new StringRepresentation(getApplicationTaskOutputs(part1, part2), MediaType.TEXT_PLAIN);
						}else{
							return new StringRepresentation(SoftwareServerRESTUtilities.createHTMLList(getApplicationTaskOutputs(part1, part2), Utility.endSlash(getReference().toString()), true, "software/" + part1 + "/" + part2), MediaType.TEXT_HTML);
						}
					}else{
						if(part3.equals("*")){
							return new StringRepresentation(getApplicationTaskInputsOutputs(part1, part2), MediaType.TEXT_PLAIN);
						}else if(part4.isEmpty()){
							if(isPlainRequest(Request.getCurrent())){
								return new StringRepresentation(getApplicationTaskInputs(part1, part2), MediaType.TEXT_PLAIN);
							}else{
								return new StringRepresentation(SoftwareServerRESTUtilities.createHTMLList(getApplicationTaskInputs(part1, part2), Utility.endSlash(getReference().getBaseRef().toString()) + "form/post?application=" + part1, false, "software/" + part1 + "/" + part2 + "/" + part3), MediaType.TEXT_HTML);
							}
						}else{
							application = part1;
							task = part2;
							format = part3;
							file = URLDecoder.decode(part4);
							session = -1;
						
							//if(file.startsWith(Utility.endSlash(getReference().getBaseRef().toString()) + "/file/")){		//Locally cached files already have session ids
							if(file.startsWith(Utility.endSlash(getReference().getBaseRef().toString()))){		//Locally cached files already have session ids
								session = SoftwareServer.getSession(file);
								result = Utility.endSlash(getReference().getBaseRef().toString()) + "file/" + Utility.getFilenameName(file) + "." + format;
							}else{																																											//Remote files must be assigned a session id
								session = server.getSession();
								result = Utility.endSlash(getReference().getBaseRef().toString()) + "file/" + session + "_" + Utility.getFilenameName(file) + "." + format;
							}
														
							executeTaskLater(session, application, task, file, format);
							
							if(isPlainRequest(Request.getCurrent())){
								return new StringRepresentation(result, MediaType.TEXT_PLAIN);
							}else{
								return new StringRepresentation("<a href=" + result + ">" + result + "</a>", MediaType.TEXT_HTML);
							}
						}
					}
				}
			}
		}else if(part0.equals("form")){
			if(part1.isEmpty()){
				buffer = "";
				buffer += "get\n";
				buffer += "post\n";
				buffer += "convert";
				
				return new StringRepresentation(buffer, MediaType.TEXT_PLAIN);
			}else{				
				form = getRequest().getResourceRef().getQueryAsForm();
				p = form.getFirst("application"); if(p != null) application = p.getValue();
				p = form.getFirst("task"); if(p != null) task = p.getValue();
				p = form.getFirst("file"); if(p != null) file = p.getValue();
				p = form.getFirst("format"); if(p != null) format = p.getValue();
								
				if(application != null && task != null && file != null && format != null){
					url = Utility.endSlash(getRootRef().toString()) + "software/" + application + "/" + task + "/" + format + "/" + URLEncoder.encode(file);

					return new StringRepresentation("<html><head><meta http-equiv=\"refresh\" content=\"1; url=" + url + "\"></head></html>", MediaType.TEXT_HTML);
				}else{
					if(part1.startsWith("get")){
						return new StringRepresentation(getForm(false, application, application!=null), MediaType.TEXT_HTML);
					}else if(part1.startsWith("post")){
						return new StringRepresentation(getForm(true, application, application!=null), MediaType.TEXT_HTML);
					}else if(part1.equals("convert")){
						return new StringRepresentation(getConvertForm(), MediaType.TEXT_HTML);
					}else{
						return new StringRepresentation("error: invalid endpoint", MediaType.TEXT_PLAIN);
					}
				}
			}
		}else if(part0.equals("file")){
			if(!part1.isEmpty()){	
				file = public_path + part1;
				
				if(Utility.exists(file)){
					if(Utility.isDirectory(file)){
						url = Utility.endSlash(getRootRef().toString()) + "file/" + part1 + "/";

						//return new StringRepresentation("<html><head><meta http-equiv=\"refresh\" content=\"1; url=" + url + "\"></head></html>", MediaType.TEXT_HTML);

						//Hack to get Restlet to return a "DirectoryRepresentation"
						file += "/" + part1;
						Utility.save(file, "<html><head><meta http-equiv=\"refresh\" content=\"1; url=" + url + "\"></head></html>");
						return new FileRepresentation(file, MediaType.TEXT_HTML);
					}else{
						MetadataService metadata_service = new MetadataService();
						MediaType media_type = metadata_service.getMediaType(Utility.getFilenameExtension(part1));
						if(media_type == null) media_type = MediaType.MULTIPART_ALL;
							
						FileRepresentation file_representation = new FileRepresentation(file, media_type);
						//file_representation.getDisposition().setType(Disposition.TYPE_INLINE);
						return file_representation;
					}
				}else{
					setStatus(Status.CLIENT_ERROR_NOT_FOUND);
					return new StringRepresentation("File doesn't exist", MediaType.TEXT_PLAIN);
				}
			}else{
				return new StringRepresentation("error: invalid endpoint", MediaType.TEXT_PLAIN);
			}
		}else if(part0.equals("image")){
			if(!part1.isEmpty()){
				file = Utility.getFilenameName(part1);
				
				if(alias_map.containsKey(file)){
					file = alias_map.get(file).icon;
					if(file != null) return new FileRepresentation(file, MediaType.IMAGE_JPEG);
				}
				
				//return new StringRepresentation("Image doesn't exist", MediaType.TEXT_PLAIN);
				return new FileRepresentation("images/polyglot.png", MediaType.IMAGE_PNG);
			}else{
				return new StringRepresentation("error: invalid endpoint", MediaType.TEXT_PLAIN);
			}
		}else if(part0.equals("alive")){
			return new StringRepresentation("yes", MediaType.TEXT_PLAIN);
		}else if(part0.equals("busy")){
			return new StringRepresentation("" + server.isBusy(), MediaType.TEXT_PLAIN);
		}else if(part0.equals("processors")){
			return new StringRepresentation("" + Runtime.getRuntime().availableProcessors(), MediaType.TEXT_PLAIN);
		}else if(part0.equals("memory")){
			return new StringRepresentation("" + Runtime.getRuntime().maxMemory(), MediaType.TEXT_PLAIN);
		}else if(part0.equals("load")){
			return new StringRepresentation("" + ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage(), MediaType.TEXT_PLAIN);
		}else if(part0.equals("tasks")){
			return new StringRepresentation("" + server.getTaskCount(), MediaType.TEXT_PLAIN);
		}else if(part0.equals("kills")){
			return new StringRepresentation("" + server.getKillCount(), MediaType.TEXT_PLAIN);
		}else if(part0.equals("completed_tasks")){
			return new StringRepresentation("" + server.getCompletedTaskCount(), MediaType.TEXT_PLAIN);
		}else if(ADMINISTRATORS_ENABLED && isAdministrator(getRequest())){
			if(part0.equals("screen")){
				ImageUtility.save(public_path + "screen.jpg", ImageUtility.getScreen());
				return new FileRepresentation(new File(public_path + "screen.jpg"), MediaType.IMAGE_JPEG);
			}else if(part0.equals("reset")){
				server.resetCounts();
				return new StringRepresentation("ok", MediaType.TEXT_PLAIN);
			}else if(part0.equals("reboot")){
				server.rebootMachine();
				return new StringRepresentation("ok", MediaType.TEXT_PLAIN);
			}else{
				return new StringRepresentation("error: invalid endpoint", MediaType.TEXT_PLAIN);
			}
		}else{
			return new StringRepresentation("error: invalid endpoint", MediaType.TEXT_PLAIN);
		}
	}
	
	@Post
	/**
	 * Handle HTTP POST requests.
	 * @param entity the entity
	 */
	public Representation httpPostHandler(Representation entity)
	{
		Vector<String> parts = Utility.split(getReference().getRemainingPart(), '/', true);
		String part0 = (parts.size() > 0) ? parts.get(0) : "";
		String part1 = (parts.size() > 1) ? parts.get(1) : "";
		String part2 = (parts.size() > 2) ? parts.get(2) : "";
		String part3 = (parts.size() > 3) ? parts.get(3) : "";
		TreeMap<String,String> parameters = new TreeMap<String,String>();
		String application = null, task = null, file = null, format = null, result;
		int session = server.getSession();
		boolean FORM_POST = !part0.isEmpty() && part0.equals("form");
		boolean TASK_POST = !part1.isEmpty() && !part2.isEmpty() && !part3.isEmpty();
				
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
							fi.write(new File(server.getCachePath() + session + "_" + fi.getName()));
							file = Utility.endSlash(getReference().getBaseRef().toString()) + "file/" + session + "_" + fi.getName();
						}
					}
				}catch(Exception e) {e.printStackTrace();}
			}
			
			if(FORM_POST){
				application = parameters.get("application");
				task = parameters.get("task");
				format = parameters.get("format");
			}else if(TASK_POST){
				application = part1;
				task = part2;
				format = part3;				
			}
			
			if(application != null && task != null && file != null && format != null){		
				executeTaskLater(session, application, task, file, format);
				
				result = Utility.endSlash(getReference().getBaseRef().toString()) + "file/" + Utility.getFilenameName(file) + "." + format;

				if(isPlainRequest(Request.getCurrent())){
					return new StringRepresentation(result, MediaType.TEXT_PLAIN);
				}else{
					return new StringRepresentation("<a href=" + result + ">" + result + "</a>", MediaType.TEXT_HTML);
				}
			}
		}
		
		return httpGetHandler();
	}
	
	/**
	 * Stop the REST interface and underlying Software Server.
	 */
	public void stop()
	{
		//Stop the REST interface
		try{
			component.stop();
		}catch(Exception e) {e.printStackTrace();}
		
		//Stop the Software Server
		server.stop();
	}
	
	/**
	 * Check if the given request is for plain text only.
	 * @param request the request
	 * @return true if plain/text only
	 */
	public static boolean isPlainRequest(Request request)
	{
		List<Preference<MediaType>> types = request.getClientInfo().getAcceptedMediaTypes();

		if(types.size() == 1 && types.get(0).getMetadata().getName().equals("text/plain")){
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 * Check if the given request is for json only.
	 * @param request the request
	 * @return true if application/json only
	 */
	public static boolean isJSONRequest(Request request)
	{
		List<Preference<MediaType>> types = request.getClientInfo().getAcceptedMediaTypes();

		if(types.size() == 1 && types.get(0).getMetadata().getName().equals("application/json")){
			return true;
		}else{
			return false;
		}
	}

	/**
	 * Start the restful service.
	 * @param args the input arguments
	 */
	public static void main(String[] args)
	{		
		int port = 8182;		
		String distributed_server = null;
		TreeMap<String,String> accounts = new TreeMap<String,String>();	
		
		//Load configuration file
	  try{
	    BufferedReader ins = new BufferedReader(new FileReader("SoftwareServerRestlet.conf"));
	    String line, key, value;
	    String username, password;

	    while((line=ins.readLine()) != null){
	      if(line.contains("=")){
	        key = line.substring(0, line.indexOf('=')).trim();
	        value = line.substring(line.indexOf('=')+1).trim();
	        
	        if(key.charAt(0) != '#'){
	        	if(key.equals("Port")){
	        		port = Integer.valueOf(value);
	          }else if(key.equals("DistributedServer")){
	          	distributed_server = value;
	          }else if(key.equals("Authentication")){
	  	        username = value.substring(0, value.indexOf(':')).trim();
	  	        password = value.substring(value.indexOf(':')+1).trim();
	  	        accounts.put(username, password);
	          }else if(key.equals("EnableAdministrators")){
	          	ADMINISTRATORS_ENABLED = Boolean.valueOf(value);
	          }else if(key.equals("DownloadMethod")){
	          	download_method = value;
	          }
	        }
	      }
	    }
	    
	    ins.close();
	  }catch(Exception e) {e.printStackTrace();}
		
	  //Initialize and start the service
	  initialize();
	  
		/*
		try{
			new Server(Protocol.HTTP, port, SoftwareServerRestlet.class).start();
		}catch(Exception e) {e.printStackTrace();}
		*/
			
		try{			
			component = new Component();
			component.getServers().add(Protocol.HTTP, port);
			component.getClients().add(Protocol.HTTP);
			component.getClients().add(Protocol.FILE);
			component.getLogService().setEnabled(false);
			
			org.restlet.Application application = new org.restlet.Application(){
				@Override
				public Restlet createInboundRoot(){
					Router router = new Router(getContext());
					router.attachDefault(SoftwareServerRestlet.class);
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
			
	 	//Notify other services of our existence
	 	if(distributed_server != null){
	 		final int port_final = port;
	 		final String distributed_server_final = distributed_server;
	  		  		
	 		System.out.println("\nStarting distributed software restlet notification thread...\n");

	  	new Thread(){
	  		public void run(){
	  			String hostname = "", url;
		  			
	  			try{
	  				hostname = InetAddress.getLocalHost().getHostAddress();		  			
	  			}catch(Exception e) {e.printStackTrace();}
		  			
	  			url = "http://" + distributed_server_final + "/register/" + URLEncoder.encode(hostname + ":" + port_final);
		  			
	  			while(true){
	  				SoftwareServerRESTUtilities.queryEndpoint(url);
	  				Utility.pause(2000);
	  			}
	  		}
	  	}.start();
	 	}
	}
}
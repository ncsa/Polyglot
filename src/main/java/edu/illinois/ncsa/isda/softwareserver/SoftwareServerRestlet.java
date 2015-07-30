package edu.illinois.ncsa.isda.softwareserver;
import edu.illinois.ncsa.isda.softwareserver.SoftwareServerAuxiliary.*;
import edu.illinois.ncsa.isda.softwareserver.SoftwareServerAuxiliary.Application;
import edu.illinois.ncsa.isda.softwareserver.SoftwareServerRESTUtilities.*;
import kgm.image.ImageUtility;
import kgm.utility.*;
import java.util.*;
import java.util.concurrent.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.net.*;
import java.lang.management.*;
import java.text.*;
import javax.servlet.*;
import org.json.JSONArray;
import org.restlet.*;
import org.restlet.resource.*;
import org.restlet.data.*;
import org.restlet.representation.*;
import org.restlet.routing.*;
import org.restlet.security.*;
import org.restlet.ext.fileupload.*;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.service.*;
import org.restlet.engine.header.*;
import org.restlet.engine.application.*;
import org.restlet.util.*;
import org.apache.commons.fileupload.*;
import org.apache.commons.fileupload.disk.*;
import org.apache.commons.io.*;
import org.apache.http.util.EntityUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import com.rabbitmq.client.Channel;
import org.json.*;
// Used to check whether a string is a valid IP address.
import org.apache.commons.validator.routines.InetAddressValidator;

/**
 * A restful interface for a software server.
 * Think of this as an extended software server.
 * @author Kenton McHenry
 */
public class SoftwareServerRestlet extends ServerResource
{
	private static SoftwareServer server;
	private static TreeMap<String,String> accounts = new TreeMap<String,String>();	
	private static Vector<Application> applications;
	private static Vector<TreeSet<TaskInfo>> application_tasks;
	private static TreeMap<String,Application> alias_map = new TreeMap<String,Application>();
	private static long initialization_time = -1;
	private static String public_path = "./";
	private static String context = "";
	private static boolean GUESTS_ENABLED = false;
	private static boolean ADMINISTRATORS_ENABLED = false;
	private static boolean ATOMIC_EXECUTION = true;
	private static boolean USE_OPENSTACK_PUBLIC_IP = false;
	private static String openstack_public_ipv4_url = "";		//Default value is "http://169.254.169.254/2009-04-04/meta-data/public-ipv4".
	private static String public_ip = "";
	private static String external_public_ip_services = "";
	private static String download_method = "";
	private static Component component;
	private static SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy");
	
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
		//public_path = server.getTempPath() + "public/";
		public_path = server.getCachePath() + "public/";
		
		if(!Utility.exists(public_path)){
			new File(public_path).mkdir();
		}
		
		initialization_time = System.currentTimeMillis();
	}

	/**
	 * Get the public IP determined by the software server.
	 * @return the software servers public IP
	 */
	public String getPublicIP()
	{
		return public_ip;
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
	 * Get the task involved in using the given applications to convert the given file to the specified output format (treats all extensions as lower case).
	 * @param application_alias the alias of the application to use.
	 * @param task_string the task to perform
	 * @param filename the file name of the cached file to convert
	 * @param output_format the output format
	 * @return the task to perform the conversion
	 */
	public Vector<Subtask> getTask(String application_alias, String task_string, String filename, String output_format)
	{
		boolean MULTIPLE_EXTENSIONS = alias_map.get(application_alias).supportedInput(Utility.getFilenameExtension(Utility.getFilename(filename), true).toLowerCase());
		Task task = new Task(applications);
		CachedFileData input_data, output_data;

		//input_data = new CachedFileData(filename);
		input_data = new CachedFileData(getFilenameName(filename, MULTIPLE_EXTENSIONS), Utility.getFilenameExtension(filename).toLowerCase(), Utility.getFilenameExtension(Utility.getFilename(filename), true).toLowerCase());
		//output_data = new CachedFileData(filename, output_format, MULTIPLE_EXTENSIONS);
		output_data = new CachedFileData(getFilenameName(filename, MULTIPLE_EXTENSIONS), output_format, output_format);

		task.addSubtasks(task.getApplicationString(application_alias), task_string, input_data, output_data);
		
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
	public void executeTask(int session, String application_alias, String task_string, String file, String format)
	{
		Vector<Subtask> task;
		String localhost, result, username = null, password = null;
		boolean DOWNLOAD_COMPLETED;
		
		//localhost = getReference().getBaseRef().toString();
		//localhost = "http://" + Utility.getLocalHostIP() + ":8182";
		localhost = public_ip;

		if(session >= 0){
			if(file.startsWith(Utility.endSlash(localhost) + "file/")){	//Remove session id from filenames of locally cached files
				//file = SoftwareServer.getFilename(Utility.getFilename(file));
				file = getFilename(file);
			}else{																											//Download remote files
				System.out.println("[" + sdf.format(new Date(System.currentTimeMillis())) + "] [restlet] [" + session + "]: Downloading " + file + " (" + SoftwareServerUtility.getFileSizeHR(file) + ") ...");
				SoftwareServerUtility.println("[" + sdf.format(new Date(System.currentTimeMillis())) + "] [restlet] [" + session + "]: Downloading " + file + " (" + SoftwareServerUtility.getFileSizeHR(file) + ") ...", server.getCachePath() + ".session_" + session + ".txt");
					
				if(file.contains("@")){
					String[] strings = file.split("@");
					strings = strings[0].split("//");
					strings = strings[1].split(":");
					username = strings[0];
					password = strings[1];

					SoftwareServerUtility.setDefaultAuthentication(username + ":" + password);
				}

				if(download_method.equals("wget")){
					try{
						if(username != null && password != null){
							DOWNLOAD_COMPLETED = SoftwareServerUtility.executeAndWait("wget --user=" + username + " --password=" + password + " -O " + server.getCachePath() + "/" + session + "_" + Utility.getFilenameName(file) + "." + SoftwareServerRESTUtilities.removeParameters(Utility.getFilenameExtension(file)).toLowerCase() + " " + file, server.getMaxOperationTime(), true, false) != null;
						}else{
							DOWNLOAD_COMPLETED = SoftwareServerUtility.executeAndWait("wget --verbose -O " + server.getCachePath() + "/" + session + "_" + Utility.getFilenameName(file) + "." + SoftwareServerRESTUtilities.removeParameters(Utility.getFilenameExtension(file)).toLowerCase() + " " + file, server.getMaxOperationTime(), true, false) != null;
						}

						if(!DOWNLOAD_COMPLETED){
							System.out.println("[" + sdf.format(new Date(System.currentTimeMillis())) + "] [restlet] [" + session + "]: Download of " + file + " failed");
							SoftwareServerUtility.println("[" + sdf.format(new Date(System.currentTimeMillis())) + "] [restlet] [" + session + "]: Download of " + file + " failed", server.getCachePath() + ".session_" + session + ".txt");
						}
					}catch(Exception e){e.printStackTrace();}
				}else if(download_method.equals("nio")){
					try{
						URL website = new URL(file);
						ReadableByteChannel rbc = Channels.newChannel(website.openStream());
						FileOutputStream fos = new FileOutputStream(server.getCachePath() + "/" + session + "_" + Utility.getFilenameName(file) + "." + SoftwareServerRESTUtilities.removeParameters(Utility.getFilenameExtension(file)).toLowerCase());
						fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
					}catch(Exception e){e.printStackTrace();}
				}else{
					Utility.downloadFile(server.getCachePath(), session + "_" + Utility.getFilenameName(file) + "." + SoftwareServerRESTUtilities.removeParameters(Utility.getFilenameExtension(file)).toLowerCase(), file);
				}

				file = SoftwareServerRESTUtilities.removeParameters(Utility.getFilename(file));
			}
		
			task = getTask(application_alias, task_string, session + "_" + file, format);
			//Task.print(task, applications);
		
			if(ATOMIC_EXECUTION){	
				result = server.executeTaskAtomically("localhost", session, task);
			}else{
				result = server.executeTask("localhost", session, task);
			}
							
			//Create empty output if not created (e.g. when no conversion path was found)
			if(result == null){
				result = server.getCachePath() + session + "_" + Utility.getFilenameName(file) + "." + format;
				Utility.touch(result);
			}
			
			System.out.println("[" + sdf.format(new Date(System.currentTimeMillis())) + "] [restlet] [" + session + "]: Execution complete, result at " + result + " (" + SoftwareServerUtility.getFileSizeHR(result) + ")");
			SoftwareServerUtility.println("[" + sdf.format(new Date(System.currentTimeMillis())) + "] [restlet] [" + session + "]: Execution complete, result at " + result + " (" + SoftwareServerUtility.getFileSizeHR(result) + ")", server.getCachePath() + ".session_" + session + ".txt");

			//Move result to public folder
			if(!Utility.isDirectory(result)){
				Utility.copyFile(result, public_path + session + "_" + getFilename(result));
			}else{
				try{
					FileUtils.copyDirectory(new File(result), new File(public_path + Utility.getFilename(result)));
				}catch(Exception e) {e.printStackTrace();}

				//Attach directory as a new endpoint
				Directory directory = new Directory(getContext(), "file://" + Utility.absolutePath(public_path + Utility.getFilename(result)));
				directory.setListingAllowed(true);
				//component.getDefaultHost().attach("/file/" + Utility.getFilename(result) + "/", directory);

				//Add CORS filter
  			CorsFilter corsfilter = new CorsFilter(getContext(), directory);
  			corsfilter.setAllowedOrigins(new HashSet<String>(Arrays.asList("*")));
  			//corsfilter.setAllowedHeaders(new HashSet<String>(Arrays.asList("x-requested-with", "Content-Type")));
  			corsfilter.setAllowedCredentials(true);
				component.getDefaultHost().attach("/file/" + Utility.getFilename(result) + "/", corsfilter);
			}
			
			System.out.println("[" + sdf.format(new Date(System.currentTimeMillis())) + "] [restlet] [" + session + "]: Copied result to public folder");
			SoftwareServerUtility.println("[" + sdf.format(new Date(System.currentTimeMillis())) + "] [restlet] [" + session + "]: Copied result to public folder", server.getCachePath() + ".session_" + session + ".txt");
		
			//Move log file		
			Utility.copyFile(server.getCachePath() + ".session_" + session + ".txt", public_path + session + "_" + getFilename(result) + ".txt");
		}
	}
	
	/**
	 * Execute a task (synchronized version).
	 * @param session the session id to use while executing the task
	 * @param application_alias the application to use
	 * @param task_string the task to perform (nothing assumed to be a conversion)
	 * @param file the URL of the file to convert
	 * @param format the output format
	 */
	public synchronized void executeTaskAtomically(int session, String application_alias, String task_string, String file, String format)
	{
		executeTask(session, application_alias, task_string, file, format);
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
				if(ATOMIC_EXECUTION){
					executeTaskAtomically(session_final, application_alias_final, task_string_final, file_final, format_final);
				}else{
					executeTask(session_final, application_alias_final, task_string_final, file_final, format_final);
				}
			}
		}.start();
	}
	
	/**
	 * Check if the given request is authenticated by the specified user.
	 * @param request the request
	 * @param user the user to check for
	 * @return true if authenticated as a the given user
	 */
	private boolean isUser(Request request, String user)
	{		
		if(request.getChallengeResponse() != null){
			String identifier = request.getChallengeResponse().getIdentifier();
			
			if(identifier != null && identifier.toLowerCase().startsWith(user)){
				return true;
			}
		}
		
		return false;
	}

	/**
	 * Handle HTTP GET requests.
	 */
	@Get
	public Representation httpGetHandler()
	{
		Vector<String> parts = Utility.split(getReference().getRemainingPart(), '/', true);
		if(parts.size() > 0 && parts.get(0).equals(context)) parts.remove(0);

		String part0 = (parts.size() > 0) ? parts.get(0) : "";
		String part1 = (parts.size() > 1) ? parts.get(1) : "";
		String part2 = (parts.size() > 2) ? parts.get(2) : "";
		String part3 = (parts.size() > 3) ? parts.get(3) : "";
		String part4 = (parts.size() > 4) ? parts.get(4) : "";
		String application_alias = null, task = null, file = null, format = null, localhost = "http://localhost:8182", result, url;
		boolean MULTIPLE_EXTENSIONS;
		String buffer;
		Form form;
		Parameter p;
		int session;
		
		//Parse endpoint
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
						}else if(GUESTS_ENABLED && isUser(getRequest(), "guest")){
							return new StringRepresentation("error: guest user can not submit jobs", MediaType.TEXT_PLAIN);
						}else{
							application_alias = part1;
							task = part2;
							format = part3;
	
							try{
								file = URLDecoder.decode(part4, "UTF-8");
							}catch(Exception e) {e.printStackTrace();}

							session = -1;
							//localhost = getReference().getBaseRef().toString();
							//localhost = "http://" + Utility.getLocalHostIP() + ":8182";
							localhost = public_ip;
							MULTIPLE_EXTENSIONS = alias_map.get(application_alias).supportedInput(Utility.getFilenameExtension(Utility.getFilename(file), true).toLowerCase());
	
							if(file.startsWith(Utility.endSlash(localhost))){																						//Locally cached files already have session ids
								session = getSession(file);
								result = Utility.endSlash(localhost) + "file/" + session + "_" + getFilenameName(file, MULTIPLE_EXTENSIONS) + "." + format;
							}else{																																											//Remote files must be assigned a session id
								session = server.getSession();
								result = Utility.endSlash(localhost) + "file/" + session + "_" + Utility.getFilenameName(file, MULTIPLE_EXTENSIONS) + "." + format;
							}
				
							if(GUESTS_ENABLED) result = result.substring(0, 7) + "guest:guest@" + result.substring(7);
							System.out.println("[" + sdf.format(new Date(System.currentTimeMillis())) + "] [restlet] [" + session + "]: Setting session to session-" + session + ", result will be at " + result);
							SoftwareServerUtility.println("[" + sdf.format(new Date(System.currentTimeMillis())) + "] [restlet] [" + session + "]: Setting session to session-" + session + ", result will be at " + result, server.getCachePath() + ".session_" + session + ".txt");

							executeTaskLater(session, application_alias, task, file, format);
							
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
				p = form.getFirst("application"); if(p != null) application_alias = p.getValue();
				p = form.getFirst("task"); if(p != null) task = p.getValue();
				p = form.getFirst("file"); if(p != null) file = p.getValue();
				p = form.getFirst("format"); if(p != null) format = p.getValue();
								
				if(application_alias != null && task != null && file != null && format != null){
					url = Utility.endSlash(getRootRef().toString()) + "software/" + application_alias + "/" + task + "/" + format + "/" + URLEncoder.encode(file);

					return new StringRepresentation("<html><head><meta http-equiv=\"refresh\" content=\"1; url=" + url + "\"></head></html>", MediaType.TEXT_HTML);
				}else{
					if(part1.startsWith("get")){
						return new StringRepresentation(getForm(false, application_alias, application_alias!=null), MediaType.TEXT_HTML);
					}else if(part1.startsWith("post")){
						return new StringRepresentation(getForm(true, application_alias, application_alias!=null), MediaType.TEXT_HTML);
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
					//System.out.println("[REST]: Request for file, " + file);

					if(Utility.isDirectory(file)){
						url = Utility.endSlash(getRootRef().toString()) + "file/" + part1 + "/";

						//return new StringRepresentation("<html><head><meta http-equiv=\"refresh\" content=\"1; url=" + url + "\"></head></html>", MediaType.TEXT_HTML);

						/*
						//Hack to get Restlet to return a "DirectoryRepresentation"
						file += "/" + part1;
						Utility.save(file, "<html><head><meta http-equiv=\"refresh\" content=\"1; url=" + url + "\"></head></html>");
						return new FileRepresentation(file, MediaType.TEXT_HTML);
						*/

						//Redirect to directory endpoint, added a "/"
						this.getResponse().redirectTemporary(url);
						return new StringRepresentation("Redirecting...", MediaType.TEXT_PLAIN);
					}else{
						MetadataService metadata_service = new MetadataService();
						MediaType media_type = metadata_service.getMediaType(Utility.getFilenameExtension(part1));
						if(media_type == null) media_type = MediaType.MULTIPART_ALL;
							
						FileRepresentation file_representation = new FileRepresentation(file, media_type);
						//file_representation.getDisposition().setType(Disposition.TYPE_INLINE);
						return file_representation;
					}
				}else{
					setStatus(org.restlet.data.Status.CLIENT_ERROR_NOT_FOUND);
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
			//return new StringRepresentation("yes", MediaType.TEXT_PLAIN);
			return new StringRepresentation(Long.toString(initialization_time), MediaType.TEXT_PLAIN);
		}else if(part0.equals("applications")){
			Application application;
			Operation operation;
			JSONArray json = new JSONArray();
			JSONObject application_info;
			JSONArray conversions_list;
			JSONObject conversions;
			JSONArray inputs, outputs;
			
			try{
				for(int a=0; a<applications.size(); a++){
 			  	application = applications.get(a);
					application_info = new JSONObject();
					application_info.put("alias", application.alias);
					conversions_list = new JSONArray();

     			for(int o=0; o<application.operations.size(); o++){
       			operation = application.operations.get(o);
						conversions = new JSONObject();
						inputs = new JSONArray();
						outputs = new JSONArray();

       			if(!operation.inputs.isEmpty()){
         			if(!operation.outputs.isEmpty()){   //Conversion operation
           			for(int i=0; i<operation.inputs.size(); i++){
             			inputs.put(operation.inputs.get(i));
								}

             		for(int j=0; j<operation.outputs.size(); j++){
               		outputs.put(operation.outputs.get(j));
           			}
         			}else{                              //Open/Import operation
               	for(int j=0; j<operation.inputs.size(); j++){
                	inputs.put(operation.inputs.get(j));
								}
           			
								for(int i=0; i<application.operations.size(); i++){
             			if(application.operations.get(i).inputs.isEmpty() && !application.operations.get(i).outputs.isEmpty()){
                 		for(int k=0; k<application.operations.get(i).outputs.size(); k++){
                   		outputs.put(application.operations.get(i).outputs.get(k));
               			}
             			}
           			}
         			}
					
							conversions.put("inputs", inputs);
							conversions.put("outputs", outputs);
							conversions_list.put(conversions);
       			}
					}
				
					application_info.put("conversions", conversions_list);
					json.put(application_info);
				}
			}catch(Exception e) {e.printStackTrace();}
			
			return new JsonRepresentation(json);
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
		}else if(ADMINISTRATORS_ENABLED && isUser(getRequest(), "admin")){
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
		if(parts.size() > 0 && parts.get(0).equals(context)) parts.remove(0);

		String part0 = (parts.size() > 0) ? parts.get(0) : "";
		String part1 = (parts.size() > 1) ? parts.get(1) : "";
		String part2 = (parts.size() > 2) ? parts.get(2) : "";
		String part3 = (parts.size() > 3) ? parts.get(3) : "";
		TreeMap<String,String> parameters = new TreeMap<String,String>();
		String application = null, task = null, file = null, format = null, localhost, result;
		int session = server.getSession();
		boolean FORM_POST = !part0.isEmpty() && part0.equals("form");
		boolean TASK_POST = !part1.isEmpty() && !part2.isEmpty() && !part3.isEmpty();
		boolean MULTIPLE_EXTENSIONS;

		//localhost = getReference().getBaseRef().toString();
		//localhost = "http://" + Utility.getLocalHostIP() + ":8182";
		localhost = public_ip;
							
		System.out.println("[" + sdf.format(new Date(System.currentTimeMillis())) + "] [restlet] [" + session + "]: Setting session to session-" + session);
		SoftwareServerUtility.println("[" + sdf.format(new Date(System.currentTimeMillis())) + "] [restlet] [" + session + "]: Setting session to session-" + session, server.getCachePath() + ".session_" + session + ".txt");
				
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
							fi.write(new File(server.getCachePath() + session + "_" + (fi.getName()).replace(" ","_")));
							
							String extension = Utility.getFilenameExtension(fi.getName());
							
							if(extension.isEmpty()){		//If no extension add one (the 'trid' command can be obtained at http://mark0.net/soft-trid-e.html)
								String myCommand ="trid -r:1 -ae " + server.getCachePath() + session + "_" + (fi.getName()).replace(" ","_") + " | grep % "+ "| awk  '{print tolower($2) }'" + "|  sed 's/^.\\(.*\\).$/\\1/'";
								Process p = Runtime.getRuntime().exec(new String[] {"sh", "-c", myCommand});
								p.waitFor();
								BufferedReader buf = new BufferedReader(new InputStreamReader(p.getInputStream()));
								extension = buf.readLine();
								Utility.pause(1000);

							  file = Utility.endSlash(localhost) + "file/" + session + "_" + (fi.getName()).replace(" ","_") + extension;
							}else{
							  file = Utility.endSlash(localhost) + "file/" + session + "_" + (fi.getName()).replace(" ","_");
							}
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
				
				MULTIPLE_EXTENSIONS = alias_map.get(application).supportedInput(Utility.getFilenameExtension(Utility.getFilename(file), true).toLowerCase());
				result = Utility.endSlash(getReference().getBaseRef().toString()) + "file/" + Utility.getFilenameName(file, MULTIPLE_EXTENSIONS) + "." + format;

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
	 * Parse the session id from the cached public filename (addressing that some may be in public output directories!).
	 * @param filename the cached public filename
	 * @return the session id
	 */
	public static int getSession(String filename)
	{
		int tmpi = filename.indexOf("/file/");

		if(tmpi >= 0){
			filename = filename.substring(tmpi+6);
			tmpi = filename.indexOf('_');

			if(tmpi >= 0){
				try{
					return Integer.valueOf(filename.substring(0, tmpi));
				}catch(NumberFormatException e){
					return -1;
				}
			}
		}
		
		return -1;
	}

	/**
   * Parse the filename from the cached public filename (leaves on prepended public output directories!).
   * @param filename the cached public filename (either the local URL or the local filename without the cache path prepended.  Assumes session ID is prepended!)
   * @return the filename (including parent directory if has one, e.g. previously unzipped files)
   */
  public static String getFilename(String filename)
  {
    int tmpi = filename.indexOf("/file/");

		if(tmpi >= 0){
			filename = filename.substring(tmpi+6);
		}

		tmpi = filename.indexOf('_');

   	if(tmpi >= 0){
     	return filename.substring(tmpi+1);
   	}

    return filename;
  }

	/**
   * Get the name of a file from the cached public filename (i.e. no path and no extension while leaving on prepended output directories!)
   * @param filename the cached public filename (either the local URL or the local filename without the cach path prepended.  Assumes session ID is prepended!)
   * @param MULTIPLE_EXTENSIONS true if files are allowed to have multiple extensions
   * @return the name of the file (including parent directory if has one, e.g. previously unzipped files)
   */
	public static String getFilenameName(String filename, boolean MULTIPLE_EXTENSIONS)
	{
		String name = getFilename(filename);
    int tmpi;

    //Remove extension
    if(MULTIPLE_EXTENSIONS){
			tmpi = name.lastIndexOf('/');
		
			if(tmpi >= 0){	//Start after last '/' if a directory is part of the name
				tmpi = name.indexOf('.', tmpi+1);
			}else{
      	tmpi = name.indexOf('.');
			}
    }else{
      tmpi = name.lastIndexOf('.');
    }

    if(tmpi >= 0){
      name = name.substring(0, tmpi);
    }

    return name;
	}

	/**
	 * Start the restful service.
	 * @param args the input arguments
	 */
	public static void main(String[] args)
	{		
	  String last_username = null, last_password = null;
		int port = 8182;		
		String distributed_server = null;
		String rabbitmq_uri = null;
		String rabbitmq_server = null;
		String rabbitmq_vhost = "/";
		String rabbitmq_username = null;
		String rabbitmq_password = null;
		boolean rabbitmq_WAITTOACK = true;
		
		//Load configuration file
	  try{
	    BufferedReader ins = new BufferedReader(new FileReader("SoftwareServerRestlet.conf"));
	    String line, key, value;

	    while((line=ins.readLine()) != null){
	      if(line.contains("=")){
	        key = line.substring(0, line.indexOf('=')).trim();
	        value = line.substring(line.indexOf('=')+1).trim();
	        
	        if(key.charAt(0) != '#'){
	        	if(key.equals("Context")){
	        		context = value;
	        	}else if(key.equals("Port")){
	        		port = Integer.valueOf(value);
	          }else if(key.equals("DistributedServer")){
	          	distributed_server = value;
	          }else if(key.equals("RabbitMQURI")){
	          	rabbitmq_uri = value;
	          }else if(key.equals("RabbitMQServer")){
	          	rabbitmq_server = value;
	          }else if(key.equals("RabbitMQVirtualHost")){
	          	rabbitmq_vhost = value;
	          }else if(key.equals("RabbitMQUsername")){
	          	rabbitmq_username = value;
	          }else if(key.equals("RabbitMQPassword")){
	          	rabbitmq_password = value;
	          }else if(key.equals("RabbitMQWaitToAcknowledge")){
	          	rabbitmq_WAITTOACK = Boolean.valueOf(value);
	          }else if(key.equals("Authentication")){
	  	        last_username = value.substring(0, value.indexOf(':')).trim();
	  	        last_password = value.substring(value.indexOf(':')+1).trim();
							System.out.println("Adding user: " + last_username);
	  	        accounts.put(last_username, last_password);	
	          }else if(key.equals("EnableGuests")){
							System.out.println("Adding user: guest");
							accounts.put("guest", "guest");
	          	GUESTS_ENABLED = Boolean.valueOf(value);
	          }else if(key.equals("EnableAdministrators")){
	          	ADMINISTRATORS_ENABLED = Boolean.valueOf(value);
	          }else if(key.equals("DownloadMethod")){
	          	download_method = value;
	          }else if(key.equals("AtomicExecution")){
	          	ATOMIC_EXECUTION = Boolean.valueOf(value);
	          }else if(key.equals("UseOpenStackPublicIP")){
							USE_OPENSTACK_PUBLIC_IP = Boolean.valueOf(value);
							System.out.println("Setting USE_OPENSTACK_PUBLIC_IP to " + USE_OPENSTACK_PUBLIC_IP);
	          }else if(key.equals("OpenStackPublicIPv4URL")){
							openstack_public_ipv4_url = value;
							System.out.println("Setting Openstack Public IPv4 URL to " + openstack_public_ipv4_url);
	          }else if(key.equals("ExternalPublicIPServices")){
							external_public_ip_services = value;
							System.out.println("Setting External Public IP Services to " + external_public_ip_services);
	          }
	        }
	      }
	    }
	    
	    ins.close();
	  }catch(Exception e) {e.printStackTrace();}
		
		try{
			if(USE_OPENSTACK_PUBLIC_IP){
				String ip = Utility.readURL(openstack_public_ipv4_url, "text/plain");

				if(!InetAddressValidator.getInstance().isValid(ip)){
					String[] urlList = external_public_ip_services.split(" ");

					for(int i = 0; i < urlList.length; i++){
						ip = Utility.readURL(urlList[i], "text/plain");

						if(InetAddressValidator.getInstance().isValid(ip)){
							System.out.println("Public IP " + ip + " resolved by '" + urlList[i] + "'.");
							break;
						}
					}
				}

				public_ip = "http://" + ip + ":8182";
	    }else{
				public_ip = "http://" + Utility.getLocalHostIP() + ":8182";
			}
	  }catch(Exception e){
			System.out.println("Error in getting public IP: " + e.getMessage());
			//e.printStackTrace();
	  }

	  System.out.println("Setting Public IP to " + public_ip);

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
		
					//Examine public path and reattach any previous directories to an endpoint
					File folder = new File(public_path);
					File[] files = folder.listFiles();

					for(int i=0; i<files.length; i++){
						if(files[i].isDirectory()){
							//System.out.println("[REST]: Re-attaching " + files[i].getName());
							Directory directory = new Directory(getContext(), "file://" + Utility.absolutePath(public_path + files[i].getName()));
							directory.setListingAllowed(true);

							//Add CORS filter
  						CorsFilter corsfilter = new CorsFilter(getContext(), directory);
  						corsfilter.setAllowedOrigins(new HashSet<String>(Arrays.asList("*")));
  						//corsfilter.setAllowedHeaders(new HashSet<String>(Arrays.asList("x-requested-with", "Content-Type")));
  						corsfilter.setAllowedCredentials(true);
							component.getDefaultHost().attach("/file/" + files[i].getName() + "/", corsfilter);
						}
					}
			
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
						guard.setNext(router);
						
						//Add a CORS filter to allow cross-domain requests
  					CorsFilter corsfilter = new CorsFilter(getContext(), guard);
  					corsfilter.setAllowedOrigins(new HashSet<String>(Arrays.asList("*")));
  					//corsfilter.setAllowedHeaders(new HashSet<String>(Arrays.asList("x-requested-with", "Content-Type")));
  					corsfilter.setAllowedCredentials(false);

						return corsfilter;
					}else{
						//Add a CORS filter to allow cross-domain requests
  					CorsFilter corsfilter = new CorsFilter(getContext(), router);
  					corsfilter.setAllowedOrigins(new HashSet<String>(Arrays.asList("*")));
  					//corsfilter.setAllowedHeaders(new HashSet<String>(Arrays.asList("x-requested-with", "Content-Type")));
  					corsfilter.setAllowedCredentials(false);
					
						return corsfilter;
					}
				}
			};
				
			component.getDefaultHost().attach("/", application);
			component.start();
		}catch(Exception e) {e.printStackTrace();}
				
	 	//Notify other services of our existence
	 	if(distributed_server != null){
	 		final int port_final = port;
	 		final String distributed_server_final = distributed_server;
	  		  		
	 		System.out.println("Starting distributed software restlet notification thread");

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
	 	
	 	//Connect to RabbitMQ bus
	 	if(rabbitmq_uri != null || rabbitmq_server != null){
	 		SoftwareServerRESTUtilities.rabbitMQHandler(last_username, last_password, port, applications, rabbitmq_uri, rabbitmq_server, rabbitmq_vhost, rabbitmq_username, rabbitmq_password, rabbitmq_WAITTOACK);
		}

		//A gap before message streams start.
		Utility.pause(1000);
		System.out.println();
	}
}

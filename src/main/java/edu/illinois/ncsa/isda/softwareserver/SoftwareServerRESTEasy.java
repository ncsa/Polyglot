package edu.illinois.ncsa.isda.softwareserver;
import edu.illinois.ncsa.isda.softwareserver.SoftwareServerAuxiliary.*;
import kgm.image.ImageUtility;
import kgm.utility.Utility;
import java.io.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.util.*;
import java.lang.management.ManagementFactory;
import java.net.*;
import org.apache.commons.io.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.Status;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.jboss.resteasy.plugins.server.tjws.TJWSEmbeddedJaxrsServer;
import javax.ws.rs.core.MultivaluedMap;

/**
 * A restful interface for a software server. Think of this as an extended software server.
 * @author Edgar Black
 */
public class SoftwareServerRESTEasy implements SoftwareServerRESTEasyInterface
{
	protected static TJWSEmbeddedJaxrsServer tjws;
	private static SoftwareServer server;
	private static Vector<Application> applications;
	private static Vector<TreeSet<TaskInfo>> application_tasks;
	private static TreeMap<String,Application> alias_map = new TreeMap<String,Application>();
	private static String public_path = "./";
	private static boolean ADMINISTRATORS_ENABLED = true;
	private static String download_method = "";
	
	/**
	 * Class constructor.
	 */
	public SoftwareServerRESTEasy()
	{
		server = new SoftwareServer("SoftwareServer.conf");
		applications = server.getApplications();
		application_tasks = Task.getApplicationTasks(applications);
		public_path = server.getTempPath() + "public/";
		
		if(!Utility.exists(public_path)){
			new File(public_path).mkdir();
		}
		
		// Build alias map
		for(int i = 0; i < applications.size(); i++){
			alias_map.put(applications.get(i).alias, applications.get(i));
		}
		
 		System.out.println("\nStarting distributed software restlet notification thread...\n");
	}

	/**
	 * Returns SoftwareServer greetings
	 * @return SoftwareServer greetings
	 */
	public Response WelcomeToSoftwareServer()
	{
		return Response.ok( SoftwareServerRESTUtilities.getApplicationStack(applications)).build();
	}

	/**
	 * Returns a list of applications available in software server
	 * @param uriInfo Basic URL information
	 * @param Accept content type accepted by client
	 * @return list of applications available in software server
	 */
	public Response listApplications(UriInfo uriInfo, String Accept)
	{
		if(Accept.equals("text/plain")){
			String response = getApplications();
			
			if (response.equals("")) {
				return Response.status(Status.NO_CONTENT).type("text/plain").build();
		  } else {
				return Response.ok(response, "text/plain").build();
		  }
		}else{
			String result = SoftwareServerRESTUtilities.createHTMLList(getApplications(), uriInfo.getAbsolutePath().toString().replaceAll("/$", "")	+ "/", true, "Software");
			return Response.ok(result, "text/html").build();
			// return Response.ok().entity(result).build();
		}
	}

	/**
	 * Returns a list of tasks performed by a particular application
	 * @param uriInfo Basic URL information
	 * @param Accept content type accepted by client
	 * @param app application's name
	 * @return list of tasks performed by a particular application
	 */
	public Response listTasks(UriInfo uriInfo, String Accept, String app)
	{
		if(Accept.equals("text/plain")){
			String response = getApplicationTasks(app);
			if (response.equals("")) {
				return Response.status(Status.NO_CONTENT).type("text/plain").build();
		  } else {
				return Response.ok(response, "text/plain").build();
		  }
		}else{
			String result = SoftwareServerRESTUtilities.createHTMLList(getApplicationTasks(app), uriInfo.getAbsolutePath().toString().replaceAll("/$", "") + "/", true, (uriInfo.getPath().toString()).substring(1));
			return Response.ok(result, "text/html").build();
		}
	}
	
	/**
	 * Return a list of the file formats produced by the application
	 * @param uriInfo: Basic URL information
	 * @param Accept content type accepted by client
	 * @param app application's name
	 * @param tsk task to be performed
	 * @return list of the file formats produced by the application
	 */
	public Response listOutputFmts(UriInfo uriInfo, String Accept, String app,String tsk)
	{	
		if(Accept.equals("text/plain")){
			String response=getApplicationTaskOutputs(app, tsk);
			
			if (response.equals("")) {
				return Response.status(Status.NO_CONTENT).type("text/plain").build();
		  } else {
				return Response.ok(response, "text/plain").build();
		  }
		}else{
			String result = SoftwareServerRESTUtilities.createHTMLList(getApplicationTaskOutputs(app, tsk), uriInfo.getAbsolutePath().toString().replaceAll("/$", "") + "/", true, (uriInfo.getPath().toString()).substring(1));
			return Response.ok(result, "text/html").build();
		}
	}

	/**
	 * Returns a list of input and output file formats accepted and produced by the application
	 * @param app application's name
	 * @param tsk task to be performed
	 * @return list of input and output file formats accepted and produced by the application
	 */
	public Response listInputAndOutputFormats(String app, String tsk)
	{
		return Response.ok().entity(getApplicationTaskInputsOutputs(app, tsk)).build();
	}

	/**
	 * Returns a list of input file formats accepted by the application
	 * @param uriInfo Basic URL information
	 * @param Accept content type accepted by client
	 * @param app application's name
	 * @param tsk task to be performed
	 * @return list of input file formats accepted by the application
	 */
	public Response listInputFormats(UriInfo uriInfo, String Accept, String app, String tsk)
	{
		if(Accept.equals("text/plain")){
			return Response.ok(getApplicationTaskInputs(app, tsk), "text/plain").build();
		}else{
			String result = SoftwareServerRESTUtilities.createHTMLList(getApplicationTaskInputs(app, tsk), uriInfo.getBaseUri().toString() + "form/post?application=" + app, false, (uriInfo.getPath().toString()).substring(1));
			return Response.ok(result, "text/html").build();
		}
	}

	/**
	 * Returns a link to the produced file
	 * @param uriInfo Basic URL information
	 * @param produces content type accepted by client
	 * @param app application's name
	 * @param tsk task to be performed
	 * @param fmt requested output format
	 * @param file input file to be processed 
	 * @return link to the produced file
	 */
	public Response listOutputFile(UriInfo uriInfo, String Accept, String app, String tsk, String fmt, String file)
	{
		if(Accept.equals("text/plain")){
			return Response.ok(listOutputFileHelper(app, tsk, file, fmt, uriInfo.getBaseUri().toString()),"text/plain").build();
		}else{
			String result = listOutputFileHelper(app, tsk, file, fmt, uriInfo.getBaseUri().toString());
			
			try{
				result = URLDecoder.decode(result, "UTF-8");
			}catch(UnsupportedEncodingException e){
				e.printStackTrace();
			}
			
			return Response.ok("<a href=\"" + result + "\">" + result + "</a>", "text/html").build();
		}
	}

	/**
	 * This is an auxiliary method.
	 * Returns a string with the name of the produced file
	 * @param application application's name
	 * @param fmt requested output format
	 * @param task task to be performed
	 * @param file input file to be processed 
	 * @param uri URL base information
	 * @return name of the produced file
	 */
	private String listOutputFileHelper(String application, String task, String file, String format, String uri)
	{
		int session = -1;
		String result;
		//String fileName = FilenameUtils.removeExtension(Paths.get(file).getFileName().toString());
		String fileName = FilenameUtils.removeExtension(Utility.getFilename(file));

		if(file.startsWith(uri)){ // Locally cached files already have session ids
			session = SoftwareServer.getSession(file);
			result = uri + "file/" + fileName + "." + format;
		}else{ 										// Remote files must be assigned a session id
			session = server.getSession();
			result = Utility.endSlash(uri) + "file/" + session + "_" + fileName + "." + format;
		}
		
		executeTaskLater(session, application, task, file, format, uri);
		
		return result;
	}

	/**
	 * Returns a list of actions that can be performed (get or post or convert) 
	 * @return list of actions that can be performed
	 */
	public Response form()
	{
		String result = "get\n";
		result += "post\n";
		result += "convert";
		
		return Response.ok(result).build();
	}

	/**
	 * Returns a link to the produced file
	 * @param uri Basic URL information
	 * @param application application's name
	 * @param task task to be performed
	 * @param format requested output format
	 * @param file input file to be processed 
	 * @param action (get or post or convert)
	 * @return a link to the produced file
	 */
	public Response processGet(UriInfo uri, String application, String task, String format, String file, String action)
	{
		String url = null;
		if(application.equals("")) application = null;
		if(task.equals("")) task = null;
		if(file.equals("")) file = null;
		if(format.equals("")) format = null;

		if(application != null && task != null && file != null && format != null){
			try{
				url = uri.getBaseUri().toString() + "software/" + application + "/"	+ task + "/" + format + "/" + URLEncoder.encode(file, "UTF-8");
			}catch(UnsupportedEncodingException e){
				e.printStackTrace();
			}
			
			return Response.ok("<html><head><meta http-equiv=\"refresh\" content=\"1; url=" + url	+ "\"></head></html>").build();
		}else{
			if(action.equals("get")){
				return Response.ok( SoftwareServerRESTUtilities.getForm(applications, application_tasks, alias_map, false, application, application != null)).build();
			}else if(action.equals("post")){
				return Response.ok(SoftwareServerRESTUtilities.getForm(applications, application_tasks, alias_map,true, application, application != null)).build();
			}else if(action.equals("convert")){
				return Response.ok(SoftwareServerRESTUtilities.getConvertForm(applications,application_tasks)).build();
			}else{
				return printErrorMessage(action);
			}
		}
	}

	/**
	 * Returns a link to either a produced file or to a directory tree
	 * @param uri Basic URL information
	 * @param fileOrDirName input file or directory
	 * @return a link to either a produced file or to a directory tree
	 */
	public Response fileOrDir(UriInfo uri, String fileOrDirName) throws IOException
	{
		File fileOrDir = new File(public_path + fileOrDirName);
		String url;
		String media_type = null;

		if(fileOrDir.exists()){
			if(fileOrDir.isDirectory()){ // processing a directory
				url = uri.getBaseUri().toString() + "file/" + fileOrDirName + "/";

				// String file = public_path + fileOrDirName;
				// file += "/" + fileOrDirName;
				// Utility.save(file,
				// "<html><head><meta http-equiv=\"refresh\" content=\"1; url=" + url +
				// "\"></head></html>");
				// FileUtils.writeStringToFile(new File(file),
				// "<html><head><meta http-equiv=\"refresh\" content=\"1; url=" + url +
				// "\"></head></html>");

				String myRes = "<html><body style=\"font-family: sans-serif;\">\n";
				myRes += "<h2>Listing of \"" + "/file/" + fileOrDirName + "/"	+ "\"</h2>\n";
				myRes += "<a href=\"" + url + "\">.</a><br>\n";
				myRes += "<a href=\""	+ url.substring(0, url.lastIndexOf('/', url.length() - 2)) + "\">..</a><br>\n";
				File[] filesList = fileOrDir.listFiles();
				
				for(File files:filesList){
					myRes += "<a href=\"" + url;
					
					if(files.isFile()){
						myRes += files.getName() + "\">" + files.getName();
					}else{
						myRes += files.getName() + "/\">" + files.getName() + "/";
					}
					
					myRes += "</a><br>\n";
				}
				
				myRes += "</body></html>";

				return Response.ok(myRes, "text/html").build();
			}else{ // processing a file
				Path path = Paths.get(public_path + fileOrDirName);
				media_type = Files.probeContentType(path);
				
				if (Files.size(path) > 0 ) {
					if(media_type != null){
						return Response.ok(fileOrDir, media_type).build();
					}else{
						return Response.ok(fileOrDir).build();
					}
				} else {
					return Response.status(Status.NO_CONTENT).type(media_type).build();
				}
			}
		}else{
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
		
		/*
		 * // this is to look for image in images directory File file = new
		 * File("images/" + fileName); if(!file.exists() || file.isDirectory()){
		 * file = new File(imgDefault); } // end if // return
		 * Response.ok().entity((Object)file).build();
		 */
		
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
	public Response alive()
	{
		return Response.ok("yes").build();
	}

	/**
	 * Used to verify if the server is busy
	 * @return true if the server is processing at least one request, false if not. 
	 */
	public Response busy()
	{
		return Response.ok("" + server.isBusy()).build();
	}

	/**
	 * Used to know the number of processor available to the server 
	 * @return number of processors available to the server 
	 */
	public Response processors()
	{
		return Response.ok("" + Runtime.getRuntime().availableProcessors()).build();
	}

	/**
	 * Used to know the memory available to the server 
	 * @return memory available to the server (is this right????)  
	 */
	public Response memory()
	{
		return Response.ok("" + Runtime.getRuntime().maxMemory()).build();
	}

	/**
	 * Used to get a measure of the server load
	 * @return measure of the server load  
	 */
	public Response load()
	{
		return Response.ok("" + ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage()).build(); 
	}

	/**
	 * Used to know the number of task processed since the server starts
	 * @return number of task since the server starts
	 */
	public Response tasks()
	{
		return Response.ok("" + server.getTaskCount()).build();
	}

	/**
	 * Used to know the number of task killed since the server starts
	 * @return number of task killed since the server starts
	 */
    public Response kills()
	{
		return Response.ok("" + server.getKillCount()).build();
	}

	/**
	 * Used to know the number of task completed since the server starts
	 * @return number of task completed since the server starts
	 */
	public Response completedTasks()
	{
		return Response.ok("" + server.getCompletedTaskCount()).build();
	}

	/**
	 * Used to have a look at the server screen and determine if the
	 * server must be restarted due to hang application.
	 * @return number an image of the server screen (only authorized users)
	 */
	public Response screen()
	{ 
		// only if user is ADMINISTRATORS_ENABLED
		if(ADMINISTRATORS_ENABLED){
			ImageUtility.save(public_path + "screen.jpg", ImageUtility.getScreen());
			return Response.ok((Object)new File(public_path + "screen.jpg"), "image/jpg").build();
		}else{
			return printErrorMessage("screen");
		}
	}

	/**
	 * Used to reset the server remotely in case it is necessary
	 * @return ok after reseting the server. (only authorized users)
	 */
	public Response reset()
	{ 
		// only if user is ADMINISTRATORS_ENABLED
		if(ADMINISTRATORS_ENABLED){
			server.resetCounts();
			return Response.ok("ok").build();
		}else{
			return printErrorMessage("reset");
		}
	}

	/**
	 * Used to reboot the server remotely in case it is necessary
	 * @return ok after rebooting the server. (only authorized users)
	 */
	public Response reboot()
	{ 
		// only if user is ADMINISTRATORS_ENABLED
		if(ADMINISTRATORS_ENABLED){
			server.rebootMachine();
			return Response.ok("ok").build();
		}else{
			return printErrorMessage("reboot");
		}
	}

	/**
	 * Returns an error message 
	 * @param msg String to be returned within the error message
	 * @return error message
	 */
	public Response printErrorMessage(String msg)
	{
		String result = "SoftwareServer message : " + msg + " is an invalid endpoint.";
		//return Response.status(Status.BAD_REQUEST).entity(result).build();
		return Response.status(Status.BAD_REQUEST).entity(result).type("text/plain").build();
	}

	/**
	 * Process a file submitted via the web form and return a link to the produced file
	 * @param input Form containing Input Data
	 * @param mediaType media type being processed 
	 * @param accept content type accepted by client
	 * @param uri Basic URL information
	 * @param application application's name
	 * @param task task to be performed
	 * @param format requested output format
	 * @param file name of the input file to be processed 
	 * @return a link to the produced file
	 */
	public Response formPost(MultipartFormDataInput input, String mediaType, String accept, UriInfo uri, String application, String task,String format, String file)	
	{
		return processPost(input, mediaType, accept, uri, application, task,format, file, true);
	}

	/**
	 * Process a file submitted to the Software Server and return a link to the produced file
	 * @param input Form containing Input Data
	 * @param mediaType media type being processed 
	 * @param accept content type accepted by client
	 * @param uri Basic URL information
	 * @param application application's name
	 * @param task task to be performed
	 * @param format requested output format
	 * @return a link to the produced file
	 */
	public Response taskPost(MultipartFormDataInput input, String mediaType,String accept, UriInfo uri, String application, String task, String format)
	{
		return processPost (input,mediaType,accept,uri,application,task,format,"",false);
	}

	/**
	 * This is an auxiliary method working for the formPost() and the taskPost() methods. Returns the produced file
	 * @param input Form containing Input Data
	 * @param mediaType media type being processed 
	 * @param accept content type accepted by client
	 * @param uri Basic URL information
	 * @param application application's name
	 * @param task task to be performed
	 * @param format requested output format
	 * @param file name of the input file to be processed 
	 * @param fromPost true if message comes from formPost; false otherwise
	 * @return the produced file
	 */
	public Response processPost(MultipartFormDataInput input, String mediaType, String accept, UriInfo uri, String application, String task, String format, String file, boolean formPost)
	{
		String searchFor = null, result="";
		String fileName;
		int session = server.getSession();

		if(application.equals("")) application = null;
		if(task.equals("")) task = null;
		if(file.equals("")) file = null;
		if(format.equals("")) format = null;

		// in this method, TASK_POST will always be false, need to ask about additional elements in path: !part2.isEmpty() && !part3.isEmpty(); ????

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
						file = uri.getBaseUri().toString() + "file/" + session + "_" + fileName;
						
						// Handle the body of that part with an InputStream
						try{
							InputStream istream = inputPart.getBody(InputStream.class, null);
							fileName = server.getCachePath() + session + "_" + URLDecoder.decode(fileName,"UTF-8");
							saveFile(istream, fileName);
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
			application = parameters.get("application");
			task = parameters.get("task");
			format = parameters.get("format");
		}

		if(application != null && task != null && file != null && format != null){
			try{
				file = URLDecoder.decode(file,"UTF-8");
			}catch(UnsupportedEncodingException e1){
				e1.printStackTrace();
			}
			
			executeTaskLater(session, application, task, file, format, uri.getBaseUri().toString());
			result = uri.getBaseUri().toString() + "file/" + Utility.getFilenameName(file) + "." + format;
			
			if(accept.startsWith("text/plain")){
				return Response.ok(result, "text/plain").build();
			}else{
				return Response.ok("<a href=\"" + result + "\">" + result + "</a>").build();
			}
		}
		
		return processGet(uri, application, task, format, file, "post"); // this is temporal
	}

	/**
	 * This is an auxiliary method working for the processPost() method
	 * @param uploadedInputStream input stream
	 * @param serverLocation filename
	 */
	private void saveFile(InputStream uploadedInputStream, String serverLocation)
	{
		// save uploaded file to a defined location on the server
		try{
			OutputStream outpuStream = new FileOutputStream(new File(serverLocation));
			int read = 0;
			byte[] bytes = new byte[1024];
			
			while((read = uploadedInputStream.read(bytes)) != -1){
				outpuStream.write(bytes, 0, read);
			}
			
			outpuStream.flush();
			outpuStream.close();
		}catch(IOException e){
			e.printStackTrace();
		}
	}

	////////////////////////////////////////////////////////////////////////////////////
	// From now on all the methods were taken directly from SoftwareServerReslet.java //
	////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Get the applications available.
	 * @return the applications
	 */
	public String getApplications()
	{
		String buffer = "";

		for(int i = 0; i < applications.size(); i++){
			buffer += applications.get(i).alias + " (" + applications.get(i).name	+ ")\n";
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

		for(int i = 0; i < applications.size(); i++){
			if(applications.get(i).alias.equals(alias)){
				for(Iterator<TaskInfo> itr = application_tasks.get(i).iterator(); itr.hasNext();){
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

		for(int i = 0; i < applications.size(); i++){
			if(applications.get(i).alias.equals(alias)){
				for(Iterator<TaskInfo> itr1 = application_tasks.get(i).iterator(); itr1.hasNext();){
					task_info = itr1.next();

					if(task_info.name.equals(task)){
						for(Iterator<String> itr2 = task_info.outputs.iterator(); itr2.hasNext();){
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

		for(int i = 0; i < applications.size(); i++){
			if(applications.get(i).alias.equals(alias)){
				for(Iterator<TaskInfo> itr1 = application_tasks.get(i).iterator(); itr1.hasNext();){
					task_info = itr1.next();

					if(task_info.name.equals(task)){
						FIRST_VALUE = true;

						for(Iterator<String> itr2 = task_info.inputs.iterator(); itr2.hasNext();){
							if(FIRST_VALUE)
								FIRST_VALUE = false;
							else
								buffer += ", ";
							
							buffer += itr2.next();
						}

						buffer += "\n";
						FIRST_VALUE = true;

						for(Iterator<String> itr2 = task_info.outputs.iterator(); itr2.hasNext();){
							if(FIRST_VALUE)
								FIRST_VALUE = false;
							else
								buffer += ", ";
							
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
	 * Get the input formats supported by the given task.
	 * @param alias the application alias
	 * @param task the application task
	 * @return the input formats supported
	 */
	public String getApplicationTaskInputs(String alias, String task)
	{
		TaskInfo task_info;
		String buffer = "";

		for(int i = 0; i < applications.size(); i++){
			if(applications.get(i).alias.equals(alias)){
				for(Iterator<TaskInfo> itr1 = application_tasks.get(i).iterator(); itr1.hasNext();){
					task_info = itr1.next();

					if(task_info.name.equals(task)){
						for(Iterator<String> itr2 = task_info.inputs.iterator(); itr2.hasNext();){
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
	public synchronized void executeTask(int session, String application_alias, String task_string, String file, String format, String url)
	{
		// this is the new executeTask, have to make it work //
		Vector<Subtask> task;
		String result;
		String extension = SoftwareServerRESTUtilities.removeParameters(Utility.getFilenameExtension(file));

		String fileName = Utility.getFilenameName(file);

		if(session >= 0){
			if(file.startsWith(url + "file/")){ // Remove session id from filenames of locally cached files
				file = SoftwareServer.getFilename(Utility.getFilename(file));
			}else{ 															// Download remote files
				String inFilename = "/" + session + "_" + fileName + "." + extension;
				if(download_method.equals("wget")){
					try{
						Runtime.getRuntime().exec("wget -O " + server.getCachePath() + inFilename + " "+ file).waitFor();
					}catch(Exception e){
						e.printStackTrace();
					}
				}else if(download_method.equals("nio")){
					try{
						URL website = new URL(file);
						ReadableByteChannel rbc = Channels.newChannel(website.openStream());
						FileOutputStream fos = new FileOutputStream(server.getCachePath()	+ inFilename);
						fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
						fos.close(); // added by Edgar Black
					}catch(Exception e){
						e.printStackTrace();
					}
				}else{
					try{
						inFilename = inFilename.substring(1);
						inFilename = URLDecoder.decode(inFilename, "UTF-8");

						String myFile = server.getCachePath() + inFilename;

						inFilename = file.substring(file.lastIndexOf('/') + 1);
						inFilename = URLDecoder.decode(inFilename, "UTF-8");
						inFilename = URLEncoder.encode(inFilename, "UTF-8").replace("+","%20");

						file = file.substring(0, file.lastIndexOf('/') + 1) + inFilename;
						
						try{
							URL website = new URL(file);
							ReadableByteChannel rbc = Channels.newChannel(website.openStream());
							FileOutputStream fos = new FileOutputStream(myFile);
							fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
							fos.close(); // added by Edgar Black
						}catch(MalformedURLException e){
							//e.printStackTrace();
						}
					}catch(Exception e){
						e.printStackTrace();
					}
					
					// Utility.downloadFile(server.getCachePath(), inFilename , file);
				}
				
				// file = removeParameters(Utility.getFilename(file));
				try{
					file = URLDecoder.decode(inFilename, "UTF-8");
				}catch(UnsupportedEncodingException e){
					e.printStackTrace();
				}
			}

			task = getTask(application_alias, task_string, file, format);
			// Task.print(task, applications);

			result = server.executeTaskAtomically("localhost", session, task);

			// Create empty output if not created (e.g. when no conversion path was found)
			if(result == null){
				result = server.getCachePath() + session + "_"
						+ Utility.getFilenameName(file) + "." + format;
				Utility.touch(result);
			}

			// Move result to public folder
			if(!Utility.isDirectory(result)){
				Utility.copyFile(result, public_path + Utility.getFilename(result));
			}else{
				try{
					FileUtils.copyDirectory(new File(result), new File(public_path + Utility.getFilename(result)));
				}catch(Exception e){
					e.printStackTrace();
				}

				// This was commented out by Edgar Black. Is this ok? It seems to be working.....
				// Attach directory as a new endpoint
				// Directory directory = new Directory(getContext(), "file://" +
				// Utility.absolutePath(public_path + Utility.getFilename(result)));
				// directory.setListingAllowed(true);
				// component.getDefaultHost().attach("/file/" +
				// Utility.getFilename(result) + "/", directory);
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
	public void executeTaskLater(int session, String application_alias, String task_string, String file, String format, String uri)
	{
		final int session_final = session;
		final String application_alias_final = application_alias;
		final String task_string_final = task_string;
		final String file_final = file;
		final String format_final = format;
		final String uri_final = uri;

		new Thread() {
			public void run() {
				executeTask(session_final, application_alias_final, task_string_final, file_final, format_final, uri_final);
			}
		}.start();
	}

	
	/**
	 * Used to stop the servlet server
	 */
	public void stop()
	{
	  try{
	  	tjws.stop();
	  }catch(Exception e) {e.printStackTrace();}
	}


	/**
	 * Used to initialize and start the servlet server called the Tiny Java Web Server and Servlet (TJWS) Container.
	 * @param args the input arguments
	 */
	public static void main(String[] args)
	{
		tjws = new TJWSEmbeddedJaxrsServer();
		tjws.setPort(8182);
		tjws.start();
		//tjws.getDeployment().getRegistry().addPerRequestResource(ResteasyResources.class);
		tjws.getDeployment().getRegistry().addSingletonResource(new SoftwareServerRESTEasy());
	}
}

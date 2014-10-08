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
	 * Constructor:   SoftwareServerRESTEasy.
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
	 * Method:   Welcome.
	 * @returns: SoftwareServer greetings
	 */
	public Response WelcomeToSoftwareServer()
	{
		return Response.ok(getApplicationStack()).build();
	}

	/**
	 * Method: listApplications.
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
			String result = createHTMLList(getApplications(), uriInfo.getAbsolutePath().toString().replaceAll("/$", "")	+ "/", true, "Software");
			return Response.ok(result, "text/html").build();
			// return Response.ok().entity(result).build();
		}
	}

	/**
	 * Method: listTasks.
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
			String result = createHTMLList(getApplicationTasks(app), uriInfo.getAbsolutePath().toString().replaceAll("/$", "") + "/", true, (uriInfo.getPath().toString()).substring(1));
			return Response.ok(result, "text/html").build();
		}
	}
	
	/**
	 * Method: listOutputFmts.
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
			String result = createHTMLList(getApplicationTaskOutputs(app, tsk), uriInfo.getAbsolutePath().toString().replaceAll("/$", "") + "/", true, (uriInfo.getPath().toString()).substring(1));
			return Response.ok(result, "text/html").build();
		}
	}

	/**
	 * Method: listInputAndOutputFormats.
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
	 * Method: listInputFormats.
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
			String result = createHTMLList(getApplicationTaskInputs(app, tsk), uriInfo.getBaseUri().toString() + "form/post?application=" + app, false, (uriInfo.getPath().toString()).substring(1));
			return Response.ok(result, "text/html").build();
		}
	}

	/**
	 * Method: listOutputFile.
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
	 * Method: listOutputFileHelper.
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
	 * Method: form.
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
	 * Method: processGet.
	 * Returns a link to the produced file
	 * @param uriInfo Basic URL information
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
				return Response.ok(getForm(false, application, application != null)).build();
			}else if(action.equals("post")){
				return Response.ok(getForm(true, application, application != null)).build();
			}else if(action.equals("convert")){
				return Response.ok(getConvertForm()).build();
			}else{
				return printErrorMessage(action);
			}
		}
	}

	/**
	 * Method: fileOrDir.
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
	 * Method: appIcons.
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
	 * Method: alive.
	 * Used to verify if the server is responding
	 * @return yes if the server is alive 
	 */
	public Response alive()
	{
		return Response.ok("yes").build();
	}

	/**
	 * Method: busy.
	 * Used to verify if the server is busy
	 * @return true if the server is processing at least one request, false if not. 
	 */
	public Response busy()
	{
		return Response.ok("" + server.isBusy()).build();
	}

	/**
	 * Method: processors.
	 * Used to know the number of processor available to the server 
	 * @return number of processors available to the server 
	 */
	public Response processors()
	{
		return Response.ok("" + Runtime.getRuntime().availableProcessors()).build();
	}

	/**
	 * Method: memory.
	 * Used to know the memory available to the server 
	 * @return memory available to the server (is this right????)  
	 */
	public Response memory()
	{
		return Response.ok("" + Runtime.getRuntime().maxMemory()).build();
	}

	/**
	 * Method: load.
	 * Used to get a measure of the server load
	 * @return measure of the server load  
	 */
	public Response load()
	{
		return Response.ok("" + ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage()).build(); 
	}

	/**
	 * Method: tasks.
	 * Used to know the number of task processed since the server starts
	 * @return number of task since the server starts
	 */
	public Response tasks()
	{
		return Response.ok("" + server.getTaskCount()).build();
	}

	/**
	 * Method: kills.
	 * Used to know the number of task killed since the server starts
	 * @return number of task killed since the server starts
	 */
    public Response kills()
	{
		return Response.ok("" + server.getKillCount()).build();
	}

	/**
	 * Method: completedTasks.
	 * Used to know the number of task completed since the server starts
	 * @return number of task completed since the server starts
	 */
	public Response completedTasks()
	{
		return Response.ok("" + server.getCompletedTaskCount()).build();
	}

	/**
	 * Method: screen.
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
	 * Method: reset.
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
	 * Method: reboot.
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
	 * Method: printErrorMessage.
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
	 * Method: formPost.
	 * Returns a link to the produced file
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
	 * Method: taskPost.
	 * Returns a link to the produced file
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
	 * Method: processPost.
	 * This is an auxiliary method working for the formPost() and the taskPost() methods.
	 * Returns the produced file
	 * @param input Form containing Input Data
	 * @param mediaType media type being processed 
	 * @param accept content type accepted by client
	 * @param uriInfo Basic URL information
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
	 * Method: saveFile. This is an auxiliary method working for the processPost() method.
	 * @param uploadedInputStream input stream
	 * @param serverLocation filename
	 * @return void
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
	 * Get an icon representation of the available software.
	 * @return the HTML for the icon representation
	 */
	public String getApplicationStack()
	{
		String buffer = "";

		for(int i = 0; i < applications.size(); i++){
			buffer += "<div style=\"float:left\">\n";
			buffer += "<table>\n";
			buffer += "<tr><td align=\"center\">\n";
			buffer += "<a href=\"form/get?application=" + applications.get(i).alias + "\">";
			buffer += "<img src=\"image/" + applications.get(i).alias + ".jpg\" width=\"50\" border=\"0\">";
			buffer += "</a>\n";
			buffer += "</td></tr><tr><td align=\"center\">\n";
			buffer += "<font size=\"-1\">" + applications.get(i).name + "</font>\n";
			buffer += "</td></tr>\n";
			buffer += "</table>\n";
			buffer += "</div>\n";
			buffer += "\n";
		}

		// Add ping
		if(true){
			buffer += "<i><font size=\"-1\" color=\"#777777\"><div name=\"ping\" id=\"ping\" style=\"position:absolute;bottom:0\"></div></font></i>\n";
			buffer += "\n";
			buffer += "<script type=\"text/javascript\" src=\"https://ajax.googleapis.com/ajax/libs/jquery/1.5.2/jquery.js\"></script>\n";
			buffer += "<script type=\"text/javascript\">\n";
			buffer += "function refreshPing(){\n";
			buffer += "  $.ajax({\n";
			buffer += "    beforeSend: function(){\n";
			buffer += "      window.startTime = new Date();\n";
			buffer += "    },\n";
			buffer += "    \n";
			buffer += "    url: '/',\n";
			buffer += "    \n";
			buffer += "    success: function(){\n";
			buffer += "      window.endTime = new Date();\n";
			// buffer += "      document.getElementById('ping').innerHTML = window.endTime - window.startTime + \" ms\";\n";
			buffer += "      document.getElementById('ping').innerHTML = \"" + Runtime.getRuntime().availableProcessors() + " cores, \" + (window.endTime - window.startTime) + \" ms\";\n";
			buffer += "    }\n";
			buffer += "  });\n";
			buffer += "  \n";
			buffer += "  setTimeout('refreshPing()', 1000);\n";
			buffer += "}\n";
			buffer += "\n";
			buffer += "refreshPing();\n";
			buffer += "</script>\n";
		}

		return buffer;
	}

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
		String extension = removeParameters(Utility.getFilenameExtension(file));

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

			result = server.executeTask("localhost", session, task);

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
	 * Get a web form interface for this restful service.
	 * @param POST_UPLOADS true if this form should use POST rather than GET for uploading files
	 * @param selected_application the default application
	 * @param HIDE_APPLICATIONS true if applications menu should be hidden
	 * @return the form
	 */
	public String getForm(boolean POST_UPLOADS, String selected_application, boolean HIDE_APPLICATIONS)
	{
		String buffer = "";
		String format;
		Application application;
		TaskInfo task_info;
		int count;
		boolean FIRST_BLOCK;
		boolean FIRST_VALUE;

		buffer += "<script type=\"text/javascript\">\n";
		buffer += "function setTasks(){\n";
		buffer += "  var select = document.getElementById('application');\n";
		buffer += "  var application = select.options[select.selectedIndex].value;\n";
		buffer += "  var tasks = document.getElementById('task');\n";
		buffer += "  \n";
		buffer += "  tasks.options.length = 0;\n";
		buffer += "  \n";

		for(int i = 0; i < applications.size(); i++){
			if(i > 0) buffer += "\n";
			buffer += "  if(application == \"" + applications.get(i).alias + "\"){\n";
			count = 0;

			for(Iterator<TaskInfo> itr = application_tasks.get(i).iterator(); itr.hasNext();){
				task_info = itr.next();
				buffer += "    tasks.options[" + count + "] = new Option(\"" + task_info.name + "\", \"" + task_info.name + "\", " + (count == 0) + ", " + (count == 0) + ");\n";
				count++;
			}

			buffer += "  }\n";
		}

		buffer += "  \n";
		buffer += "  setFormats();\n";
		buffer += "}\n";
		buffer += "\n";
		buffer += "function setFormats(){\n";
		buffer += "  var select = document.getElementById('application');\n";
		buffer += "  var application = select.options[select.selectedIndex].value;\n";
		buffer += "  var select = document.getElementById('task');\n";
		buffer += "  var task = select.options[select.selectedIndex].value;\n";
		buffer += "  var inputs = document.getElementById('inputs');\n";
		buffer += "  var outputs = document.getElementById('format');\n";
		buffer += "  \n";
		buffer += "  inputs.innerHTML = \"\";\n";
		buffer += "  outputs.options.length = 0;\n";
		buffer += "  \n";

		FIRST_BLOCK = true;

		for(int i = 0; i < applications.size(); i++){
			for(Iterator<TaskInfo> itr1 = application_tasks.get(i).iterator(); itr1.hasNext();){
				task_info = itr1.next();
				if(FIRST_BLOCK)
					FIRST_BLOCK = false;
				else
					buffer += "\n";
				
				buffer += "  if(application == \"" + applications.get(i).alias + "\" && task == \"" + task_info.name + "\"){\n";
				buffer += "    inputs.innerHTML = \"";
				FIRST_VALUE = true;

				for(Iterator<String> itr2 = task_info.inputs.iterator(); itr2.hasNext();){
					if(FIRST_VALUE)
						FIRST_VALUE = false;
					else
						buffer += ", ";
					
					buffer += itr2.next();
				}

				buffer += "\";\n";
				count = 0;

				for(Iterator<String> itr2 = task_info.outputs.iterator(); itr2.hasNext();){
					format = itr2.next();
					buffer += "    outputs.options[" + count + "] = new Option(\"" + format + "\", \"" + format + "\", false, false);\n";
					count++;
				}

				buffer += "  }\n";
			}
		}

		buffer += "}\n";
		buffer += "\n";
		buffer += "function setAPICall(){\n";
		buffer += "  var select = document.getElementById('application');\n";
		buffer += "  var application = select.options[select.selectedIndex].value;\n";
		buffer += "  var select = document.getElementById('task');\n";
		buffer += "  var task = select.options[select.selectedIndex].value;\n";
		buffer += "  var file = document.getElementById('file').value;\n";
		buffer += "  var select = document.getElementById('format');\n";
		buffer += "  var format = select.options[select.selectedIndex].value;\n";

		if(!POST_UPLOADS){
			buffer += "  var api_url = \"http://\" + location.host + \"/software/\" + application + \"/\" + task + \"/\" + format + \"/\" + encodeURIComponent(file);\n";
			buffer += "  var api_html = \"http://\" + location.host + \"/software/<font color=\\\"#ff7777\\\">\" + application + \"</font>/<font color=\\\"#77ff77\\\">\" + task + \"</font>/<font color=\\\"#7777ff\\\">\" + format + \"</font>/<font color=\\\"#777777\\\">\" + encodeURIComponent(file) + \"</font>\";\n";
		}else{
			buffer += "  var api_url = \"http://\" + location.host + \"/software/\" + application + \"/\" + task + \"/\" + format + \"/\";\n";
			buffer += "  var api_html = \"http://\" + location.host + \"/software/<font color=\\\"#ff7777\\\">\" + application + \"</font>/<font color=\\\"#77ff77\\\">\" + task + \"</font>/<font color=\\\"#7777ff\\\">\" + format + \"</font>/\";\n";
		}

		buffer += "  \n";
		buffer += "  api.innerHTML = \"<i><b><font color=\\\"#777777\\\">RESTful API call</font></b><br><br><a href=\\\"\" + api_url + \"\\\"style=\\\"text-decoration:none; color:#777777\\\">\" + api_html + \"</a></i>\";\n";
		buffer += "  setTimeout('setAPICall()', 500);\n";
		buffer += "}\n";
		buffer += "</script>\n";
		buffer += "\n";
		buffer += "<center>\n";

		if(!POST_UPLOADS){
			buffer += "<form name=\"converson\" action=\"\" method=\"get\">\n";
		}else{
			buffer += "<form enctype=\"multipart/form-data\" name=\"converson\" action=\"\" method=\"post\">\n";
		}

		buffer += "<table>\n";

		if(selected_application != null && HIDE_APPLICATIONS){
			application = alias_map.get(selected_application);
			buffer += "<tr><td>";
			if(application.icon != null) buffer += "<img src=\"../image/" + application.alias + ".jpg\" width=\"50\" border=\"0\">";
			buffer += "</td><td><h2>" + application.name + "</h2></td></tr>\n";
		}

		buffer += "<tr><td>";
		if(!HIDE_APPLICATIONS) buffer += "<b>Application:</b>";
		buffer += "</td>\n";
		buffer += "<td><select name=\"application\" id=\"application\" onchange=\"setTasks();\"";
		if(HIDE_APPLICATIONS) buffer += " style=\"visibility:hidden;\"";
		buffer += ">\n";

		for(int i = 0; i < applications.size(); i++){
			buffer += "<option value=\"" + applications.get(i).alias + "\"";

			if(selected_application != null && selected_application.equals(applications.get(i).alias)){
				buffer += " selected";
			}

			buffer += ">" + applications.get(i) + "</option>\n";
		}

		buffer += "</select></td></tr>\n";
		buffer += "<tr><td><b>Task:</b></td>\n";
		buffer += "<td><select name=\"task\" id=\"task\" onchange=\"setFormats();\">\n";

		for(Iterator<TaskInfo> itr = application_tasks.get(0).iterator(); itr.hasNext();){
			task_info = itr.next();
			buffer += "<option value=\"" + task_info.name + "\">" + task_info.name + "</option>\n";
		}

		buffer += "</select></td></tr>\n";
		buffer += "<tr><td><td width=\"100\"><i><font size=\"-1\"><div id=\"inputs\">";
		FIRST_VALUE = true;

		for(Iterator<String> itr = application_tasks.get(0).first().inputs.iterator(); itr.hasNext();){
			if(FIRST_VALUE)
				FIRST_VALUE = false;
			else
				buffer += ", ";
			
			buffer += itr.next();
		}

		buffer += "</div></font></i></td></tr>\n";

		if(!POST_UPLOADS){
			buffer += "<tr><td><b>File URL:</b></td><td><input type=\"text\" name=\"file\" id=\"file\" size=\"100\"></td></tr>\n";
		}else{
			buffer += "<tr><td><b>File:</b></td><td><input type=\"file\" name=\"file\" id=\"file\" size=\"100\"></td></tr>\n";
		}

		buffer += "<tr><td><b>Format:</b></td>\n";
		buffer += "<td><select name=\"format\" id=\"format\">\n";

		for(Iterator<String> itr = application_tasks.get(0).first().outputs.iterator(); itr.hasNext();){
			format = itr.next();
			buffer += "<option value=\"" + format + "\">" + format + "</option>\n";
		}

		buffer += "</select></td></tr>\n";
		buffer += "<tr><td></td><td><input type=\"submit\" value=\"Submit\"></td></tr>\n";
		buffer += "<tr><td height=\"25\"></td><td></td></tr>\n";
		buffer += "<tr><td></td><td align=\"center\"><div name=\"api\" id=\"api\"></div></td></tr>\n";
		buffer += "</table>\n";
		buffer += "</form>\n";
		buffer += "</center>\n";
		buffer += "\n";
		buffer += "<script type=\"text/javascript\">setAPICall();</script>\n";

		if(selected_application != null){
			buffer += "<script type=\"text/javascript\">setTasks();</script>\n";
		}

		return buffer;
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
		TaskInfo convert_task;
		String buffer = "";
		String format;
		int count;
		boolean FIRST_VALUE;

		buffer += "<script type=\"text/javascript\">\n";
		buffer += "function setFormats(){\n";
		buffer += "  var select = document.getElementById('application');\n";
		buffer += "  var application = select.options[select.selectedIndex].value;\n";
		buffer += "  var inputs = document.getElementById('inputs');\n";
		buffer += "  var outputs = document.getElementById('format');\n";
		buffer += "  \n";
		buffer += "  inputs.innerHTML = \"\";\n";
		buffer += "  outputs.options.length = 0;\n";
		buffer += "  \n";

		for(int i = 0; i < applications.size(); i++){
			convert_task = TaskInfo.getTask(application_tasks.get(i), "convert");

			if(convert_task != null){
				if(i > 0) buffer += "\n";
				buffer += "  if(application == \"" + applications.get(i).alias + "\"){\n";
				buffer += "    inputs.innerHTML = \"";
				FIRST_VALUE = true;

				for(Iterator<String> itr = convert_task.inputs.iterator(); itr.hasNext();){
					if(FIRST_VALUE)
						FIRST_VALUE = false;
					else
						buffer += ", ";
					
					buffer += itr.next();
				}

				buffer += "\";\n";
				count = 0;

				for(Iterator<String> itr = convert_task.outputs.iterator(); itr.hasNext();){
					format = itr.next();
					buffer += "    outputs.options[" + count + "] = new Option(\"" + format + "\", \"" + format + "\", false, false);\n";
					count++;
				}

				buffer += "  }\n";
			}
		}

		buffer += "}\n";
		buffer += "</script>\n\n";

		buffer += "<form name=\"converson\" action=\"form/\" method=\"get\">\n";
		buffer += "<input type=\"hidden\" name=\"task\" value=\"convert\">\n";
		buffer += "<table>\n";
		buffer += "<tr><td><b>Application:</b></td>\n";
		buffer += "<td><select name=\"application\" id=\"application\" onchange=\"setFormats();\">\n";

		for(int i = 0; i < applications.size(); i++){
			buffer += "<option value=\"" + applications.get(i).alias + "\">" + applications.get(i) + "</option>\n";
		}

		buffer += "</select></td></tr>\n";
		buffer += "<tr><td><td width=\"100\"><i><font size=\"-1\"><div id=\"inputs\">";

		convert_task = TaskInfo.getTask(application_tasks.get(0), "convert");

		if(convert_task != null){
			FIRST_VALUE = true;

			for(Iterator<String> itr = convert_task.inputs.iterator(); itr.hasNext();){
				if(FIRST_VALUE)
					FIRST_VALUE = false;
				else
					buffer += ", ";
				
				buffer += itr.next();
			}
		}

		buffer += "</div></font></i></td></tr>\n";
		buffer += "<tr><td><b>File:</b></td><td><input type=\"text\" name=\"file\" size=\"100\"></td></tr>\n";
		buffer += "<tr><td><b>Format:</b></td>\n";
		buffer += "<td><select name=\"format\" id=\"format\">\n";

		if(convert_task != null){
			for(Iterator<String> itr = convert_task.outputs.iterator(); itr.hasNext();){
				format = itr.next();
				buffer += "<option value=\"" + format + "\">" + format + "</option>\n";
			}
		}

		buffer += "</select></td></tr>\n";
		buffer += "<tr><td></td><td><input type=\"submit\" value=\"Convert\"></td></tr>\n";
		buffer += "</table>\n";
		buffer += "</form>";

		return buffer;
	}

	/**
	 * Convert a line separated list into an HTML list of links.
	 * @param list the line separated list of items
	 * @param link the URL base
	 * @param APPEND true if list items should be appended to the end of link
	 * @param title the title of the generated HTML page (can be null)
	 * @return an HTML version of the given list
	 */
	public static String createHTMLList(String list, String link, boolean APPEND,	String title)
	{
		String buffer = "";
		Scanner scanner = new Scanner(list);
		String line, endpoint;
		int tmpi;
	
		if(title != null){
			buffer += "<h1>" + title + "</h1>\n";
		}
	
		buffer += "<ul>\n";
	
		while(scanner.hasNextLine()){
			line = scanner.nextLine().trim();
			endpoint = line;
			tmpi = endpoint.indexOf('(');
	
			if(tmpi >= 0){ // Remove full application name within parenthesis of software list
				endpoint = endpoint.substring(0, tmpi).trim();
			}
	
			buffer += "<li><a href=\"" + link;
			if(APPEND) buffer += endpoint;
			buffer += "\">" + line + "</a></li>\n";
		}
	
		buffer += "</ul>\n";
		scanner.close(); // added by Edgar Black
		return buffer;
	}

	/**
	 * Remove parameters from a URL.
	 * @param url the URL of a file
	 * @return the URL of a file without parameters
	 */
	public static String removeParameters(String url)
	{
		int tmpi = url.lastIndexOf('?');

		if(tmpi >= 0){
			return url.substring(0, tmpi);
		}else{
			return url;
		}
	}

	/**
	 * Query an endpoint.
	 * @param url the URL of the endpoint
	 * @return the text obtained from the endpoint
	 */
	public static String queryEndpoint(String url)
	{
		HttpURLConnection.setFollowRedirects(false);
	  HttpURLConnection conn = null;
	  BufferedReader ins;
	  StringBuilder outs = new StringBuilder();
	  char[] buffer = new char[1024];
	  String text = null;
	  int tmpi;
			  			
	  try{
	    conn = (HttpURLConnection)new URL(url).openConnection();
      conn.setRequestProperty("Accept", "text/plain");
	    conn.connect();
	    ins = new BufferedReader(new InputStreamReader(conn.getInputStream()));
	    
	    do{
	      tmpi = ins.read(buffer, 0, buffer.length);
	      if(tmpi>0) outs.append(buffer, 0, tmpi);
	    }while(tmpi >= 0);
	    
	    text = outs.toString();
	    conn.disconnect();
	  }catch(Exception e){
	    //e.printStackTrace();
	  }finally{
	    if(conn != null) conn.disconnect();
	  }
	  
	  return text;
	}

	public static void stop()
	{
	  try{
	  	tjws.stop();
	  }catch(Exception e) {e.printStackTrace();}
	}

	public static void main(String[] args)
	{
		tjws = new TJWSEmbeddedJaxrsServer();
		tjws.setPort(8182);
		tjws.start();
		//tjws.getDeployment().getRegistry().addPerRequestResource(ResteasyResources.class);
		tjws.getDeployment().getRegistry().addSingletonResource(new SoftwareServerRESTEasy());
	}
}

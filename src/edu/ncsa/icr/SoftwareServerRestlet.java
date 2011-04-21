package edu.ncsa.icr;
import edu.ncsa.icr.ICRAuxiliary.*;
import edu.ncsa.icr.ICRAuxiliary.Application;
import edu.ncsa.image.ImageUtility;
import edu.ncsa.utility.*;
import java.util.*;
import java.io.*;
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
import org.apache.commons.fileupload.*;
import org.apache.commons.fileupload.disk.*;

/**
 * A restful interface for a software reuse server.
 * Think of this as an extended software reuse server.
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
		String buffer = "";
		
		for(int i=0; i<applications.size(); i++){	
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
		
		
		//Add ping
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
			//buffer += "      document.getElementById('ping').innerHTML = window.endTime - window.startTime + \" ms\";\n";
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
		
		for(int i=0; i<applications.size(); i++){
			if(i > 0) buffer += "\n";
			buffer += "  if(application == \"" + applications.get(i).alias + "\"){\n";
			count = 0;
			
			for(Iterator<TaskInfo> itr=application_tasks.get(i).iterator(); itr.hasNext();){
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
		
		for(int i=0; i<applications.size(); i++){
			for(Iterator<TaskInfo> itr1=application_tasks.get(i).iterator(); itr1.hasNext();){
				task_info = itr1.next();
				if(FIRST_BLOCK) FIRST_BLOCK = false; else	buffer += "\n";
				buffer += "  if(application == \"" + applications.get(i).alias + "\" && task == \"" + task_info.name + "\"){\n";	
				buffer += "    inputs.innerHTML = \"";
				FIRST_VALUE = true;
				
				for(Iterator<String> itr2=task_info.inputs.iterator(); itr2.hasNext();){
					if(FIRST_VALUE) FIRST_VALUE = false; else buffer += ", ";
					buffer += itr2.next();
				}
				
				buffer += "\";\n";
				count = 0;
				
				for(Iterator<String> itr2=task_info.outputs.iterator(); itr2.hasNext();){
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
		
		for(int i=0; i<applications.size(); i++){
			buffer += "<option value=\"" + applications.get(i).alias + "\"";
			
			if(selected_application != null && selected_application.equals(applications.get(i).alias)){
				buffer += " selected";
			}

			buffer += ">" + applications.get(i) + "</option>\n";
		}
		
		buffer += "</select></td></tr>\n";
		buffer += "<tr><td><b>Task:</b></td>\n";
		buffer += "<td><select name=\"task\" id=\"task\" onchange=\"setFormats();\">\n";
				
		for(Iterator<TaskInfo> itr=application_tasks.get(0).iterator(); itr.hasNext();){
			task_info = itr.next();
			buffer += "<option value=\"" + task_info.name + "\">" + task_info.name + "</option>\n";
		}
		
		buffer += "</select></td></tr>\n";
		buffer += "<tr><td><td width=\"100\"><i><font size=\"-1\"><div id=\"inputs\">";
		FIRST_VALUE = true;

		for(Iterator<String> itr=application_tasks.get(0).first().inputs.iterator(); itr.hasNext();){
			if(FIRST_VALUE) FIRST_VALUE = false; else buffer += ", ";
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
		
		for(Iterator<String> itr=application_tasks.get(0).first().outputs.iterator(); itr.hasNext();){
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
		
		for(int i=0; i<applications.size(); i++){
			convert_task = TaskInfo.getTask(application_tasks.get(i), "convert");
			
			if(convert_task != null){
				if(i > 0) buffer += "\n";
				buffer += "  if(application == \"" + applications.get(i).alias + "\"){\n";	
				buffer += "    inputs.innerHTML = \"";
				FIRST_VALUE = true;
				
				for(Iterator<String> itr=convert_task.inputs.iterator(); itr.hasNext();){
					if(FIRST_VALUE) FIRST_VALUE = false; else buffer += ", ";
					buffer += itr.next();
				}
				
				buffer += "\";\n";
				count = 0;
				
				for(Iterator<String> itr=convert_task.outputs.iterator(); itr.hasNext();){
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
		
		for(int i=0; i<applications.size(); i++){
			buffer += "<option value=\"" + applications.get(i).alias + "\">" + applications.get(i) + "</option>\n";
		}
		
		buffer += "</select></td></tr>\n";
		buffer += "<tr><td><td width=\"100\"><i><font size=\"-1\"><div id=\"inputs\">";
		
		convert_task = TaskInfo.getTask(application_tasks.get(0), "convert");
		
		if(convert_task != null){
			FIRST_VALUE = true;

			for(Iterator<String> itr=convert_task.inputs.iterator(); itr.hasNext();){
				if(FIRST_VALUE) FIRST_VALUE = false; else buffer += ", ";
				buffer += itr.next();
			}
		}
		
		buffer += "</div></font></i></td></tr>\n";
		buffer += "<tr><td><b>File:</b></td><td><input type=\"text\" name=\"file\" size=\"100\"></td></tr>\n";
		buffer += "<tr><td><b>Format:</b></td>\n";
		buffer += "<td><select name=\"format\" id=\"format\">\n";
		
		if(convert_task != null){
			for(Iterator<String> itr=convert_task.outputs.iterator(); itr.hasNext();){
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
			if(file.startsWith(getReference().getBaseRef() + "file/")){	//Remove session id from filenames of locally cached files
				file = SoftwareServer.getFilename(Utility.getFilename(file));
			}else{																											//Download remote files
				Utility.downloadFile(server.getCachePath(), session + "_" + Utility.getFilenameName(file), file);
				file = Utility.getFilename(file);
			}
		
			task = getTask(application_alias, task_string, file, format);
			//Task.print(task, applications);
			
			result = server.executeTask("localhost", session, task);

			//Create empty output if not created (e.g. when no conversion path was found)
			if(result == null){
				result = server.getCachePath() + session + "_" + Utility.getFilenameName(file) + "." + format;
				Utility.touch(result);
			}
			
			//Move result to public folder
			Utility.copyFile(result, public_path + Utility.getFilename(result));
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
				return new StringRepresentation(getApplications(), MediaType.TEXT_PLAIN);
			}else{
				if(part2.isEmpty()){
					return new StringRepresentation(getApplicationTasks(part1), MediaType.TEXT_PLAIN);
				}else{
					if(part3.isEmpty()){
						return new StringRepresentation(getApplicationTaskOutputs(part1, part2), MediaType.TEXT_PLAIN);
					}else{
						if(part3.equals("*")){
							return new StringRepresentation(getApplicationTaskInputsOutputs(part1, part2), MediaType.TEXT_PLAIN);
						}else if(part4.isEmpty()){
							return new StringRepresentation(getApplicationTaskInputs(part1, part2), MediaType.TEXT_PLAIN);
						}else{
							application = part1;
							task = part2;
							format = part3;
							file = URLDecoder.decode(part4);
							session = -1;
							
							if(file.startsWith(getReference().getBaseRef() + "/file/")){		//Locally cached files already have session ids
								session = SoftwareServer.getSession(file);
								result = getReference().getBaseRef() + "file/" + Utility.getFilenameName(file) + "." + format;
							}else{																													//Remote files must be assigned a session id
								session = server.getSession();
								result = getReference().getBaseRef() + "file/" + session + "_" + Utility.getFilenameName(file) + "." + format;
							}
														
							executeTaskLater(session, application, task, file, format);
							
							if(isTextOnly(Request.getCurrent())){
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
					url = getRootRef() + "software/" + application + "/" + task + "/" + format + "/" + URLEncoder.encode(file);

					return new StringRepresentation("<html><head><meta http-equiv=\"refresh\" content=\"1; url=" + url + "\"></head</html>", MediaType.TEXT_HTML);
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
				file = alias_map.get(Utility.getFilenameName(part1)).icon;
				
				if(file != null){
					return new FileRepresentation(file, MediaType.IMAGE_JPEG);
				}else{
					return new StringRepresentation("Image doesn't exist", MediaType.TEXT_PLAIN);
				}
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
		}else if(part0.equals("screen")){
			if(ADMINISTRATORS_ENABLED && isAdministrator(getRequest())){
				ImageUtility.save(public_path + "screen.jpg", ImageUtility.getScreen());
				
				return new FileRepresentation(new File(public_path + "screen.jpg"), MediaType.IMAGE_JPEG);
			}else{
				return new StringRepresentation("error: you don't have permission to do this", MediaType.TEXT_PLAIN);
			}
		}else if(part0.equals("reboot")){
			if(ADMINISTRATORS_ENABLED && isAdministrator(getRequest())){
				server.rebootMachine();
				
				return new StringRepresentation("ok", MediaType.TEXT_PLAIN);
			}else{
				return new StringRepresentation("error: you don't have permission to do this", MediaType.TEXT_PLAIN);
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
							file = getReference().getBaseRef() + "file/" + session + "_" + fi.getName();
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
				
				result = getReference().getBaseRef() + "file/" + Utility.getFilenameName(file) + "." + format;

				if(isTextOnly(Request.getCurrent())){
					return new StringRepresentation(result, MediaType.TEXT_PLAIN);
				}else{
					return new StringRepresentation("<a href=" + result + ">" + result + "</a>", MediaType.TEXT_HTML);
				}
			}
		}
		
		return httpGetHandler();
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

	/**
	 * Check if the given request is for plain text only.
	 * @param request the request
	 * @return true if plain/text only
	 */
	public static boolean isTextOnly(Request request)
	{
		List<Preference<MediaType>> types = request.getClientInfo().getAcceptedMediaTypes();

		if(types.size() == 1 && types.get(0).getMetadata().getName().equals("text/plain")){
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
			new Server(Protocol.HTTP, port, SoftwareReuseRestlet.class).start();
		}catch(Exception e) {e.printStackTrace();}
		*/
		
		try{			
			Component component = new Component();
			component.getServers().add(Protocol.HTTP, port);
			component.getClients().add(Protocol.HTTP);
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
	  				queryEndpoint(url);
	  				Utility.pause(2000);
	  			}
	  		}
	  	}.start();
  	}
	}
}
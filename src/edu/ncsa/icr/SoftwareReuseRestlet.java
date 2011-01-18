package edu.ncsa.icr;
import edu.ncsa.icr.SoftwareReuseAuxiliary.*;
import edu.ncsa.icr.SoftwareReuseAuxiliary.Application;
import edu.ncsa.utility.*;
import java.util.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.net.*;
import org.restlet.*;
import org.restlet.resource.*;
import org.restlet.data.*;
import org.restlet.representation.*;
import org.restlet.routing.Router;

/**
 * A restful interface for a software reuse server.
 * Think of this as an extended software reuse server.
 * @author Kenton McHenry
 */
public class SoftwareReuseRestlet extends ServerResource
{
	private static SoftwareReuseServer server;
	private static Vector<Application> applications;
	private static Vector<TreeSet<TaskInfo>> application_tasks;
	
	/**
	 * Initialize.
	 */
	public static void initialize()
	{
		Operation operation;
		
		server = new SoftwareReuseServer("SoftwareReuseServer.ini");
		applications = server.getApplications();
		application_tasks = Task.getApplicationTasks(applications);
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
	 * Get a web form interface for this restful service.
	 * @return the form
	 */
	public String getForm()
	{
		String buffer = "";
		String format;
		TaskInfo task_info;
		int count;
		boolean FIRST_IF;
		
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
		
		FIRST_IF = true;
		
		for(int i=0; i<applications.size(); i++){
			for(Iterator<TaskInfo> itr1=application_tasks.get(i).iterator(); itr1.hasNext();){
				if(FIRST_IF){
					FIRST_IF = false;
				}else{
					 buffer += "\n";
				}
				
				task_info = itr1.next();
				buffer += "  if(application == \"" + applications.get(i).alias + "\" && task == \"" + task_info.name + "\"){\n";
				count = 0;
	
				buffer += "    inputs.innerHTML = \"";
				
				for(Iterator<String> itr2=task_info.inputs.iterator(); itr2.hasNext();){
					if(count > 0) buffer += ", ";
					buffer += itr2.next();
					count++;
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
		buffer += "</script>\n\n";
		
		buffer += "<form name=\"converson\" action=\"form/\" method=\"get\">\n";
		buffer += "<table>\n";
		buffer += "<tr><td><b>Application:</b></td>\n";
		buffer += "<td><select name=\"application\" id=\"application\" onchange=\"setTasks();\">\n";
		
		for(int i=0; i<applications.size(); i++){
			buffer += "<option value=\"" + applications.get(i).alias + "\">" + applications.get(i) + "</option>\n";
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
		
		count = 0;

		for(Iterator<String> itr=application_tasks.get(0).first().inputs.iterator(); itr.hasNext();){
			if(count > 0) buffer += ", ";
			buffer += itr.next();
			count++;
		}
		
		buffer += "</div></font></i></td></tr>\n";
		buffer += "<tr><td><b>File:</b></td><td><input type=\"text\" name=\"file\" size=\"100\"></td></tr>\n";
		buffer += "<tr><td><b>Format:</b></td>\n";
		buffer += "<td><select name=\"format\" id=\"format\">\n";
		
		for(Iterator<String> itr=application_tasks.get(0).first().outputs.iterator(); itr.hasNext();){
			format = itr.next();
			buffer += "<option value=\"" + format + "\">" + format + "</option>\n";
		}
		
		buffer += "</select></td></tr>\n";		
		buffer += "<tr><td></td><td><input type=\"submit\" value=\"Submit\"></td></tr>\n";
		buffer += "</table>\n";
		buffer += "</form>";
		
		return buffer;
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
				count = 0;
	
				buffer += "    inputs.innerHTML = \"";
				
				for(Iterator<String> itr=convert_task.inputs.iterator(); itr.hasNext();){
					if(count > 0) buffer += ", ";
					buffer += itr.next();
					count++;
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
			count = 0;

			for(Iterator<String> itr=convert_task.inputs.iterator(); itr.hasNext();){
				if(count > 0) buffer += ", ";
				buffer += itr.next();
				count++;
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
	 * @param operation_comment the operation comment
	 * @param filename the file name of the cached file to convert
	 * @param output_format the output format
	 * @return the task to perform the conversion
	 */
	public Vector<Subtask> getTask(String application_alias, String operation_comment, String filename, String output_format)
	{
		Task task = new Task(applications);
		task.addSubtasks(task.getApplicationString(application_alias), operation_comment, new CachedFileData(filename), new CachedFileData(filename, output_format));
		
		return task.getSubtasks();
	}
	
	/**
	 * Convert a file to the specified output format using the given application.
	 * @param application_alias the application to use
	 * @param operation_comment the operation comment
	 * @param file the URL of the file to convert
	 * @param format the output format
	 */
	public synchronized void convert(String application_alias, String operation_comment, String file, String format)
	{
		Vector<Subtask> task = getTask(application_alias, operation_comment, Utility.getFilename(file), format);
		
		//Task.print(task, applications);
	
		Utility.downloadFile(server.getCachePath(), "0_" + Utility.getFilenameName(file), file);
		server.executeTasks("localhost", 0, task);	
	}
	
	/**
	 * Convert a file (asynchronously) to the specified output format using the given application.
	 * @param application_alias the application to use
	 * @param operation_comment the operation comment
	 * @param file the URL of the file to convert
	 * @param format the output format
	 * @return the results of the conversion
	 */
	public String convertLater(String application_alias, String operation_comment, String file, String format)
	{
		final String application_alias_final = application_alias;
		final String comment_final = operation_comment;
		final String file_final = file;
		final String format_final = format;
		
		new Thread(){
			public void run(){
				convert(application_alias_final, comment_final, file_final, format_final);
			}
		}.start();
		
		return Utility.getFilenameName(file) + "." + format;
	}
	
	@Get
	public Representation httpGetHandler()
	{
		Vector<String> parts = Utility.split(getReference().getRemainingPart(), '/', true);
		String part0 = (parts.size() > 0) ? parts.get(0) : "";
		String part1 = (parts.size() > 1) ? parts.get(1) : "";
		String part2 = (parts.size() > 2) ? parts.get(2) : "";
		String part3 = (parts.size() > 3) ? parts.get(3) : "";
		String application = null, task = null, file = null, format = null, url;
		String buffer;
		Form form;
		Parameter p;
		
		if(part0.isEmpty()){
			return new StringRepresentation(getApplications(), MediaType.TEXT_PLAIN);
		}else{
			if(part0.equals("form")){
				if(part1.isEmpty()){
					buffer = "";
					buffer += "general\n";
					buffer += "convert";
					
					return new StringRepresentation(buffer, MediaType.TEXT_PLAIN);
				}else{
					form = getRequest().getResourceRef().getQueryAsForm();
					p = form.getFirst("application"); if(p != null) application = p.getValue();
					p = form.getFirst("task"); if(p != null) task = p.getValue();
					p = form.getFirst("file"); if(p != null) file = p.getValue();
					p = form.getFirst("format"); if(p != null) format = p.getValue();
									
					if(application != null && task != null && file != null && format != null){
						url = getRootRef() + "/" + application + "/" + task + "/" + format + "/" + URLEncoder.encode(file);
	
						return new StringRepresentation("<html><head><meta http-equiv=\"refresh\" content=\"1; url=" + url + "\"></head</html>", MediaType.TEXT_HTML);
					}else{
						if(part1.equals("general")){
							return new StringRepresentation(getForm(), MediaType.TEXT_HTML);
						}else if(part1.equals("convert")){
							return new StringRepresentation(getConvertForm(), MediaType.TEXT_HTML);
						}else{
							return new StringRepresentation("invalid endpoint", MediaType.TEXT_PLAIN);
						}
					}
				}
			}else if(part0.equals("result")){
				if(!part1.isEmpty()){	
					file = server.getCachePath() + "0_" + part1;
					
					if(Utility.exists(file)){
						return new FileRepresentation(file, MediaType.MULTIPART_ALL);
					}else{
						return new StringRepresentation("File doesn't exist", MediaType.TEXT_PLAIN);
					}
				}else{
					return new StringRepresentation("invalid endpoint", MediaType.TEXT_PLAIN);
				}
			}else if(part0.equals("busy")){
				return new StringRepresentation("" + server.isBusy(), MediaType.TEXT_PLAIN);
			}else if(part1.isEmpty()){
				return new StringRepresentation(getApplicationTasks(part0), MediaType.TEXT_PLAIN);
			}else{
				if(part2.isEmpty()){
					return new StringRepresentation(getApplicationTaskInputs(part0, part1), MediaType.TEXT_PLAIN);
				}else{
					if(part3.isEmpty()){
						return new StringRepresentation(getApplicationTaskOutputs(part0, part1), MediaType.TEXT_PLAIN);
					}else{
						application = part0;
						task = part1;
						format = part2;
						file = URLDecoder.decode(part3);
						
						file = getReference().getBaseRef() + "/result/" + convertLater(application, task.equals("convert") ? "" : task, file, format);
						
						return new StringRepresentation("<a href=" + file + ">" + file + "</a>", MediaType.TEXT_HTML);
					}
				}
			}
		}
	}
	
	/**
	 * Start the restful service.
	 * @param args the input arguments
	 */
	public static void main(String[] args)
	{		
		int port = 8182;
		String master_resltet = null;
		
		//Load *.ini file
	  try{
	    BufferedReader ins = new BufferedReader(new FileReader("SoftwareReuseRestlet.ini"));
	    String line, key, value;
	    
	    while((line=ins.readLine()) != null){
	      if(line.contains("=")){
	        key = line.substring(0, line.indexOf('='));
	        value = line.substring(line.indexOf('=')+1);
	        
	        if(key.charAt(0) != '#'){
	        	if(key.equals("Port")){
	        		port = Integer.valueOf(value);
	          }else if(key.equals("Master")){
	          	master_resltet = value;
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
			new Server(Protocol.HTTP, port, SoftwareReuseConversionRestlet.class).start();
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
					router.attachDefault(SoftwareReuseRestlet.class);
					return router;
				}
			};
			
			component.getDefaultHost().attach("/software", application);
			component.start();
		}catch(Exception e) {e.printStackTrace();}
	}
}
package edu.ncsa.icr;
import edu.ncsa.icr.SoftwareReuseAuxiliary.*;
import edu.ncsa.icr.SoftwareReuseAuxiliary.Application;
import edu.ncsa.utility.*;
import java.util.*;
import org.restlet.*;
import org.restlet.resource.*;
import org.restlet.data.*;
import org.restlet.representation.*;
import org.restlet.routing.Router;

/**
 * A restful conversion interface for a software reuse server.
 * Think of this as an extended software reuse server.
 * @author Kenton McHenry
 */
public class SoftwareReuseConversionRestlet extends ServerResource
{
	private static SoftwareReuseServer server;
	private static Vector<Application> applications;
	private static Vector<TreeSet<String>> inputs = new Vector<TreeSet<String>>();
	private static Vector<TreeSet<String>> outputs = new Vector<TreeSet<String>>();
	
	/**
	 * Initialize.
	 */
	public static void initialize()
	{
		Operation operation;
		
		server = new SoftwareReuseServer("SoftwareReuseServer.ini");
		applications = server.getApplications();
				
		for(int i=0; i<applications.size(); i++){
			inputs.add(new TreeSet<String>());
			outputs.add(new TreeSet<String>());
			
			for(int j=0; j<applications.get(i).operations.size(); j++){
				operation = applications.get(i).operations.get(j);
				
				for(int k=0; k<operation.inputs.size(); k++){
					inputs.get(i).add(operation.inputs.get(k).toString());
				}
				
				for(int k=0; k<operation.outputs.size(); k++){
					outputs.get(i).add(operation.outputs.get(k).toString());
				}
			}
		}
	}
	
	/**
	 * Get the applications along with their available inputs/outputs.
	 * @return the applications and their inputs/outputs
	 */
	public String getApplications()
	{
		String buffer = "";

		for(int i=0; i<applications.size(); i++){
			buffer += applications.get(i).alias + "\n";
			
			for(Iterator<String> itr=inputs.get(i).iterator(); itr.hasNext();){
				buffer += itr.next() + " ";
			}
			
			buffer += "\n";
			
			for(Iterator<String> itr=outputs.get(i).iterator(); itr.hasNext();){
				buffer += itr.next() + " ";
			}
			
			buffer += "\n\n";
		}
		
		return buffer;
	}
	
	/**
	 * Get a web form interface to this restful service.
	 * @return the form
	 */
	public String getForm()
	{
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
		buffer += "  outputs.options.length = 0;\n";
		buffer += "  \n";
		
		for(int i=0; i<applications.size(); i++){
			if(i > 0) buffer += "\n";
			buffer += "  if(application == \"" + applications.get(i) + "\"){\n";
			count = 0;

			buffer += "    inputs.innerHTML = \"";
			
			for(Iterator<String> itr=inputs.get(i).iterator(); itr.hasNext();){
				if(count > 0) buffer += ", ";
				buffer += itr.next();
				count++;
			}
			
			buffer += "\";\n";
			count = 0;
			
			for(Iterator<String> itr=outputs.get(i).iterator(); itr.hasNext();){
				format = itr.next();
				buffer += "    outputs.options[" + count + "] = new Option(\"" + format + "\", \"" + format + "\", false, false);\n";
				count++;
			}
			
			buffer += "  }\n";
		}
		
		buffer += "}\n";
		buffer += "</script>\n\n";
		
		buffer += "<form name=\"converson\" action=\".\" method=\"get\">\n";
		buffer += "<table>\n";
		buffer += "<tr><td><b>Application:</b></td>\n";
		buffer += "<td><select name=\"application\" id=\"application\" onchange=\"setFormats();\">\n";
		
		for(int i=0; i<applications.size(); i++){
			buffer += "<option value=\"" + applications.get(i) + "\">" + applications.get(i) + "</option>\n";
		}
		
		buffer += "</select></td></tr>\n";
		buffer += "<tr><td><td width=\"100\"><i><font size=\"-1\"><div id=\"inputs\">";
		count = 0;
		
		for(Iterator<String> itr=inputs.get(0).iterator(); itr.hasNext();){
			if(count > 0) buffer += ", ";
			buffer += itr.next();
			count++;
		}
		
		buffer += "</div></font></i></td></tr>\n";
		buffer += "<tr><td><b>File:</b></td><td><input type=\"text\" name=\"file\" size=\"100\"></td></tr>\n";
		buffer += "<tr><td><b>Format:</b></td>\n";
		buffer += "<td><select name=\"format\" id=\"format\">\n";
		
		for(Iterator<String> itr=outputs.get(0).iterator(); itr.hasNext();){
			format = itr.next();
			buffer += "<option value=\"" + format + "\">" + format + "</option>\n";
		}
		
		buffer += "</select></td></tr>\n";		
		buffer += "<tr><td></td><td><input type=\"submit\" value=\"Convert\"></td></tr>\n";
		buffer += "</table>\n";
		buffer += "</form>";
		
		return buffer;
	}
	
	/**
	 * Get the tasks involved in using the given applications to convert the given file to the specified output format.
	 * @param application_name the name of the application to use.
	 * @param filename the file name of the cached file to convert
	 * @param output_format the output format
	 * @return the tasks to perform the conversion
	 */
	public Vector<Task> getTasks(String application_name, String filename, String output_format)
	{
		Vector<Task> tasks = new Vector<Task>();
		Application application;
		Operation operation;
		String input_format = Utility.getFilenameExtension(filename);
		String format;
		int application_index = -1;
		int input_operation_index = -1;
		int output_operation_index = -1;
		
		//Find the application
		for(int i=0; i<applications.size(); i++){
			if(applications.get(i).name.equals(application_name)){
				application_index = i;
				break;
			}
		}
		
		//Find the needed operations
		if(application_index != -1){
			application = applications.get(application_index);
			
			for(int i=0; i<application.operations.size(); i++){
				operation = application.operations.get(i);
				
				if(input_operation_index == -1){
					for(int j=0; j<operation.inputs.size(); j++){
						format = ((FileData)operation.inputs.get(j)).getFormat();
						
						if(format.equals(input_format)){
							input_operation_index = i;
							break;
						}
					}
				}
				
				if(output_operation_index == -1){
					for(int j=0; j<operation.outputs.size(); j++){
						format = ((FileData)operation.outputs.get(j)).getFormat();
						
						if(format.equals(output_format)){
							output_operation_index = i;
							break;
						}
					}
				}
				
				//Ignore incomplete converts
				if(operation.name.equals("convert")){
					if((input_operation_index == i || output_operation_index == i) && input_operation_index != output_operation_index){
						input_operation_index = -1;
						output_operation_index = -1;
					}
				}
				
				//Break if we have found both an input and an output operation
				if(input_operation_index != -1 && output_operation_index != -1) break;
			}
			
			//Build the task
			if(input_operation_index != -1 && output_operation_index != -1){
				if(input_operation_index == output_operation_index){	//Binary operation (e.g. "convert")
					tasks.add(new Task(application_index, input_operation_index, new CachedFileData(filename), new CachedFileData(filename, output_format)));
				}else{
					tasks.add(new Task(application_index, input_operation_index, new CachedFileData(filename), new Data()));
					tasks.add(new Task(application_index, output_operation_index, new Data(), new CachedFileData(filename, output_format)));
				}
			}
		}
		
		return tasks;
	}
	
	/**
	 * Convert a file to the specified output format using the given application.
	 * @param application the application to use
	 * @param file the URL of the file to convert
	 * @param format the output format
	 */
	public synchronized void convert(String application, String file, String format)
	{
		Vector<Task> tasks = getTasks(application, Utility.getFilename(file), format);
					
		//Task.print(applications, tasks);
	
		Utility.downloadFile(server.getCachePath(), "0_" + Utility.getFilenameName(file), file);
		server.executeTasks("localhost", 0, tasks);	
	}
	
	/**
	 * Convert a file (asynchronously) to the specified output format using the given application.
	 * @param application the application to use
	 * @param file the URL of the file to convert
	 * @param format the output format
	 * @return the results of the conversion
	 */
	public String convertLater(String application, String file, String format)
	{
		final String application_final = application;
		final String file_final = file;
		final String format_final = format;
		
		new Thread(){
			public void run(){
				convert(application_final, file_final, format_final);
			}
		}.start();
		
		return Utility.getFilenameName(file) + "." + format;
	}
	
	@Get
	public Representation httpGetHandler()
	{
		String application = null;
		String file = null;
		String format = null;
		Form form;
		Parameter p;
		
		Vector<String> parts = Utility.split(getReference().getRemainingPart(), '/', true);
		String part0 = (parts.size() > 0) ? parts.get(0) : "";
		String part1 = (parts.size() > 1) ? parts.get(1) : "";
				
		if(part0.equals("software")){
			return new StringRepresentation(getApplications(), MediaType.TEXT_PLAIN);
		}else if(part0.equals("new")){
			return new StringRepresentation(getForm(), MediaType.TEXT_HTML);
		}else if(part0.equals("download")){
			form = getRequest().getResourceRef().getQueryAsForm();
			p = form.getFirst("file"); if(p != null) file = p.getValue();
	
			if(file != null){
				return new StringRepresentation("ok1", MediaType.TEXT_PLAIN);
			}else{
				return new StringRepresentation("missing arguments", MediaType.TEXT_PLAIN);
			}
		}else if(part0.startsWith("?")){
			form = getRequest().getResourceRef().getQueryAsForm();
			p = form.getFirst("application"); if(p != null) application = p.getValue();
			p = form.getFirst("file"); if(p != null) file = p.getValue();
			p = form.getFirst("format"); if(p != null) format = p.getValue();
	
			if(application != null && file != null && format != null){
				file = getReference().getBaseRef() + "/download/?file=" + convertLater(application, file, format);
				return new StringRepresentation("<a href=" + file + ">" + file + "</a>", MediaType.TEXT_HTML);
			}else{
				return new StringRepresentation("missing arguments", MediaType.TEXT_PLAIN);
			}
		}else if(part0.isEmpty()){
			return new StringRepresentation("missing arguments", MediaType.TEXT_PLAIN);
		}else{
			return new StringRepresentation("invalid endpoint", MediaType.TEXT_PLAIN);
		}
	}
	
	/**
	 * Start the restful service.
	 * @param args the input arguments
	 */
	public static void main(String[] args)
	{		
		initialize();

		/*
		try{
			new Server(Protocol.HTTP, 8182, SoftwareReuseConversionRestlet.class).start();
		}catch(Exception e) {e.printStackTrace();}
		*/
		
		try{			
			Component component = new Component();
			component.getServers().add(Protocol.HTTP, 8182);
			component.getClients().add(Protocol.HTTP);
			component.getLogService().setEnabled(false);
			
			org.restlet.Application application = new org.restlet.Application(){
				@Override
				public Restlet createInboundRoot(){
					Router router = new Router(getContext());
					router.attachDefault(SoftwareReuseConversionRestlet.class);
					return router;
				}
			};
			
			component.getDefaultHost().attach("/convert", application);
			component.start();
		}catch(Exception e) {e.printStackTrace();}
	}
}
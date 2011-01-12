package edu.ncsa.icr;
import edu.ncsa.icr.SoftwareReuseAuxiliary.*;
import edu.ncsa.icr.SoftwareReuseAuxiliary.Application;
import edu.ncsa.utility.Utility;
import java.util.*;
import org.restlet.*;
import org.restlet.resource.*;
import org.restlet.data.*;
import org.restlet.representation.*;

/**
 * A restful conversion interface for a software reuse server.
 * @author Kenton McHenry
 */
public class SoftwareReuseConversionRestlet extends ServerResource
{
	private static SoftwareReuseServer server;
	private static Vector<Application> applications;
	
	/**
	 * Get the applications along with their available inputs/outputs.
	 * @return the applications and their inputs/outputs
	 */
	public String getApplications()
	{
		String buffer = "";
		TreeSet<String> application_inputs = new TreeSet<String>();
		TreeSet<String> application_outputs = new TreeSet<String>();
		Operation operation;			

		for(int i=0; i<applications.size(); i++){
			application_inputs.clear();
			application_outputs.clear();
			buffer += applications.get(i).alias + "\n";
			
			for(int j=0; j<applications.get(i).operations.size(); j++){
				operation = applications.get(i).operations.get(j);
				
				for(int k=0; k<operation.inputs.size(); k++){
					application_inputs.add(operation.inputs.get(k).toString());
				}
				
				for(int k=0; k<operation.outputs.size(); k++){
					application_outputs.add(operation.outputs.get(k).toString());
				}
			}
			
			for(Iterator<String> itr=application_inputs.iterator(); itr.hasNext();){
				buffer += itr.next() + " ";
			}
			
			buffer += "\n";
			
			for(Iterator<String> itr=application_outputs.iterator(); itr.hasNext();){
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
		
		buffer += "<form name=\"converson\", action=\"\" method=\"get\">\n";
		buffer += "Application: <input type=\"text\" name=\"application\"><br>\n";
		buffer += "File: <input type=\"text\" name=\"file\"><br>\n";
		buffer += "Format: <input type=\"text\" name=\"format\"><br>\n";
		buffer += "<input type=\"submit\" value=\"Submit\">";
		buffer += "</form>";
		
		return buffer;
	}
	
	/**
	 * Get the tasks involved in using the given applications to convert the given file to the specified output format.
	 * @param application_name the name of the application to use.
	 * @param filename the file name of the file to convert
	 * @param output_format the output format
	 * @return the tasks to perform the conversion
	 */
	public Vector<Task> getTasks(String application_name, String filename, String output_format)
	{
		Vector<Task> tasks = new Vector<Task>();
		Application application;
		Operation operation;
		String input_format = Utility.getFilenameExtension(filename);
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
						if(operation.inputs.get(j).equals(input_format)){
							input_operation_index = i;
							break;
						}
					}
				}
				
				if(output_operation_index == -1){
					for(int j=0; j<operation.outputs.size(); j++){
						if(operation.outputs.get(j).equals(output_format)){
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
					tasks.add(new Task(application_index, input_operation_index, new CachedFileData(filename), null));
					tasks.add(new Task(application_index, output_operation_index, null, new CachedFileData(filename, output_format)));
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
		String filename = server.getCachePath() + "0_" + Utility.getFilename(file);
		
		Utility.downloadFile(server.getCachePath(), "0_" + Utility.getFilenameName(file), file);
		server.executeTasks("localhost", 0, getTasks(application, filename, format));
	}
	
	@Get
	public Representation httpGetHandler()
	{
		Representation representation = null;
		String application = null;
		String file = null;
		String format = null;
		boolean SHOW_FORM = false;
		Form form;
		Parameter p;
		
		form = getRequest().getResourceRef().getQueryAsForm();
		p = form.getFirst("application"); if(p != null) application = p.getValue();
		p = form.getFirst("file"); if(p != null) file = p.getValue();
		p = form.getFirst("format"); if(p != null) format = p.getValue();
		p = form.getFirst("form"); if(p != null) SHOW_FORM = Boolean.valueOf(p.getValue());

		if(application != null && file != null && format != null){
			convert(application, file, format);
		}else{
			if(SHOW_FORM){
				representation = new StringRepresentation(getForm(), MediaType.TEXT_HTML);
			}else{
				representation = new StringRepresentation(getApplications(), MediaType.TEXT_PLAIN);
			}
		}
		
		return representation;
	}
	
	/**
	 * Start the restful service.
	 * @param args the input arguments
	 */
	public static void main(String[] args)
	{		
		server = new SoftwareReuseServer("SoftwareReuseServer.ini");
		applications = server.getApplications();

		try{
			new Server(Protocol.HTTP, 8182, SoftwareReuseConversionRestlet.class).start();
		}catch(Exception e) {e.printStackTrace();}
	}
}
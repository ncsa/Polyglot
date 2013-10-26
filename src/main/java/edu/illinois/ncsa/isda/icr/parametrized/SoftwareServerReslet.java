package edu.illinois.ncsa.isda.icr.parametrized;
import edu.illinois.ncsa.isda.icr.parametrized.ICRAuxiliary.*;
import edu.illinois.ncsa.isda.icr.parametrized.ICRAuxiliary.Application;
import java.util.*;
import java.io.*;
import java.lang.management.*;
import java.net.*;
import javax.servlet.*;
import kgm.image.ImageUtility;
import kgm.utility.*;
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
public class SoftwareServerReslet extends ServerResource
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
		application_tasks = Task.getApplicationTasks(applications); //changed to include parameters
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
	 * Get the output formats supported by the given task from a known input format.
	 * @param alias the application alias
	 * @param task the application task
	 * @return the output formats supported
	 */
	public String getApplicationTaskOutputs(String alias, String task, String input)
	{
		Application application=null;
		for(Application app: applications){
			if(app.alias.equalsIgnoreCase(alias)){
				application = app;
				break;
			}
		}
		if(application==null)
			return "";
		boolean FOUND;
		String buff="";
		boolean OPEN=false;
		for(Operation op:application.operations){
			if(op.name.equalsIgnoreCase("open")||op.name.equalsIgnoreCase("import")){
				for(Data in: op.inputs){
					if(((FileData)in).getFormat().equalsIgnoreCase(input)){
						OPEN=true;
					}
				}
			}
		}
		if(OPEN){
			for(Operation op:application.operations){
				if(op.name.equalsIgnoreCase("save")||op.name.equalsIgnoreCase("export")){
					for(Data out:op.outputs){
						buff+=((FileData)out).getFormat()+"\n";
					}
				}
			}
		}
		for(Operation op:application.operations){
			FOUND=false;
			for(Data in: op.inputs){
				if(((FileData)in).getFormat().equalsIgnoreCase(input)){
					FOUND=true;
					break;
				}
			}
			if(FOUND){
				for(Data out:op.outputs){
					buff+=((FileData)out).getFormat()+"\n";
				}
			}
		}
		return buff;
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
	public String getApplicationTaskInputs(String alias, String task, String output){
		Application application=null;
		for(Application app: applications){
			if(app.alias.equalsIgnoreCase(alias)){
				application = app;
				break;
			}
		}
		if(application==null)
			return "";
		boolean FOUND;
		String buff="";
		boolean SAVE=false;
		for(Operation op:application.operations){
			if(op.name.equalsIgnoreCase("save")||op.name.equalsIgnoreCase("export")){
				for(Data out: op.outputs){
					if(((FileData)out).getFormat().equalsIgnoreCase(output)){
						SAVE=true;
					}
				}
			}
		}
		if(SAVE){
			for(Operation op:application.operations){
				if(op.name.equalsIgnoreCase("open")||op.name.equalsIgnoreCase("import")){
					for(Data in:op.inputs){
						buff+=((FileData)in).getFormat()+"\n";
					}
				}
			}
		}
		for(Operation op:application.operations){
			FOUND=false;
			for(Data out: op.outputs){
				if(((FileData)out).getFormat().equalsIgnoreCase(output)){
					FOUND=true;
					break;
				}
			}
			if(FOUND){
				for(Data in:op.inputs){
					buff+=((FileData)in).getFormat()+"\n";
				}
			}
		}
		return buff;
	}
	// /**
	// * Get the parameters supported by the given task.
	// * @param alias the application alias
	// * @param task the application task
	// * @param output_format the output format
	// * @param input_file the input filename
	// * @return the parameters supported
	// */
	// public String getApplicationTaskParameters(String alias, String task, String output_format, String input_file) //changed: the parameters associated with input_format, output_format will be only from the FIRST option of this pair found
	// {
	// TaskInfo task_info;
	// String buffer = "";
	// String input_ext="";
	// String input_format=input_file;
	// input_ext=Utility.getFilenameExtension(input_file);
	// if(!input_ext.isEmpty())
	// input_format=input_ext;
	//
	// Pair<OperationParameters, OperationParameters> ops;
	// OperationParameters opin, opout;
	// String key = input_format+" "+output_format;
	// for(int i=0; i<applications.size(); i++){
	// if(applications.get(i).alias.equals(alias)){
	// for(Iterator<TaskInfo> itr1=application_tasks.get(i).iterator(); itr1.hasNext();){
	// task_info = itr1.next();
	// if(task_info.name.equals(task)){
	// if((ops=task_info.conv_parameters.get(key))!=null){
	// return "input parameters: \n"+ops.first.toString()+"\noutput parameters: \n"+ops.second.toString()+"\n";
	// }
	//// if(!input_format.equalsIgnoreCase("*") && !task_info.inputs.contains(input_format))
	//// continue;
	//// if(!output_format.equalsIgnoreCase("*") && !task_info.outputs.contains(output_format))
	//// continue;
	// if((opin=task_info.in_parameters.get(input_format))!=null)
	// buffer+="input parameters: \n"+opin.toString()+"\n";
	// if((opout=task_info.out_parameters.get(output_format))!=null)
	// buffer+="output parameters: \n"+opout.toString()+"\n";
	// return buffer;
	// }
	// }
	// break;
	// }
	// }
	// return buffer;
	// }
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
	//TODO

	/**
	 * Recursive - Get HTML for a single parameter (might contain nested elements)
	 * @return the HTML for the parameter
	 */
	Pair<String, String> getJSSingleParam(String prefix, SingleParameter ps, String parent_id, String parent_val,int level){
		String ans="";
		String ind="";
		String enabledisable="";
		for(int i=0; i<(2*level)+4;i++)
			ind+=" ";
		ans += ind+"p = document.createElement(\"P\");\n";
		ans += ind+"tn = document.createTextNode(\""+ps.nameForDisplay;
		if(ps.range)
			ans+=" ["+ps.minRange+", "+ps.maxRange+"]";
		ans +=": \");\n";
		if(!parent_id.isEmpty()){
			enabledisable+=ind+"parent = document.getElementById('"+parent_id+"');\n";
			enabledisable+=ind+"son = document.getElementById('"+prefix+ps.nameForDisplay+"');\n";
			enabledisable+=ind+"son.disabled = (parent.value==\""+parent_val+"\")? false : true;\n";
			// enabledisable+=ind+"if(ed.value == \""+parent_val+"\")\n";
			// enabledisable+=ind+"  document.getElementById(\""+prefix+ps.nameForDisplay+"\").disabled=false;\n";
			// enabledisable+=ind+"else\n";
			// enabledisable+=ind+"  document.getElementById(\""+prefix+ps.nameForDisplay+"\").disabled=true;\n";
		}
		if(ps.continuousParam){
			ans += ind+"input = document.createElement(\"input\");\n";
			ans += ind+"input.id = \""+prefix+ps.nameForDisplay+"\";\n";
			ans += ind+"input.name = \""+prefix+ps.nameForDisplay+"\";\n";
			ans += ind+"input.type = \"text\";\n";
		}
		else{
			ans += ind+"input = document.createElement(\"select\");\n";
			ans += ind+"input.id = \""+prefix+ps.nameForDisplay+"\";\n";
			ans += ind+"input.name = \""+prefix+ps.nameForDisplay+"\";\n";
			// ans += ind+"option=document.createElement(\"option\");\n";
			// ans += ind+"option.value=\"null\";\n";
			ans += ind+"input.onchange = enableDisable;\n";
			//onchange=\"enableDisable();\"
			ans += ind+"input.add(new Option(\"\", \"\", false, false));\n";
			// ans += ind+"option = new Option(\"\", \"\", false, false);\n";
			// ans += ind+"input.appendChild(option);\n";
			for(int i=0; i<ps.possibleValues.size();i++){
				// System.out.println(ps.possibleValues.get(i));
				// ans += ind+"option=document.createElement(\"option\");\n";
				// ans += ind+"option.value=\""+ps.possibleValues.get(i)+"\";\n";
				// ans += ind+"option.appendChild(document.createTextNode(\""+ps.possibleValues.get(i)+"\"));\n";
				// ans += ind+"option = new Option(\""+ps.possibleValues.get(i)+"\", \""+ps.possibleValues.get(i)+"\", false, false);\n";
				// ans += ind+"input.appendChild(option);\n";
				ans += ind+"input.add(new Option(\""+ps.possibleValues.get(i)+"\", \""+ps.possibleValues.get(i)+"\", false, false));\n";
			}
		}
		ans += ind+"p.appendChild(tn);\n";
		ans += ind+"p.appendChild(input);\n";
		ans += ind+"params.appendChild(p);\n";
		Pair<String, String>p=null;
		for(int i=0; i<ps.possibleValues.size();i++){
			if(ps.nestedParams.get(i)!=null){
				for(int j=0; j<ps.nestedParams.get(i).size(); j++){
					if(ps.nestedParams.get(i).get(j)!=null)
						p=getJSSingleParam(prefix,ps.nestedParams.get(i).get(j), prefix+ps.nameForDisplay, ps.possibleValues.get(i),level+1);
					ans+=p.first;
					enabledisable+=p.second;
					// ans+=getJSSingleParam(prefix,ps.nestedParams.get(i).get(j), prefix+ps.nameForDisplay, ps.possibleValues.get(i),level+1);
				}
			}
		}
		return new Pair<String,String>(ans,enabledisable);
	}
	/**
	 * Get the HTML for parameters form
	 * @param application_alias the application to use
	 * @param task_string the task to perform (nothing assumed to be a conversion)
	 * @param output_format the output format
	 * @param input_format the URL of the file to convert
	 */
	public synchronized Pair<String,String> getJSParameters(String application_alias, String task_string, String output_format, String input_format)
	{
		// String buff="   if(input_format == \""+input_format+"\" && output_format == \""+output_format+"\"){\n";
		String params="";
		Vector<Subtask> task;
		Subtask subtask;
		Application application;
		TreeSet<Integer> application_set = new TreeSet<Integer>();
		Operation operation;
		Data input_data, output_data;
		OperationParameters op=null;
		task = getTaskInfo(application_alias, task_string, input_format, output_format);
		if(task.size()==0){
			return new Pair<String,String>("","");
			// return "";
			// buff+="    //conversion not available\n";
			//   buff +="    p = document.createElement(\"P\");\n";
			//   buff +="    tn = document.createTextNode(\"no parameters available for this conversion\");\n";
			//   buff +="    p.appendChild(tn);\n";
			//   buff +="    params.appendChild(p);\n";
		}
		String buff="   if(input_format == \""+input_format+"\" && output_format == \""+output_format+"\"){\n";
		String enabledisable="   if(input_format == \""+input_format+"\" && output_format == \""+output_format+"\"){\n";
		for(int j=0; j<task.size(); j++){
			subtask = task.get(j);
			application = applications.get(subtask.application); application_set.add(subtask.application);
			operation = application.operations.get(subtask.operation);
			input_data = subtask.input_data;
			output_data = subtask.output_data;
			params="";

			buff +="    p = document.createElement(\"P\");\n";
			buff +="    b = document.createElement(\"B\");\n";
			buff +="    tn = document.createTextNode(\"operation: "+operation.name+"\");\n";
			buff +="    b.appendChild(tn);\n";
			buff +="    p.appendChild(b);\n";
			buff +="    params.appendChild(p);\n";  
			//   buff +="    var p"+j+" = document.createElement(\"P\");\n";
			//   buff +="    var tn"+j+" = document.createTextNode(\"operation: "+operation.name+"\");\n";
			//   buff +="    p"+j+".appendChild(tn"+j+");\n";
			//   buff +="    params.appendChild(p"+j+");\n";  

			int param_count=0;
			String str="";
			Pair<String,String>p=null;
			if(input_data!=null){
				if(operation.in_parameters.get(input_data.toString())!=null)
					op=operation.in_parameters.get(input_data.toString());
				SingleParameter ps;
				if(op!=null && op.paramSelections!=null && op.paramSelections.size()>0){
					for(Iterator<SingleParameter> itr=op.paramSelections.iterator(); itr.hasNext();){
						ps=itr.next();
						p=getJSSingleParam(operation.name+"_",ps,"","",0);
						str+=p.first;
						enabledisable+=p.second;
						//   str+=getJSSingleParam(task_string+"_",ps,"","",0);
						//   str+=getJSSingleParam(ps,Integer.toString(param_count),0);
						param_count++;
					}
				}
			}

			if(output_data!=null){
				if(operation.out_parameters.get(output_data.toString())!=null)
					op=operation.out_parameters.get(output_data.toString());
				SingleParameter ps;
				if(op!=null && op.paramSelections!=null && op.paramSelections.size()>0){
					for(Iterator<SingleParameter> itr=op.paramSelections.iterator(); itr.hasNext();){
						ps=itr.next();
						p=getJSSingleParam(operation.name+"_",ps,"","",0);
						str+=p.first;
						enabledisable+=p.second;
						//   str+=getJSSingleParam(task_string+"_",ps,"","",0);
						//   str+=getJSSingleParam(ps,Integer.toString(param_count),0);
						param_count++;
					}
				}
			}

			if(str.isEmpty())
				return new Pair<String,String>("","");
			//   return "";
			// buff+="    //no params for this sw/operation/input/output combination\n";
			else
				buff+=str;
		}
		buff+="\n   }\n";
		enabledisable+="\n   }\n";
		return new Pair<String,String>(buff,enabledisable);
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
				if(FIRST_BLOCK) FIRST_BLOCK = false; else buffer += "\n";
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
		//changed
		buffer += "  setParameters()\n";
		//changed
		buffer += "}\n";
		buffer += "\n";
		String buffer2="";
		//changed
		buffer += "function setParameters(){\n";
		buffer += "  var params = document.getElementById('params');\n";
		buffer += "  params.innerHTML=\"\";\n";
		buffer += "  var select = document.getElementById('application');\n";
		buffer += "  var application = select.options[select.selectedIndex].value;\n";
		buffer += "  var select = document.getElementById('task');\n";
		buffer += "  var task = select.options[select.selectedIndex].value;\n";
		// buffer += "  var inputs = document.getElementById('inputs');\n";
		buffer += "  var select = document.getElementById('format');\n";
		buffer += "  var output_format = select.options[select.selectedIndex].value;\n";
		buffer += "  var file = document.getElementById('file').value;\n";
		buffer += "  var input_format = file.substr(file.lastIndexOf('.') + 1);\n";
		buffer += "  var p, b, tn;\n";
		buffer += "  var li, input, option;\n";
		buffer2 += "function enableDisable(){\n";
		buffer2 += "  var select = document.getElementById('application');\n";
		buffer2 += "  var application = select.options[select.selectedIndex].value;\n";
		buffer2 += "  var select = document.getElementById('task');\n";
		buffer2 += "  var task = select.options[select.selectedIndex].value;\n";
		// buffer2 += "  var inputs = document.getElementById('inputs');\n";
		buffer2 += "  var select = document.getElementById('format');\n";
		buffer2 += "  var output_format = select.options[select.selectedIndex].value;\n";
		buffer2 += "  var file = document.getElementById('file').value;\n";
		buffer2 += "  var input_format = file.substr(file.lastIndexOf('.') + 1);\n";
		buffer2 += "  var parent, son, p, tn;\n";
		// buffer2 += "p = document.createElement(\"P\");\n";
		// buffer2 += "tn = document.createTextNode(\"EM ENABLEDISABLE\");\n";
		// buffer2 += "  var params = document.getElementById('params');\n";
		// buffer2 += "p.appendChild(tn);\n";
		// buffer2 += "params.appendChild(p);\n";
		// buffer2 += "  var p, b, tn;\n";
		// buffer2 += "  var li, input, option;\n";
		String at;
		String at2;
		Pair<String,String> p;
		for(int i=0; i<applications.size(); i++){
			for(Iterator<TaskInfo> itr1=application_tasks.get(i).iterator(); itr1.hasNext();){
				task_info = itr1.next();
				at="";
				at2="";
				for(String out:task_info.outputs){
					for(String in:task_info.inputs){
						p=getJSParameters(applications.get(i).alias, task_info.name, out, in);
						at+=p.first;
						at2+=p.second;
						// at+=getJSParameters(applications.get(i).alias, task_info.name, out, in);
					}
				}
				if(!at.isEmpty()){
					buffer += "  if(application == \"" + applications.get(i).alias + "\" && task == \"" + task_info.name + "\"){\n";
					buffer +=at;
					buffer +="  }\n";
					buffer2 += "  if(application == \"" + applications.get(i).alias + "\" && task == \"" + task_info.name + "\"){\n";
					buffer2 += at2;
					buffer2 +="  }\n";
				}
			}
		}
		buffer+="  enableDisable();\n";
		buffer += "}\n";
		// buffer2 +="  setTimeout('enableDisable()', 500);\n";
		buffer2 += "}\n\n";
		buffer+=buffer2;
		buffer += "\n";
		//changed
		buffer += "function setAPICall(){\n";
		buffer += "  var select = document.getElementById('application');\n";
		buffer += "  var application = select.options[select.selectedIndex].value;\n";
		buffer += "  var select = document.getElementById('task');\n";
		buffer += "  var task = select.options[select.selectedIndex].value;\n";
		buffer += "  var file = document.getElementById('file').value;\n";
		buffer += "  var select = document.getElementById('format');\n";
		buffer += "  var format = select.options[select.selectedIndex].value;\n";
		// buffer += "  var params = document.getElementById('params');\n";
		buffer += "  var nodes = document.getElementById('params').childNodes;\n";
		buffer += "  var curr_val, curr_p, p_operation, p_str;\n";
		buffer += "  var param_str=\"\";\n";
		buffer += "  var curr_op=\"\";\n";
		buffer += "  var first=true;\n";
		buffer += "  var first_p=true;\n";
		buffer += "  var inner_nodes;\n";

		buffer += "  for(i=0; i<nodes.length; i++){\n";
		buffer += "    inner_nodes = nodes[i].childNodes;\n";
		buffer += "    for(j=0; j<inner_nodes.length; j++){\n";
		buffer += "      if(inner_nodes[j].name!=undefined && inner_nodes[j].disabled!=true){\n";
		buffer += "        curr_p=inner_nodes[j].name;\n";
		buffer += "        curr_val=inner_nodes[j].value;\n";
		buffer += "        if(curr_val!=\"\"){\n";
		buffer += "          p_operation=curr_p.substr(0, curr_p.indexOf('_'));\n";
		buffer += "          p_str=curr_p.substr(curr_p.indexOf('_') + 1);\n";
		buffer += "          if(curr_op!=p_operation){\n";
		buffer += "            curr_op=p_operation;\n";
		buffer += "            first_p=true;\n";
		buffer += "            if(first) first=false; else param_str+=\";\";\n";
		buffer += "            param_str+=curr_op+\":\";\n";
		buffer += "          }\n";
		buffer += "          if(first_p) first_p=false; else param_str+=\"&\";\n";
//		buffer += "          param_str+=inner_nodes[j].value;\n";
		buffer += "          param_str+=p_str+\"=\"+curr_val;\n";
		buffer += "        }\n";
		buffer += "      }\n";
		buffer += "    }\n";
		
//		buffer += "    curr_p=nodes[i].name;\n";
		
		buffer += "  }\n";
		buffer += "  if(param_str==\"\" && file!=\"\")\n";
		buffer += "    param_str=\"*\";\n";

		// buffer += "    }\n";
//		buffer += "  }\n";
		// aqui
		if(!POST_UPLOADS){
			buffer += "  var api_url = \"http://\" + location.host + \"/software/\" + application + \"/\" + task + \"/\" + format + \"/\" + encodeURIComponent(file);\n";

			buffer += "  var api_html = \"http://\" + location.host + \"/software/<font color=\\\"#ff7777\\\">\" + application + \"</font>/<font color=\\\"#77ff77\\\">\" + task + \"</font>/<font color=\\\"#7777ff\\\">\" + format + \"</font>/<font color=\\\"#777777\\\">\" + encodeURIComponent(file) + \"</font>\";\n";
			buffer += "  if(file!=\"\")\n";
			buffer += "    api_html +=\"/<font color=\\\"#ff7777\\\">\" + param_str + \"</font>\";\n";

//			buffer += "  var api_html = \"http://\" + location.host + \"/software/<font color=\\\"#ff7777\\\">\" + application + \"</font>/<font color=\\\"#77ff77\\\">\" + task + \"</font>/<font color=\\\"#7777ff\\\">\" + format + \"</font>/<font color=\\\"#777777\\\">\" + encodeURIComponent(file) + \"</font>/<font color=\\\"#77ff77\\\">\" + param_str + \"</font>\";\n";
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
			buffer += "<tr><td><b>File URL:</b></td><td><input type=\"text\" name=\"file\" id=\"file\" size=\"100\" onchange=\"setParameters()\" /></td></tr>\n";
		}else{
			buffer += "<tr><td><b>File:</b></td><td><input type=\"file\" name=\"file\" id=\"file\" size=\"100\" onchange=\"setParameters()\" /></td></tr>\n";
		}
		buffer += "<tr><td><b>Format:</b></td>\n";
		//buffer += "<td><select name=\"format\" id=\"format\" >\n";//changed
		buffer += "<td><select name=\"format\" id=\"format\" onchange=\"setParameters();\">\n";//changed
		for(Iterator<String> itr=application_tasks.get(0).first().outputs.iterator(); itr.hasNext();){
			format = itr.next();
			buffer += "<option value=\"" + format + "\">" + format + "</option>\n";
		}
		buffer += "</select></td></tr>\n";
		//changed
		buffer += "<tr><td><b>Parameters:</b></td>\n";
		buffer += "<tr><td></td><td align=\"left\"><div name=\"params\" id=\"params\">PPPPPPPPPPP</div></td></tr>\n";
		//changed
		buffer += "<tr><td height=\"25\"></td><td></td></tr>\n";
		buffer += "<tr><td></td><td><input type=\"submit\" value=\"Submit\"></td></tr>\n";
		buffer += "<tr><td height=\"25\"></td><td></td></tr>\n";
		buffer += "<tr><td></td><td align=\"center\"><div name=\"api\" id=\"api\"></div></td></tr>\n";
		buffer += "</table>\n";
		buffer += "</form>\n";
		buffer += "</center>\n";
		buffer += "\n";
		buffer += "<script type=\"text/javascript\">setAPICall();</script>\n";
		// buffer += "<script type=\"text/javascript\">enableDisable();</script>\n";
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
	{//TODO: change to support the parameters
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
	public Vector<Subtask> getTaskInfo(String application_alias, String task_string, String input_format, String output_format)
	{
		Task task = new Task(applications);
		task.addSubtasks(task.getApplicationString(application_alias), task_string, new CachedFileData((String)null, input_format), new CachedFileData((String)null, output_format));
		return task.getSubtasks();
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
	 * @param application_alias the application to use
	 * @param task_string the task to perform (nothing assumed to be a conversion)
	 * @param output_format the output format
	 * @param input_format the URL of the file to convert
	 */
	public synchronized String getApplicationTaskParameters(String application_alias, String task_string, String output_format, String input_format)
	{
		String buff="";
		String params="";
		Vector<Subtask> task;
		Subtask subtask;
		Application application;
		TreeSet<Integer> application_set = new TreeSet<Integer>();
		Operation operation;
		Data input_data, output_data;
		task = getTaskInfo(application_alias, task_string, input_format, output_format);
		//System.out.println("BEFORE FOR OF TASK - TASK SIZE IS "+task.size());
		if(task.size()==0)
			return "This conversion cannot be performed by Polyglot at this time. Please consider writing a script.";
		for(int j=0; j<task.size(); j++){
			//     System.out.println(j+" "+buff);
			subtask = task.get(j);
			application = applications.get(subtask.application); application_set.add(subtask.application);
			operation = application.operations.get(subtask.operation);
			input_data = subtask.input_data;
			output_data = subtask.output_data;
			params="";
			buff+="operation: "+operation.name+"\n";
			if(input_data!=null){
				//   buff+="\n********** IN PARAMS: "+j+"\n";
				//   buff+="input_data: "+input_data.toString()+"\n";
				if(operation.in_parameters.get(input_data.toString())!=null)
					params+=operation.in_parameters.get(input_data.toString()).toString();
				//   else buff+="operation: "+operation.name+"\nparameters:\n\tempty";
			}
			if(output_data!=null){
				//   buff+="\n********** OUT PARAMS: "+j+"\n";
				//   buff+="output_data: "+output_data.toString()+"\n";
				if(operation.out_parameters.get(output_data.toString())!=null)
					params+=operation.out_parameters.get(output_data.toString()).toString();
				//   else buff+="operation: "+operation.name+"\nparameters:\n\tempty";
			}
			if(params.isEmpty())
				params="\tempty\n";
			buff+="parameters: \n";
			buff+=params;
		}
		return buff;
	}
	/**
	 * Execute a task.
	 * @param session the session id to use while executing the task
	 * @param application_alias the application to use
	 * @param task_string the task to perform (nothing assumed to be a conversion)
	 * @param file the URL of the file to convert
	 * @param format the output format
	 */
	public synchronized void executeTask(int session, String application_alias, String task_string, String file, String format, String params)
	{
		Vector<Subtask> task;
		String result;
		if(session >= 0){
			if(file.startsWith(Utility.endSlash(getReference().getBaseRef().toString()) + "file/")){ //Remove session id from filenames of locally cached files
				file = SoftwareServer.getFilename(Utility.getFilename(file));
			}else{ //Download remote files
				Utility.downloadFile(server.getCachePath(), session + "_" + Utility.getFilenameName(file), file);
				file = Utility.getFilename(file);
			}
			// System.out.println(application_alias+" "+task_string+" "+file+" "+format+" "+params);
			task = getTask(application_alias, task_string, file, format);
			if(params!=null && !params.equalsIgnoreCase("*") && params.trim().length()!=0){
				Vector<String> args = Utility.split(params, ';', true);
				Vector<String> ops = new Vector<String>();
				Vector<String> ops_params = new Vector<String>();
				Vector<String> vals;
				Vector<String> pv_pairs;
				for(int i=0; i<args.size(); i++){
					vals= Utility.split(args.elementAt(i), ':', true);
					ops.add(vals.firstElement());
					if(vals.size()>1){
						String pv="";
						pv_pairs=Utility.split(vals.elementAt(1), '&', true);
						for(String pair:pv_pairs)
							pv+="\""+pair+"\" ";
						ops_params.add(pv);
					}
					else
						ops_params.add("");
				}
				// for(int i=0; i<ops.size(); i++){
				// System.out.print(ops.elementAt(i)+": ");
				// System.out.println(ops_params.elementAt(i));
				// }
				// Task.print(task, applications);
				// System.out.println("task size: "+task.size()+" operations size: "+ops.size());
				Application application;
				Operation operation;
				Vector<String> task_params=new Vector<String>();
				for(Subtask st:task){
					// System.out.println("*******************");
					boolean found=false;
					application = applications.get(st.application);
					operation = application.operations.get(st.operation);
					// System.out.print(application.alias + " ");
					// System.out.print(operation.name + " ");
					// System.out.print(st.input_data + " ");
					// System.out.print(st.output_data + "\n");
					for(int i=0; i<ops.size();i++){
						if(operation.name.equalsIgnoreCase(ops.get(i))){
							found=true;
							task_params.add(ops_params.get(i));
							break;
						}
					}
					if(!found)
						task_params.add("");
				}
				// for(String tp:task_params)
				// System.out.println(tp);
				result = server.executeTask("localhost", session, task, task_params);
			}
			else{
				result = server.executeTask("localhost", session, task, null);
			}
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
	public void executeTaskLater(int session, String application_alias, String task_string, String file, String format, String params)
	{
		final int session_final = session;
		final String application_alias_final = application_alias;
		final String task_string_final = task_string;
		final String file_final = file;
		final String format_final = format;
		final String params_final = params;
		new Thread(){
			public void run(){
				executeTask(session_final, application_alias_final, task_string_final, file_final, format_final, params_final);
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
		String part5 = (parts.size() > 5) ? parts.get(5) : "";
		String application = null, task = null, file = null, format = null, result, url;
		String params=null;
		String buffer;
		Form form;
		Parameter p;
		int session;
		if(part0.isEmpty()){
			return new StringRepresentation(getApplicationStack(), MediaType.TEXT_HTML);//get available application icons
		}
		else if(part0.equals("software")){
			if(part1.isEmpty()){
				return new StringRepresentation(getApplications(), MediaType.TEXT_PLAIN);//get available applications
			}
			else{//part1=the application name
				if(part2.isEmpty()){
					return new StringRepresentation(getApplicationTasks(part1), MediaType.TEXT_PLAIN);//get tasks available for application chosen
				}
				else{//part2=the application task
					if(part3.isEmpty()){
						return new StringRepresentation(getApplicationTaskOutputs(part1, part2), MediaType.TEXT_PLAIN);//get possible output formats for the task chosen 
					}
					else{//part3=the output format
						if(part3.equals("*") && part4.isEmpty()){//show all input and output formats
							return new StringRepresentation(getApplicationTaskInputsOutputs(part1, part2), MediaType.TEXT_PLAIN);//get list of inputs and outputs for the task
						}
						else if(part3.equals("*") && !part4.isEmpty()){//gimp/convert/*/jpg -> want to know all the convs that can be done from jpg
							return new StringRepresentation(getApplicationTaskOutputs(part1, part2, part4), MediaType.TEXT_PLAIN);
						}
						else if(part4.isEmpty()||part4.equals("*")){//gimp/convert/jpg/* -> want to know all possible input formats for convs resulting in a jpg
							return new StringRepresentation(getApplicationTaskInputs(part1, part2, part3), MediaType.TEXT_PLAIN);//get the input formats supported by the task
						}
						else if(part5.isEmpty()){
							return new StringRepresentation(getApplicationTaskParameters(part1, part2, part3, part4), MediaType.TEXT_PLAIN);//get the parameters supported by the task for this specific input and output formats
						}
						else{
							application = part1;
							task = part2;
							format = part3;
							file = URLDecoder.decode(part4);
							session = -1;
							params = part5;
							if(file.startsWith(Utility.endSlash(getReference().getBaseRef().toString()) + "/file/")){ //Locally cached files already have session ids
								session = SoftwareServer.getSession(file);
								result = Utility.endSlash(getReference().getBaseRef().toString()) + "file/" + Utility.getFilenameName(file) + "." + format;
							}
							else{ //Remote files must be assigned a session id
								session = server.getSession();
								result = Utility.endSlash(getReference().getBaseRef().toString()) + "file/" + session + "_" + Utility.getFilenameName(file) + "." + format;
							}
							executeTaskLater(session, application, task, file, format, params);
							if(isTextOnly(Request.getCurrent())){
								return new StringRepresentation(result, MediaType.TEXT_PLAIN);
							}
							else{
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
		String part4 = (parts.size() > 4) ? parts.get(4) : "";
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
				executeTaskLater(session, application, task, file, format, "");//TODO: add params
				result = Utility.endSlash(getReference().getBaseRef().toString()) + "file/" + Utility.getFilenameName(file) + "." + format;
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
					router.attachDefault(SoftwareServerReslet.class);
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

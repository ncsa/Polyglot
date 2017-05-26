package edu.illinois.ncsa.isda.softwareserver.polyglot;
import edu.illinois.ncsa.isda.softwareserver.polyglot.PolyglotAuxiliary.*;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;
import kgm.utility.Utility;
import org.restlet.data.MediaType;
import org.restlet.representation.StringRepresentation;
import com.mongodb.*;

/**
 * Polyglot REST interface utility functions.
 * @author Kenton McHenry
 */
public class PolyglotRESTUtilities
{
	
	/**
	 * truncate filename to reasonable length
	 * @param filepath full path of file
	 * @return full path of file with truncated filename (containing valid Url encoding)
	 * @throws Exception
	 */
	public static String truncateFileName(String filepath) throws Exception
	{
		//internal filename will be sessionid_jobid_filename.extension(.log), 10 digits of MAX_INTEGER 
		final int DOT_LOG_EXTENSION_LENGTH = 3;
		final int FILENAME_RESERVED_LENGTH = 22 + DOT_LOG_EXTENSION_LENGTH;
		final int MAX_FILENAME_LENGTH = 255;
		final int MAX_EXTENSION_LENGTH = 5;
		
		String extension = Utility.getFilenameExtension(filepath);
		String parent_path = Utility.getFilenamePath(filepath);
		String filename = Utility.getFilename(filepath);
		
		if(extension.length() >= MAX_EXTENSION_LENGTH){
			throw new Exception("do not support long extension file");
		}
      
		int loops = 0;
      
		while(filename.length() >= Integer.MAX_VALUE - FILENAME_RESERVED_LENGTH || filename.length() + FILENAME_RESERVED_LENGTH >= MAX_FILENAME_LENGTH) {
			int half = Math.min(MAX_FILENAME_LENGTH-FILENAME_RESERVED_LENGTH, filename.length())>>1;
			filename = filename.substring(0, half);
			filename = filename + "." + extension;
			filename = URLEncoder.encode(filename, "UTF-8");
          
			if(loops >= 1)
				System.err.println("[truncateFileName]: repeates: " + loops + "; on " + filename);
          
			loops++;
		}
      
		return parent_path + filename;
	}
	
	/**
	 * Get a web form interface for this restful service.
	 * @param polylgot the polyglot steward
	 * @param POST_UPLOADS true if this form should use POST rather than GET for uploading files
	 * @param selected_output the default output format
	 * @return the form
	 */
	public static String getForm(Polyglot polyglot, boolean POST_UPLOADS, String selected_output)
	{
		String buffer = "";
		String output;
		boolean FIRST_BLOCK;
		boolean FIRST_VALUE;
		
		buffer += "<script type=\"text/javascript\">\n";
		buffer += "function setInputs(){\n";
		buffer += "  var inputs = document.getElementById('inputs');\n";
		buffer += "  var outputs = document.getElementById('output');\n";
		buffer += "  var output = outputs.options[outputs.selectedIndex].value;\n";
		buffer += "  \n";
		buffer += "  inputs.innerHTML = \"\";\n";
		buffer += "  \n";
		
		FIRST_BLOCK = true;
		
		for(Iterator<String> itr1=polyglot.getOutputs().iterator(); itr1.hasNext();){
			output = itr1.next();
			if(FIRST_BLOCK) FIRST_BLOCK = false; else	buffer += "\n";
			buffer += "  if(output == \"" + output + "\"){\n";	
			buffer += "    inputs.innerHTML = \"";
			FIRST_VALUE = true;
			
			for(Iterator<String> itr2=polyglot.getInputs(output).iterator(); itr2.hasNext();){
				if(FIRST_VALUE) FIRST_VALUE = false; else buffer += ", ";
				buffer += itr2.next();
			}
			
			buffer += "\";\n";
			buffer += "  }\n";		
		}
		
		buffer += "}\n";
		buffer += "\n";
		buffer += "function setAPICall(){\n";
		buffer += "  var file = document.getElementById('file').value;\n";
		buffer += "  var outputs = document.getElementById('output');\n";
		buffer += "  var output = outputs.options[outputs.selectedIndex].value;\n";
		
		if(!POST_UPLOADS){
			buffer += "  var api_url = \"http://\" + location.host + \"/convert/\" + output + \"/\" + encodeURIComponent(file);\n";
			buffer += "  var api_html = \"http://\" + location.host + \"/convert/<font color=\\\"#7777ff\\\">\" + output + \"</font>/<font color=\\\"#777777\\\">\" + encodeURIComponent(file) + \"</font>\";\n";
		}else{
			buffer += "  var api_url = \"http://\" + location.host + \"/convert/\" + output + \"/\";\n";
			buffer += "  var api_html = \"http://\" + location.host + \"/convert/<font color=\\\"#7777ff\\\">\" + output + \"</font>/\";\n";
		}
		
		buffer += "  \n";
		buffer += "  api.innerHTML = \"<i><b><font color=\\\"#777777\\\">REST API call</font></b><br><br><a href=\\\"\" + api_url + \"\\\"style=\\\"text-decoration:none; color:#777777\\\">\" + api_html + \"</a></i>\";\n";
		buffer += "  setTimeout('setAPICall()', 500);\n";
		buffer += "}\n";
		buffer += "</script>\n";
		buffer += "\n";
		buffer += "<center>\n";
		
		if(!POST_UPLOADS){
			buffer += "<form name=\"conversion\" action=\"\" method=\"get\">\n";
		}else{
			buffer += "<form enctype=\"multipart/form-data\" name=\"conversion\" action=\"\" method=\"post\">\n";
		}
		
		buffer += "<table>\n";
		buffer += "<tr><td><b>Output:</b></td>\n";
		buffer += "<td><select name=\"output\" id=\"output\" onchange=\"setInputs();\">\n";
		
		for(Iterator<String> itr=polyglot.getOutputs().iterator(); itr.hasNext();){
			output = itr.next();
			buffer += "<option value=\"" + output + "\"";
			
			if(selected_output != null && selected_output.equals(output)){
				buffer += " selected";
			}
			
			buffer += ">" + output + "</option>\n";
		}
		
		buffer += "</select></td></tr>\n";	

		if(!POST_UPLOADS){
			buffer += "<tr><td><b>File URL:</b></td><td><input type=\"text\" name=\"file\" id=\"file\" size=\"100\"></td></tr>\n";
		}else{
			buffer += "<tr><td><b>File:</b></td><td><input type=\"file\" name=\"file\" id=\"file\" size=\"100\"></td></tr>\n";
		}
		
		buffer += "<tr><td></td><td width=\"625\"><i><font size=\"-1\"><div id=\"inputs\"></div></font></i></td></tr>\n";
		buffer += "<tr><td></td><td><input type=\"submit\" value=\"Submit\"></td></tr>\n";
		buffer += "<tr><td height=\"25\"></td><td></td></tr>\n";
		buffer += "<tr><td></td><td align=\"center\"><div name=\"api\" id=\"api\"></div></td></tr>\n";
		buffer += "</table>\n";
		buffer += "</form>\n";
		buffer += "</center>\n";
		buffer += "\n";		
		buffer += "<script type=\"text/javascript\">setInputs();</script>\n";
		buffer += "<script type=\"text/javascript\">setAPICall();</script>\n";
		
		return buffer;	
	}
	
	/**
	 * Push logged information to mongo.
	 * @param db a mongo database
	 * @param polyglot the polyglot steward
	 */
	public static void updateMongo(DB db, Polyglot polyglot)
	{
	  DBCollection collection;
		
		//Update active software servers
		collection = db.getCollection("servers");
		collection.drop();

		for(Iterator<String> itr=polyglot.getServers().iterator(); itr.hasNext();){
	   	BasicDBObject document = new BasicDBObject("host", itr.next());
	   	collection.insert(document);
    }

		//Update available software
		collection = db.getCollection("software");
		collection.drop();

		for(Iterator<String> itr=polyglot.getSoftware().iterator(); itr.hasNext();){
	   	BasicDBObject document = new BasicDBObject("name", itr.next());
	   	collection.insert(document);
    }
	
		//Update allowed inputs	
		collection = db.getCollection("inputs");
		collection.drop();

		for(Iterator<String> itr=polyglot.getInputs().iterator(); itr.hasNext();){
	   	BasicDBObject document = new BasicDBObject("extension", itr.next());
	   	collection.insert(document);
    }

		//Update allowed outputs
		collection = db.getCollection("outputs");
		collection.drop();

		for(Iterator<String> itr=polyglot.getOutputs().iterator(); itr.hasNext();){
	   	BasicDBObject document = new BasicDBObject("extension", itr.next());
	   	collection.insert(document);
    }
	}
	
	/**
	 * Push request information to mongo.
	 * @param db a mongo database
	 */
	public static void updateMongo(DB db, RequestInformation request)
	{
	  DBCollection collection = db.getCollection("requests");
		
		BasicDBObject document = new BasicDBObject();
		document.append("address", request.address);
		document.append("filename", request.filename);
		document.append("filesize", request.filesize);
		document.append("input", request.input);
		document.append("output", request.output);
		document.append("start_time", request.start_time);
		document.append("end_time", request.end_time);
		document.append("success", request.success);
		collection.insert(document);
	}

	/**
	 * Convert a Collection of strings to a line separated list of strings.
	 * @param strings a Collection of strings
	 * @return the resulting string representation
	 */
	public static String toString(Collection<String> strings)
	{
		String string = "";
		
		for(Iterator<String> itr=strings.iterator(); itr.hasNext();){
			string += itr.next() + "\n";
		}
		
		return string;
	}
}

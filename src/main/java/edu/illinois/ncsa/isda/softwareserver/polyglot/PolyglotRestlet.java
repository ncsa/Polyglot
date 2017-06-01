package edu.illinois.ncsa.isda.softwareserver.polyglot;
import edu.illinois.ncsa.isda.softwareserver.*;
import edu.illinois.ncsa.isda.softwareserver.SoftwareServerRESTUtilities.*;
import edu.illinois.ncsa.isda.softwareserver.polyglot.PolyglotAuxiliary.*;
import kgm.utility.Utility;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.net.*;
import java.lang.*;
import java.util.*;
import java.util.concurrent.*;
import java.text.*;
import javax.servlet.*;
import javax.xml.bind.DatatypeConverter;
import org.restlet.*;
import org.restlet.resource.*;
import org.restlet.data.*;
import org.restlet.representation.*;
import org.restlet.routing.*;
import org.restlet.security.*;
import org.restlet.ext.fileupload.*;
import org.restlet.ext.json.*;
import org.restlet.engine.header.*;
import org.restlet.engine.application.*;
import org.restlet.util.*;
import org.json.*;
import org.restlet.service.*;
import org.apache.commons.fileupload.*;
import org.apache.commons.fileupload.disk.*;
import org.apache.commons.lang3.*;
import com.mongodb.*;

/**
 * A restful interface for a polyglot steward.
 * Think of this as an extended polyglot steward.
 * @author Kenton McHenry
 */
public class PolyglotRestlet extends ServerResource
{
  private static Polyglot polyglot = new PolyglotStewardAMQ(false);
	private static TreeMap<String,String> accounts = new TreeMap<String,String>();
	private static String authentication_url = null;
	private static String nonadmin_user = "";
	private static int steward_port = -1;
	private static String context = "";
	private static int port = 8184;
	private static boolean RETURN_URL = false;
	private static boolean DOWNLOAD_SS_FILE = false;
	private static boolean MONGO_LOGGING = false;
	private static int mongo_update_interval = 2000;
	private static boolean SOFTWARE_SERVER_REST_INTERFACE = false;
	private static boolean PURGE_JOBS = false;
	private static String download_method = "";
	private static boolean HOST_POSTED_FILES = false;
	private static boolean REDIRECT_TO_SOFTWARESERVER = false;
	private static boolean NEW_TEMP_FOLDERS = true;
	private static String root_path = "./";
	private static String temp_path = root_path + "Temp";
	private static String public_path = root_path + "Public";
	private static Component component;
	
	//Logs
	private static long start_time;
	private static ArrayList<RequestInformation> requests = new ArrayList<RequestInformation>();
  private static MongoClient mongo;
	private static DB db;
	
	/**
	 * Set whether or not to return a URL as the result (as opposed to the file itself).
	 * @param value true if the URL to the resulting file should be returned
	 */
	public void setReturnURL(boolean value)
	{
		RETURN_URL = value;
	}

	/**
	 * Set whether or not to access Software Servers via their REST interface.
	 * @param value true if a Software Servers REST interface should be used
	 */
	public void setSoftwareServerRESTInterface(boolean value)
	{
		SOFTWARE_SERVER_REST_INTERFACE = value;
	}
	
	/**
	 * Set whether or not previous jobs should be purged or not.
	 * @param value true if previous jobs should be purged
	 */
	public void setPurgeJobs(boolean value)
	{
		PURGE_JOBS = value;
	}
	
	/**
	 * Get a web form interface for this restful service.
	 * @param POST_UPLOADS true if this form should use POST rather than GET for uploading files
	 * @param selected_output the default output format
	 * @return the form
	 */
	public String getForm(boolean POST_UPLOADS, String selected_output)
	{
		return PolyglotRESTUtilities.getForm(polyglot, POST_UPLOADS, selected_output);
	}
	
	/**
	 * Get a web form interface for this restful service.
	 * @return the form
	 */
	public String getForm()
	{
		return getForm(false, null);
	}	

	/**
	 * Handle HTTP GET requests.
	 * @return a response
	 */
	@Get
	public Representation httpGetHandler()
	{
		RequestInformation request;
		Vector<String> parts = Utility.split(getReference().getRemainingPart(), '/', true);
		if(parts.size() > 0 && parts.get(0).equals(context)) parts.remove(0);

		String part0 = (parts.size() > 0) ? parts.get(0) : "";
		String part1 = (parts.size() > 1) ? parts.get(1) : "";
		String part2 = (parts.size() > 2) ? parts.get(2) : "";
		String part3 = (parts.size() > 3) ? parts.get(3) : "";
		String client, application = null, file = null, output = null, input = null, result_file = null, result_url, url, type;
		String bd_useremail = "", bd_host = "", bd_token = "";
		String buffer;
		Boolean MAIL = false;
		Form form;
		Parameter p;
		int chain = -1;

		//Parse endpoint	
		if(part0.equals("convert")){
			if(part1.isEmpty()){
				if(SoftwareServerRestlet.isPlainRequest(Request.getCurrent())){
					return new StringRepresentation(PolyglotRESTUtilities.toString(polyglot.getOutputs()), MediaType.TEXT_PLAIN);
				}else{
					return new StringRepresentation(SoftwareServerRESTUtilities.createHTMLList(PolyglotRESTUtilities.toString(polyglot.getOutputs()), Utility.endSlash(getReference().toString()), true, "Outputs"), MediaType.TEXT_HTML);
				}
			}else{
				if(part2.isEmpty()){
					return new StringRepresentation(PolyglotRESTUtilities.toString(polyglot.getInputs(part1)), MediaType.TEXT_PLAIN);
				}else{
					client = getClientInfo().getAddress();
					output = part1;
					file = URLDecoder.decode(part2);
			
					//Check if a specific application is requested
					form = getRequest().getResourceRef().getQueryAsForm();
        	p = form.getFirst("application"); if(p != null) application = p.getValue();
        	p = form.getFirst("mail"); if(p != null) MAIL = Boolean.valueOf(p.getValue());

					if(MAIL){
						Series<Header> series = (Series<Header>)getRequestAttributes().get("org.restlet.http.headers");
    				bd_useremail = series.getFirstValue("X-bd-username");
    				bd_host = series.getFirstValue("Bd-host");
    				bd_token = series.getFirstValue("Bd-access-token");
						bd_useremail += "," + bd_host + "," + bd_token + ",GET";
					}
				
					//Do the conversion
					if(application == null){
						System.out.println("[" + SoftwareServerUtility.getTimeStamp() + "] [restlet]: " + client + " requesting \033[94m" + file + "\033[0m->" + output + " (" + SoftwareServerUtility.getFileSizeHR(file) + ") ...");
					}else{
						System.out.println("[" + SoftwareServerUtility.getTimeStamp() + "] [restlet]: " + client + " requesting \033[94m" + file + "\033[0m->" + output + " using " + application + " (" + SoftwareServerUtility.getFileSizeHR(file) + ") ...");
					}
					
					//Download URLs
					if(!(polyglot instanceof PolyglotStewardAMQ)){
						if(download_method.equals("wget")){
							try{
								Runtime.getRuntime().exec("wget -O " + temp_path + "/" + Utility.getFilenameName(file) + "." + SoftwareServerRESTUtilities.removeParameters(Utility.getFilenameExtension(file)) + " " + file).waitFor();
							}catch(Exception e){e.printStackTrace();}
						}else if(download_method.equals("nio")){
							try{
								URL website = new URL(file);
								ReadableByteChannel rbc = Channels.newChannel(website.openStream());
								FileOutputStream fos = new FileOutputStream(temp_path + "/" + Utility.getFilenameName(file) + "." + SoftwareServerRESTUtilities.removeParameters(Utility.getFilenameExtension(file)));
								fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
							}catch(Exception e){e.printStackTrace();}
						}else{
							//Utility.downloadFile(temp_path, file);
							Utility.downloadFile(temp_path, Utility.getFilenameName(file) + "." + SoftwareServerRESTUtilities.removeParameters(Utility.getFilenameExtension(file)), file);
						}
	
						//file = temp_path + Utility.getFilename(file);
						file = temp_path + SoftwareServerRESTUtilities.removeParameters(Utility.getFilename(file));
					}else{		//Handle files that don't have extensions
						if(Utility.getFilenameExtension(file).isEmpty()){
							if(!part3.isEmpty()){
								input = part3;
								Utility.save(public_path + "/" + Utility.getFilenameName(file) + "." + input + ".url", "[InternetShortcut]\nURL=" + file);
								file = "http://" + Utility.getLocalHostIP() + ":8184/file/" + Utility.getFilename(file) + "." + input;
							}else{
								return new StringRepresentation("Input format not specified", MediaType.TEXT_PLAIN);
							}
						}
					}
					
					request = new RequestInformation(client, file, output);
				
					if(polyglot instanceof PolyglotSteward && SOFTWARE_SERVER_REST_INTERFACE){
						((PolyglotSteward)polyglot).convertOverREST(file, public_path, output);
					}else{
						if(application == null){
							result_file = polyglot.convertAndEmail(file, public_path, output, bd_useremail);
						}else{
							result_file = polyglot.convertAndEmail(application, file, public_path, output, bd_useremail);
						}

						if(result_file.equals("404")){
							setStatus(org.restlet.data.Status.CLIENT_ERROR_NOT_FOUND);
							return new StringRepresentation("File doesn't exist", MediaType.TEXT_PLAIN);
						}
					}

					if(result_file == null) result_file = Utility.getFilenameName(file) + "." + output;		//If a name wasn't suggested assume this.
					result_url = Utility.endSlash(getReference().getBaseRef().toString()) + "file/" + result_file;
					int job_id = SoftwareServerRestlet.getSession(result_url);

					if(Utility.existsAndNotEmpty(public_path + result_file) || Utility.existsAndNotEmpty(public_path + result_file + ".url")){
						request.setEndOfRequest(true);
						System.out.println("[" + SoftwareServerUtility.getTimeStamp() + "] [restlet]" + (job_id >= 0 ? " [" + job_id + "]" : "") + ": " + client + " request for " + file + "->" + output + " will be at \033[94m" + result_url + "\033[0m");
						SoftwareServerUtility.println("[" + SoftwareServerUtility.getTimeStamp() + "] [restlet]" + (job_id >= 0 ? " [" + job_id + "]" : "") + ": " + client + " request for " + file + "->" + output + " will be at " + result_url, public_path + result_file + ".log");
					}else{
						request.setEndOfRequest(false);
						System.out.println("[" + SoftwareServerUtility.getTimeStamp() + "] [restlet]: " + client + " request for " + file + "->" + output + " failed.");
					}
					
					requests.add(request);
					if(MONGO_LOGGING) PolyglotRESTUtilities.updateMongo(db, request);
					
					if(RETURN_URL){		//Return URL of file
						if(SoftwareServerRestlet.isPlainRequest(Request.getCurrent())){
							return new StringRepresentation(result_url, MediaType.TEXT_PLAIN);
						}else{
							return new StringRepresentation("<a href=" + result_url + ">" + result_url + "</a>", MediaType.TEXT_HTML);
						}
					}else{						//Return the file
						result_file = public_path + result_file;
					
						if(Utility.exists(result_file)){
							MetadataService metadata_service = new MetadataService();
							MediaType media_type = metadata_service.getMediaType(output);
							
							if(media_type == null) media_type = MediaType.MULTIPART_ALL;
							
							FileRepresentation file_representation = new FileRepresentation(result_file, media_type);
							file_representation.getDisposition().setType(Disposition.TYPE_INLINE);
							
							return file_representation;
						}else{
							setStatus(org.restlet.data.Status.CLIENT_ERROR_NOT_FOUND);
							return new StringRepresentation("File doesn't exist", MediaType.TEXT_PLAIN);
						}
					}
				}
			}
		}else if(part0.equals("path")){		//Return the conversion path for the given output and input
			if(part1.isEmpty()){
				if(SoftwareServerRestlet.isPlainRequest(Request.getCurrent())){
					return new StringRepresentation(PolyglotRESTUtilities.toString(polyglot.getOutputs()), MediaType.TEXT_PLAIN);
				}else{
					return new StringRepresentation(SoftwareServerRESTUtilities.createHTMLList(PolyglotRESTUtilities.toString(polyglot.getOutputs()), Utility.endSlash(getReference().toString()), true, "Outputs"), MediaType.TEXT_HTML);
				}
			}else{
				if(part2.isEmpty()){
					if(SoftwareServerRestlet.isPlainRequest(Request.getCurrent())){
						return new StringRepresentation(PolyglotRESTUtilities.toString(polyglot.getInputs(part1)), MediaType.TEXT_PLAIN);
					}else{
						return new StringRepresentation(SoftwareServerRESTUtilities.createHTMLList(PolyglotRESTUtilities.toString(polyglot.getInputs(part1)), Utility.endSlash(getReference().toString()), true, "Inputs"), MediaType.TEXT_HTML);
					}
				}else{
					Vector<PolyglotAuxiliary.Conversion<String,String>> conversions = polyglot.getInputOutputGraph().getShortestConversionPath(part2, part1, false);
					JSONArray json = new JSONArray();
					JSONObject step;

					if(conversions != null){
						try{
							for(int i=0; i<conversions.size(); i++){
								step = new JSONObject();
								step.put("application", conversions.get(i).edge);
								step.put("input", conversions.get(i).input);
								step.put("output", conversions.get(i).output);
								json.put(step);
							}
						}catch(Exception e) {e.printStackTrace();}
					}

					return new JsonRepresentation(json);
				}
			}
		}else if(part0.equals("form")){
			if(part1.isEmpty()){
				buffer = "";
				buffer += "get\n";
				buffer += "post\n";
				
				if(SoftwareServerRestlet.isPlainRequest(Request.getCurrent())){
					return new StringRepresentation(buffer, MediaType.TEXT_PLAIN);
				}else{
					return new StringRepresentation(SoftwareServerRESTUtilities.createHTMLList(buffer, Utility.endSlash(getReference().toString()), true, "Forms"), MediaType.TEXT_HTML);
				}
			}else{
				form = getRequest().getResourceRef().getQueryAsForm();
				p = form.getFirst("file"); if(p != null) file = p.getValue();
				p = form.getFirst("output"); if(p != null) output = p.getValue();
								
				if(file != null && output != null){
					try{
						file = URLEncoder.encode(file, "UTF-8");
					}catch(Exception e) {e.printStackTrace();}
						
					url = getRootRef() + "convert/" + output + "/" + file;
	
					return new StringRepresentation("<html><head><meta http-equiv=\"refresh\" content=\"1; url=" + url + "\"></head</html>", MediaType.TEXT_HTML);
				}else{
					if(part1.startsWith("get")){
						return new StringRepresentation(getForm(false, output), MediaType.TEXT_HTML);
					}else if(part1.startsWith("post")){
						return new StringRepresentation(getForm(true, output), MediaType.TEXT_HTML);
					}else{
						return new StringRepresentation("error: invalid endpoint", MediaType.TEXT_PLAIN);
					}
				}
			}
		}else if(part0.equals("file")){
			if(!part1.isEmpty()){
				part1 = SoftwareServerRESTUtilities.removeParameters(part1);
				file = public_path + part1;

				//Workaround: If the file is not there, but file.url exists, retrieve and put it there. Configurable using the "DownloadSSFile" field in PolyglotRestlet.conf.
				if(DOWNLOAD_SS_FILE){
					if((!Utility.exists(file)) && (Utility.exists(file + ".url"))){
						//System.out.println("File does not exist, but file.url '" + file + ".url' exists.");
						result_url = Utility.getLine(file + ".url", 2).substring(4).trim();		//Link is on 2nd line after "URL="

						if(result_url.isEmpty()){
							for(int i=0; i<10; i++){
								System.out.println("result_url is empty, sleeping for 1 second...");
								
								try{
									Thread.sleep(1000);
								}catch(Exception e) {e.printStackTrace();}

								result_url = Utility.getLine(file + ".url", 2).substring(4).trim();		//Link is on 2nd line after "URL="
							}
						}

						System.out.println("result_url = '" + result_url + "'.");

						if(!result_url.isEmpty()){
							//System.out.println("About to download '" + result_url + "' to file '" + file + "'");
							Boolean result_status = Utility.downloadFile(public_path, part1, result_url, false);
							System.out.println("Downloaded '" + result_url + "' to file '" + file + "', result status is " + result_status);
						}
					}
				}

				if(Utility.exists(file)){
					if(Utility.isDirectory(file)){
						url = Utility.endSlash(getRootRef().toString()) + "file/" + part1 + "/";

						//Redirect to directory endpoint, added a "/"
						this.getResponse().redirectTemporary(url);
						return new StringRepresentation("Redirecting...", MediaType.TEXT_PLAIN);
					}else{
						MetadataService metadata_service = new MetadataService();
						MediaType media_type = metadata_service.getMediaType(SoftwareServerRESTUtilities.removeParameters(Utility.getFilenameExtension(part1)));
					
						if(media_type == null){
							if(Utility.getFilenameExtension(part1).equals("log")){
								media_type = MediaType.TEXT_PLAIN;
							}else{
								media_type = MediaType.MULTIPART_ALL;
							}
						}
							
						FileRepresentation file_representation = new FileRepresentation(file, media_type);
						//file_representation.getDisposition().setType(Disposition.TYPE_INLINE);
						return file_representation;
					}
				}else{
					if(Utility.exists(file + ".url")){
						result_url = Utility.getLine(file + ".url", 2).substring(4).trim();		//Link is on 2nd line after "URL="

						if(!result_url.isEmpty()){
							this.getResponse().redirectTemporary(result_url);
							return new StringRepresentation("Redirecting...", MediaType.TEXT_PLAIN);
						}else{
							setStatus(org.restlet.data.Status.CLIENT_ERROR_NOT_FOUND);
							return new StringRepresentation("File doesn't exist", MediaType.TEXT_PLAIN);
						}
					}else{
						setStatus(org.restlet.data.Status.CLIENT_ERROR_NOT_FOUND);
						return new StringRepresentation("File doesn't exist", MediaType.TEXT_PLAIN);
					}
				}
			}else{
				return new StringRepresentation("error: invalid endpoint", MediaType.TEXT_PLAIN);
			}
		}else if(part0.equals("image")){
			if(!part1.isEmpty()){	
				file = "images/" + Utility.getFilename(Utility.unixPath(part1));
				
				if(Utility.exists(file)){
					type = Utility.getFilenameExtension(file);
					
					if(type.equals("png")){
						return new FileRepresentation(file, MediaType.IMAGE_PNG);
					}else if(type.equals("gif")){
						return new FileRepresentation(file, MediaType.IMAGE_GIF);
					}else{
						return new FileRepresentation(file, MediaType.IMAGE_JPEG);
					}
				}else{
					return new StringRepresentation("Image doesn't exist", MediaType.TEXT_PLAIN);
				}
			}else{
				return new StringRepresentation("error: invalid endpoint", MediaType.TEXT_PLAIN);
			}
		}else if(part0.equals("alive")){
			return new StringRepresentation("yes", MediaType.TEXT_PLAIN);
		}else if(part0.equals("checkin") && polyglot instanceof PolyglotStewardAMQ){
			if(!part1.isEmpty()){
				try{
					final int job_id = Integer.parseInt(part1);
					String log_file, ss_log_file = "", line;
				
					if(!part2.isEmpty()){
						try{
							part2 = URLDecoder.decode(part2, "UTF-8");
						}catch(Exception e) {e.printStackTrace();}
					
						//Find relevant log file to append to. TODO: Replace with an index to avoid this possibly costly search step
						File[] files = new File(public_path).listFiles(new FilenameFilter() {
							public boolean accept(File dir, String name) {
								return name.startsWith(job_id + "_") && name.endsWith(".log");
							}
						});

						log_file = files[0].getAbsolutePath();
	
						System.out.println("[" + SoftwareServerUtility.getTimeStamp() + "] [restlet] [" + job_id + "]: Software Server at " + getClientInfo().getAddress() + " checked in result for job-" + job_id + ", \033[94m" + part2 + "\033[0m" + " (" + SoftwareServerUtility.getFileSizeHR(part2) + ")");
						SoftwareServerUtility.println("[" + SoftwareServerUtility.getTimeStamp() + "] [restlet] [" + job_id + "]: Software Server at " + getClientInfo().getAddress() + " checked in result for job-" + job_id + ", " + part2 + " (" + SoftwareServerUtility.getFileSizeHR(part2) + ")", log_file);
						
						//Append Software Server log
						SoftwareServerUtility.println("[" + SoftwareServerUtility.getTimeStamp() + "] [restlet] [" + job_id + "]: [Begin Software Server Log - " + getClientInfo().getAddress() + "] =========", log_file);
						Scanner s = new Scanner(SoftwareServerUtility.readURL(part2 + ".log", null).trim());

						while(s.hasNextLine()){
							line = s.nextLine();

							if(line.contains("Setting session to")) ss_log_file = "";	//If this server has done several parts don't duplicate previous log outputs!
							ss_log_file += line + "\n";
						}

						SoftwareServerUtility.println(ss_log_file.trim(), log_file);
						SoftwareServerUtility.println("[" + SoftwareServerUtility.getTimeStamp() + "] [restlet] [" + job_id + "]: ============ [End Software Server Log - " + getClientInfo().getAddress() + "]", log_file);

						return new StringRepresentation(((PolyglotStewardAMQ)polyglot).checkin(getClientInfo().getAddress(), job_id, part2), MediaType.TEXT_PLAIN);
					}else{
						return new StringRepresentation("error: invalid file", MediaType.TEXT_PLAIN);
					}
				}catch(NumberFormatException e){
					return new StringRepresentation("error: invalid job id", MediaType.TEXT_PLAIN);
				}
			}else{
				return new StringRepresentation("error: invalid job id", MediaType.TEXT_PLAIN);
			}
		}else if(part0.equals("servers")){
			if(part1.isEmpty()){
				if(SoftwareServerRestlet.isPlainRequest(Request.getCurrent())){
					return new StringRepresentation(PolyglotRESTUtilities.toString(polyglot.getServers()), MediaType.TEXT_PLAIN);
				}else{
					return new StringRepresentation(SoftwareServerRESTUtilities.createHTMLList(PolyglotRESTUtilities.toString(polyglot.getServers()), Utility.endSlash(getReference().toString()), true, "Servers"), MediaType.TEXT_HTML);
				}
			}else{
				String ss_ip = part1.split(":")[0];
				url = "http://" + ss_ip + ":8182";

				//Add on additional parts if any
				if(!part2.isEmpty()) url += "/software";

				for(int i=2; i<parts.size(); i++){
					url += "/" + parts.get(i);
				}

				//Redirect to specified Software Server
				this.getResponse().redirectTemporary(url);
				return new StringRepresentation("Redirecting...", MediaType.TEXT_PLAIN);
			}
		}else if(part0.equals("software")){
			if(part1.isEmpty()){
				if(SoftwareServerRestlet.isPlainRequest(Request.getCurrent())){
					return new StringRepresentation(PolyglotRESTUtilities.toString(polyglot.getSoftware()), MediaType.TEXT_PLAIN);
				}else{
					return new StringRepresentation(SoftwareServerRESTUtilities.createHTMLList(PolyglotRESTUtilities.toString(polyglot.getSoftware()), Utility.endSlash(getReference().toString()), true, "Software"), MediaType.TEXT_HTML);
				}
			}else{
				if(REDIRECT_TO_SOFTWARESERVER){
					//Find a server with the specified application.
					TreeSet<String> swHosts = ((PolyglotStewardAMQ)polyglot).getSoftwareHosts();
					String targetPrefix = part1 + ":";

					for(String sh:swHosts) {
						if(sh.startsWith(targetPrefix)) {
							String ip = sh.split(":")[1];
							System.out.println("[" + SoftwareServerUtility.getTimeStamp() + "] [restlet]: Redirecting request for " + part1 + " to " + ip);

							//Redirect to found software server
							url = "http://" + ip + ":8182/software";

							for(int k=1; k<parts.size(); k++){
								url += "/" + parts.get(k);
							}

							this.getResponse().redirectTemporary(url);
							return new StringRepresentation("Redirecting...", MediaType.TEXT_PLAIN);
						}
					}
				}else{
					if(part2.isEmpty()){
						if(SoftwareServerRestlet.isPlainRequest(Request.getCurrent())){
							return new StringRepresentation(PolyglotRESTUtilities.toString(polyglot.getInputOutputGraph().getEdgeOutputs(part1)), MediaType.TEXT_PLAIN);
						}else{
							return new StringRepresentation(SoftwareServerRESTUtilities.createHTMLList(PolyglotRESTUtilities.toString(polyglot.getInputOutputGraph().getEdgeOutputs(part1)), Utility.endSlash(getReference().toString()), true, "Outputs"), MediaType.TEXT_HTML);
						}
					}else{
						if(part3.isEmpty()){
							if(SoftwareServerRestlet.isPlainRequest(Request.getCurrent())){
								return new StringRepresentation(PolyglotRESTUtilities.toString(polyglot.getInputOutputGraph().getEdgeInputs(part1)), MediaType.TEXT_PLAIN);
							}else{
								return new StringRepresentation(SoftwareServerRESTUtilities.createHTMLList(PolyglotRESTUtilities.toString(polyglot.getInputOutputGraph().getEdgeInputs(part1)), Utility.endSlash(getReference().toString()), true, "Inputs"), MediaType.TEXT_HTML);
							}
						}
					}
				}

				return new StringRepresentation("error: application not available", MediaType.TEXT_PLAIN);
			}
		}else if(part0.equals("inputs")){
			if(part1.isEmpty()){
				if(SoftwareServerRestlet.isPlainRequest(Request.getCurrent())){
					return new StringRepresentation(PolyglotRESTUtilities.toString(polyglot.getInputs()), MediaType.TEXT_PLAIN);
				}else{
					return new StringRepresentation(SoftwareServerRESTUtilities.createHTMLList(PolyglotRESTUtilities.toString(polyglot.getInputs()), Utility.endSlash(getReference().toString()), true, "Inputs"), MediaType.TEXT_HTML);
				}
			}else{
				part1 = SoftwareServerRESTUtilities.removeParameters(part1).toLowerCase();	//Ignore case of file extension
	
				form = getRequest().getResourceRef().getQueryAsForm();
        p = form.getFirst("chain"); if(p != null) chain = Integer.parseInt(p.getValue());

				if(SoftwareServerRestlet.isJSONRequest(Request.getCurrent())){
					return new JsonRepresentation(new JSONArray(polyglot.getOutputs(part1, chain)));
				}else{
					return new StringRepresentation(PolyglotRESTUtilities.toString(polyglot.getOutputs(part1, chain)), MediaType.TEXT_PLAIN);
				}
			}
		}else if(part0.equals("outputs")){				
			return new StringRepresentation(PolyglotRESTUtilities.toString(polyglot.getOutputs()), MediaType.TEXT_PLAIN);
		}else if(part0.equals("requests")){
			buffer = Long.toString(start_time) + "\n";
			buffer += System.currentTimeMillis() + "\n";
			
			for(Iterator<RequestInformation> itr=requests.iterator(); itr.hasNext();){
				buffer += itr.next().toString() + "\n";
			}
			
			return new StringRepresentation(buffer, MediaType.TEXT_PLAIN);
		}else if(part0.equals("requests?hr=true")){
			buffer = new SimpleDateFormat("yyyy-mm-dd HH:mm:ss").format(new Date(start_time)) + "\n";
			buffer += new SimpleDateFormat("yyyy-mm-dd HH:mm:ss").format(new Date(System.currentTimeMillis())) + "\n";
			
			for(Iterator<RequestInformation> itr=requests.iterator(); itr.hasNext();){
				buffer += itr.next().toString() + "\n";
			}
			
			return new StringRepresentation(buffer, MediaType.TEXT_PLAIN);
		}else{
			buffer = "";
			buffer += "convert\n";
			buffer += "path\n";
			buffer += "form\n";
			buffer += "alive\n";
			buffer += "inputs\n";
			buffer += "outputs\n";
			buffer += "servers\n";
			buffer += "software\n";
			buffer += "alive\n";
			buffer += "requests\n";
			
			if(SoftwareServerRestlet.isPlainRequest(Request.getCurrent())){
				return new StringRepresentation(buffer, MediaType.TEXT_PLAIN);
			}else{
				return new StringRepresentation(SoftwareServerRESTUtilities.createHTMLList(buffer, Utility.endSlash(getReference().toString()), true, "Endpoints"), MediaType.TEXT_HTML);
			}
		}
	}
	
	/**
	 * Handle HTTP POST requests.
	 * @param entity the entity
	 * @return a response
	 */
	@Post
	public Representation httpPostHandler(Representation entity)
	{
		RequestInformation request;
		Vector<String> parts = Utility.split(getReference().getRemainingPart(), '/', true);
		if(parts.size() > 0 && parts.get(0).equals(context)) parts.remove(0);

		String part0 = (parts.size() > 0) ? parts.get(0) : "";
		String part1 = (parts.size() > 1) ? parts.get(1) : "";
		String part2 = (parts.size() > 2) ? parts.get(2) : "";
		boolean FORM_POST = !part0.isEmpty() && part0.equals("form");
		boolean SOFTWARESERVER_POST = !part0.isEmpty() && (part0.equals("servers") || part0.equals("software") || part0.equals("checkin"));
		boolean TASK_POST = !part1.isEmpty() && !part0.equals("servers") && !part0.equals("software") && !part0.equals("checkin");
		TreeMap<String,String> parameters = new TreeMap<String,String>();
		String application=null, file=null, output = null, result_file = null, result_url, url;
		String bd_useremail = "", bd_host = "", bd_token = "";
		String client = getClientInfo().getAddress();
		Boolean MAIL = false;
	
		int jobid = 0;
		
		if(FORM_POST || TASK_POST || SOFTWARESERVER_POST){
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
						}else{ // do checkin or copy posted file
							if(part0.equals("checkin")) {
								String filename, file_url;
								String log_file, ss_log_file = "", sf_log, line;
								String tmp_file_name = (fi.getName()).replace(" ","_");
						
								if(StringUtils.isNumeric(part1) && tmp_file_name.indexOf('_') != -1){                     
									final int job_id = Integer.parseInt(part1);

									// The file name posted by SS contains SS session id, but Polyglot needs to save it using Polyglot job id. So change the file name. "part1" is job_id.
									filename = tmp_file_name.substring(tmp_file_name.indexOf('_')+1);
									if(!filename.startsWith(job_id + "_")) filename = job_id + "_" + filename;	//TODO: warning if by chance the original filename by coincedence started with the job id number and an underscore!?
									file = public_path + filename;
									fi.write(new File(file));

									//Unzip the checkin if its a directory
									if(file.endsWith(".checkin.zip")){
										SoftwareServerUtility.unzip(public_path, file);
										filename = Utility.getFilenameName(file);
										filename = filename.substring(0, filename.length()-8);

										//Attach directory as a new endpoint
										Directory directory = new Directory(getContext(), "file://" + Utility.absolutePath(public_path + filename));
										directory.setListingAllowed(true);

										//Add CORS filter
										CorsFilter corsfilter = new CorsFilter(getContext(), directory);
										corsfilter.setAllowedOrigins(new HashSet<String>(Arrays.asList("*")));
										corsfilter.setAllowedCredentials(true);
										component.getDefaultHost().attach("/file/" + filename + "/", corsfilter);
									}

									file_url = Utility.endSlash(getReference().getBaseRef().toString()) + "file/" + filename;
						
									//Find relevant log file to append to. TODO: Replace with an index to avoid this possibly costly search step
									File[] files = new File(public_path).listFiles(new FilenameFilter() {
										public boolean accept(File dir, String name) {
											return name.startsWith(job_id + "_") && name.endsWith(".log");
										}
									});

									log_file = files[0].getAbsolutePath();

									System.out.println("[" + SoftwareServerUtility.getTimeStamp() + "] [restlet] [" + job_id + "]: Software Server at " + getClientInfo().getAddress() + " checked in result for job-" + job_id + ", \033[94m" + file_url + "\033[0m" + " (" + SoftwareServerUtility.getFileSizeHR(file) + ")");
									SoftwareServerUtility.println("[" + SoftwareServerUtility.getTimeStamp() + "] [restlet] [" + job_id + "]: Software Server at " + getClientInfo().getAddress() + " checked in result for job-" + job_id + ", " + file_url + " (" + SoftwareServerUtility.getFileSizeHR(file) + ")", log_file);
						
									//Append Software Server log
									SoftwareServerUtility.println("[" + SoftwareServerUtility.getTimeStamp() + "] [restlet] [" + job_id + "]: [Begin Software Server Log - " + getClientInfo().getAddress() + "] =========", log_file);
									Scanner s = new Scanner(URLDecoder.decode(part2).trim());

									while(s.hasNextLine()){
										line = s.nextLine();

										if(line.contains("Setting session to")) ss_log_file = "";	//If this server has done several parts don't duplicate previous log outputs!
										ss_log_file += line + "\n";
									}

									SoftwareServerUtility.println(ss_log_file.trim(), log_file);
									SoftwareServerUtility.println("[" + SoftwareServerUtility.getTimeStamp() + "] [restlet] [" + job_id + "]: ============ [End Software Server Log - " + getClientInfo().getAddress() + "]", log_file);
						
									//Append Siegfried output
									sf_log = SoftwareServerUtility.executeAndWait("sf " + file, 60000, true, true);

									if(sf_log != null){
										SoftwareServerUtility.println("[" + SoftwareServerUtility.getTimeStamp() + "] [restlet] [" + job_id + "]: [Begin Siegfried Log - " + Utility.getFilename(file) + "] =========", log_file);
										SoftwareServerUtility.print(sf_log, log_file);
										SoftwareServerUtility.println("[" + SoftwareServerUtility.getTimeStamp() + "] [restlet] [" + job_id + "]: ============ [End Siegfried Log - " + Utility.getFilename(file) + "]", log_file);
									}

									return new StringRepresentation(((PolyglotStewardAMQ)polyglot).checkin(getClientInfo().getAddress(), job_id, file_url), MediaType.TEXT_PLAIN);
								}else{
									System.out.println("[" + SoftwareServerUtility.getTimeStamp() + "] [restlet]: File not being processed due to security concern - either the file id is non-numeric or the filename is invalid.");
									continue;
								}
							}else {
								//1. increate jobid and url encode input filename
								jobid = polyglot.incrementAndGetJobID();
								String filename = fi.getName();
								try {
									filename = URLEncoder.encode(filename, "UTF-8");
								}catch (UnsupportedEncodingException ex) {
									ex.printStackTrace();
								}
								
								//2. rename posted file as uniqueness and copy to Polyglot folder
								if(HOST_POSTED_FILES){ // add jobid_ as prefix and copy posted file to Public folder
									file = public_path + jobid + "_" + (filename);
									fi.write(new File(file));
	
									String extension = Utility.getFilenameExtension(fi.getName());
	
									if(extension.isEmpty()){		//If no extension add one
										String myCommand ="trid -r:1 -ae " + public_path + (fi.getName()).replace(" ","_") + " | grep % "+ "| awk  '{print tolower($2) }'" + "|  sed 's/^.\\(.*\\).$/\\1/'";
										// the 'trid' command can be obtained at http://mark0.net/soft-trid-e.html
										Process p = Runtime.getRuntime().exec(new String[] {"sh", "-c", myCommand});
										p.waitFor();
										BufferedReader buf = new BufferedReader(new InputStreamReader(p.getInputStream()));
										extension = buf.readLine();
										Utility.pause(1000);
										
										file += extension;
									}
	
									//file = "http://" + InetAddress.getLocalHost().getHostAddress() + ":8184/file/" + Utility.getFilename(file);
									file = "http://" + Utility.getLocalHostIP() + ":8184/file/" + Utility.getFilename(file);
									if(!nonadmin_user.isEmpty()) file = SoftwareServerUtility.addAuthentication(file, nonadmin_user + ":" + accounts.get(nonadmin_user));
									System.out.println("[" + SoftwareServerUtility.getTimeStamp() + "] [restlet]: Temporarily hosting file \"" + Utility.getFilename(file) + "\" for " + client + " at " + file);
								}else{ // add jobid_ as prefix and copy posted file to Temp folder
									file = temp_path + jobid + "_" + (filename);
									fi.write(new File(file));
								}
							}
						}
					}
				}catch(Exception e) {e.printStackTrace();}
			}
			
			if(FORM_POST){
				output = parameters.get("output");
			}else if(TASK_POST){
				output = part1;				
			}

			//Do the conversion
			if(file != null && output != null){
				//Check if a specific application is requested
				application = parameters.get("application");
				MAIL = Boolean.valueOf(parameters.get("mail"));

				if(MAIL){
					Series<Header> series = (Series<Header>)getRequestAttributes().get("org.restlet.http.headers");
    			bd_useremail = series.getFirstValue("X-bd-username");
    			bd_host = series.getFirstValue("Bd-host");
    			bd_token = series.getFirstValue("Bd-access-token");
					bd_useremail += "," + bd_host + "," + bd_token + ",POST";
				}

				if(application == null){
					//Removed calling of SoftwareServerUtility.getFileSizeHR(file), which seems to cause a deadlock when "file" is hosted on this host:8184 itself.
					//System.out.println("[" + SoftwareServerUtility.getTimeStamp() + "] [restlet]: " + client + " requesting \033[94m" + file + "\033[0m->" + output + " (" + SoftwareServerUtility.getFileSizeHR(file) + ") ...");
					System.out.println("[" + SoftwareServerUtility.getTimeStamp() + "] [restlet]: " + client + " requesting \033[94m" + file + "\033[0m->" + output + " ...");
				}else{
					System.out.println("[" + SoftwareServerUtility.getTimeStamp() + "] [restlet]: " + client + " requesting \033[94m" + file + "\033[0m->" + output + " using " + application + " ...");
				}
				
				request = new RequestInformation(client, file, output);

				if(polyglot instanceof PolyglotSteward && SOFTWARE_SERVER_REST_INTERFACE){
					((PolyglotSteward)polyglot).convertOverREST(file, public_path, output);
				}else{
					if(application == null){
						result_file = polyglot.convertAndEmail(jobid, file, public_path, output, bd_useremail);
					}else{
						result_file = polyglot.convertAndEmail(jobid, application, file, public_path, output, bd_useremail);
					}
					// remove jobid_ added by convertAndEmail, since we already added extra jobid_ as the prefix of posted filename.
					result_file = result_file.substring(result_file.indexOf('_')+1);
				}
				
				// create empty .log file under public path
				Utility.touch(public_path + result_file + ".log");

				if(result_file == null) result_file = Utility.getFilenameName(file) + "." + output;
				result_url = Utility.endSlash(getReference().getBaseRef().toString()) + "file/" + result_file;
				int job_id = SoftwareServerRestlet.getSession(result_url);

				if(Utility.existsAndNotEmpty(public_path + result_file) || Utility.existsAndNotEmpty(public_path + result_file + ".url")){
					request.setEndOfRequest(true);
					System.out.println("[" + SoftwareServerUtility.getTimeStamp() + "] [restlet]" + (job_id >= 0 ? " [" + job_id + "]" : "") + ": " + client + " request for " + file + "->" + output + " will be at \033[94m" + result_url + "\033[0m");
					SoftwareServerUtility.println("[" + SoftwareServerUtility.getTimeStamp() + "] [restlet]" + (job_id >= 0 ? " [" + job_id + "]" : "") + ": " + client + " request for " + SoftwareServerUtility.removeCredentials(file) + "->" + output + " will be at " + result_url, public_path + result_file + ".log");
				}else{
					request.setEndOfRequest(false);
					System.out.println("[" + SoftwareServerUtility.getTimeStamp() + "] [restlet]: " + client + " request for " + file + "->" + output + " failed.");
				}
				
				requests.add(request);
				if(MONGO_LOGGING) PolyglotRESTUtilities.updateMongo(db, request);
				
				if(RETURN_URL){		//Return URL of file
					if(SoftwareServerRestlet.isPlainRequest(Request.getCurrent())){
						return new StringRepresentation(result_url, MediaType.TEXT_PLAIN);
					}else{
						return new StringRepresentation("<a href=" + result_url + ">" + result_url + "</a>", MediaType.TEXT_HTML);
					}
				}else{						//Return the file
					result_file = public_path + result_file;
					
					if(Utility.exists(result_file)){
						MetadataService metadata_service = new MetadataService();
						MediaType media_type = metadata_service.getMediaType(output);
						
						if(media_type == null) media_type = MediaType.MULTIPART_ALL;
						
						FileRepresentation file_representation = new FileRepresentation(result_file, media_type);
						file_representation.getDisposition().setType(Disposition.TYPE_INLINE);
						
						return file_representation;
					}else{
						setStatus(org.restlet.data.Status.CLIENT_ERROR_NOT_FOUND);
						return new StringRepresentation("File doesn't exist", MediaType.TEXT_PLAIN);
					}
				}
			}
		}

		//Check for direct software server requests
		if(part0.equals("servers")){
			if(!part1.isEmpty()){
				url = "http://" + part1 + ":8182";

				//Add on additional parts if any
				if(!part2.isEmpty()) url += "/software";

				for(int i=2; i<parts.size(); i++){
					url += "/" + parts.get(i);
				}
							
				//TODO: For some reasone this doesn't seem to be necessary for the task to go through?
				try{
					url += "/" + URLEncoder.encode(file, "UTF-8");
				}catch(Exception e) {e.printStackTrace();}

				//Redirect to specified Software Server
				this.getResponse().redirectTemporary(url);
				return new StringRepresentation("Redirecting...", MediaType.TEXT_PLAIN);
			}
		}else if(part0.equals("software")){
			if(!part1.isEmpty()){
				//Find a server with the specified application, TODO: this may need to be more efficient
				Vector<String> servers = polyglot.getServers();

				for(int i=0; i<servers.size(); i++){
					String[] lines = SoftwareServerUtility.readURL("http://" + servers.get(i) + ":8182/software", "text/plain").split("\\r?\\n");

					for(int j=0; j<lines.length; j++){
						if(lines[j].split(" ")[0].equals(part1)){
							System.out.println("[" + SoftwareServerUtility.getTimeStamp() + "] [restlet]: Redirecting request for " + part1 + " to " + servers.get(i));

							//Redirect to found software server
							url = "http://" + servers.get(i) + ":8182/software";

							for(int k=1; k<parts.size(); k++){
								url += "/" + parts.get(k);
							}

							//TODO: For some reasone this doesn't seem to be necessary for the task to go through?
							try{
								url += "/" + URLEncoder.encode(file, "UTF-8");
							}catch(Exception e) {e.printStackTrace();}

							this.getResponse().redirectTemporary(url);
							return new StringRepresentation("Redirecting...", MediaType.TEXT_PLAIN);
						}
					}
				}

				return new StringRepresentation("error: application not available", MediaType.TEXT_PLAIN);
			}
		}
		
		return httpGetHandler();
	}
	
	/**
	 * Stop the REST interface.
	 */
	public void stop()
	{		
		try{
			component.stop();
		}catch(Exception e) {e.printStackTrace();}
		
		polyglot.close();
	}
	
	/**
	 * Start the Polyglot REST interface.
	 * @param args command line arguments
	 */
	public static void main(String[] args)
	{		
		//Load configuration file
	  try{
	    BufferedReader ins = new BufferedReader(new FileReader("PolyglotRestlet.conf"));
	    String line, key, value;
	    String server, username, password;
	    int tmpi;
	    
	    while((line=ins.readLine()) != null){
	      if(line.contains("=")){
	        key = line.substring(0, line.indexOf('='));
	        value = line.substring(line.indexOf('=')+1);
	        
	        if(key.charAt(0) != '#'){
	        	if(key.equals("NewTempFolders")){
	        		NEW_TEMP_FOLDERS = Boolean.valueOf(value);
	        	}else if(key.equals("RootPath")){
	        		//root_path = value + "/";
	        		root_path = Utility.unixPath(Utility.absolutePath(value)) + "/";
	        		
	        		if(!Utility.exists(root_path)){
	        			System.out.println("Root path doesn't exist!");
	        			System.exit(1);
	        		}
	        		
	        		if(NEW_TEMP_FOLDERS){
	        			//Purge jobs as they are no longer valid now
	        			if(polyglot instanceof PolyglotStewardAMQ) ((PolyglotStewardAMQ)polyglot).purgeJobs();
	        			
	        			//Find last folder and increment
		        		tmpi = 0;
		        		
		        		while(Utility.exists(root_path + "Temp" + Utility.toString(tmpi,3))){
		        			tmpi++;
		        		}
		        		
		        		temp_path = root_path + "Temp" + Utility.toString(tmpi,3) + "/";
		        		tmpi = 0;
		        		
		        		while(Utility.exists(root_path + "Public" + Utility.toString(tmpi,3))){
		        			tmpi++;
		        		}
		        		
		        		public_path = root_path + "Public" + Utility.toString(tmpi,3) + "/";
	        		}else{
		        		temp_path = root_path + "Temp/";
		        		public_path = root_path + "Public/";
		        		
		        		//Set the job counter according to previous executions
	        			if(polyglot instanceof PolyglotStewardAMQ && Utility.exists(public_path)) ((PolyglotStewardAMQ)polyglot).setJobCounter(public_path);
	        		}
	        		
	        		//Create needed folders
	        		if(!Utility.exists(temp_path)) new File(temp_path).mkdir();
	        		if(!Utility.exists(public_path)) new File(public_path).mkdir();
	        	}else if(key.equals("Context")){
	        		context = value;
	        	}else if(key.equals("Port")){
	        		port = Integer.valueOf(value);
	          }else if(key.equals("StewardPort") && polyglot instanceof PolyglotSteward){
	          	steward_port = Integer.valueOf(value);
	          	
	        	  if(steward_port >= 0){
	        	  	((PolyglotSteward)polyglot).listen(steward_port);
	        	  }
	          }else if(key.equals("SoftwareServer") && polyglot instanceof PolyglotSteward){
          		tmpi = value.lastIndexOf(':');
	        		
	        		if(tmpi != -1){
	        			server = value.substring(0, tmpi);
	        			port = Integer.valueOf(value.substring(tmpi+1));
	        			((PolyglotSteward)polyglot).add(server, port);
	        		}
	          }else if(key.equals("ReturnURL")){
	          	RETURN_URL = Boolean.valueOf(value);
	          }else if(key.equals("DownloadSSFile")){
	          	DOWNLOAD_SS_FILE = Boolean.valueOf(value);
	          	//System.out.println("[" + SoftwareServerUtility.getTimeStamp() + "] [restlet]: DOWNLOAD_SS_FILE = " + String.valueOf(DOWNLOAD_SS_FILE));
	          }else if(key.equals("MongoLogging")){
	          	MONGO_LOGGING = Boolean.valueOf(value);
	          }else if(key.equals("MongoUpdateInterval")){
	          	mongo_update_interval = Integer.valueOf(value);
	          }else if(key.equals("SoftwareServerRESTInterface")){
	          	SOFTWARE_SERVER_REST_INTERFACE = Boolean.valueOf(value);
	          }else if(key.equals("SoftwareServerRESTPort")){
							if(polyglot instanceof PolyglotSteward) ((PolyglotSteward)polyglot).setSoftwareServerRESTPort(Integer.valueOf(value));
	          }else if(key.equals("DownloadMethod")){
							download_method = value;
	          }else if(key.equals("HostPostedFile")){
							HOST_POSTED_FILES = Boolean.valueOf(value);
	          }else if(key.equals("MaxTaskTime")){
							if(polyglot instanceof PolyglotSteward) ((PolyglotSteward)polyglot).setMaxTaskTime(Integer.valueOf(value));
	          }else if(key.equals("RedirectToSoftwareServer")){
							REDIRECT_TO_SOFTWARESERVER = Boolean.valueOf(value);
						}else if(key.equals("AuthenticationEndpoint")){
							authentication_url = value;
	          }else if(key.equals("Authentication")){
	  	        username = value.substring(0, value.indexOf(':')).trim();
	  	        password = value.substring(value.indexOf(':')+1).trim();
							System.out.println("Adding user: " + username);
	  	        accounts.put(username, password);
					
							if(!username.toLowerCase().startsWith("admin")){
								nonadmin_user = username;
								if(polyglot instanceof PolyglotStewardAMQ) ((PolyglotStewardAMQ)polyglot).setAuthentication(username, password);
							}
	          }
	        }
	      }
	    }
	    
	    ins.close();
	  }catch(Exception e) {e.printStackTrace();}
	  
	  //Start after configuration is loaded
	  if(polyglot instanceof PolyglotStewardAMQ){
	  	if(PURGE_JOBS) ((PolyglotStewardAMQ)polyglot).purgeJobs();
	  	((PolyglotStewardAMQ)polyglot).start();
	  }
	 	
	 	if(MONGO_LOGGING){
			//Setup database connection
			try{
	    	Properties properties = new Properties();
	    	properties.load(new FileInputStream("mongo.properties"));
	    	mongo = new MongoClient(properties.getProperty("server"));
	    	db = mongo.getDB(properties.getProperty("database"));
	    	//db.authenticate(properties.getProperty("username"), properties.getProperty("password").toCharArray());
	    	//DBCollection collection = db.getCollection(properties.getProperty("collection"));
	    	db.getLastError();		//Test the connection, will cause an exception if not connected.
			}catch(Exception e){
				System.out.println("\nMongo database not found, disabling Mongo logging...");
				MONGO_LOGGING = false;
			}

			if(MONGO_LOGGING){
	 			System.out.println("\nStarting Mongo information update thread...");

	  		new Thread(){
	  			public void run(){		  			
	  				while(true){
	  					PolyglotRESTUtilities.updateMongo(db, polyglot);
	  					Utility.pause(mongo_update_interval);
	  				}
	  			}
	  		}.start();
			}
	 	}
		
	  //Start the service
	  start_time = System.currentTimeMillis();
	  
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
					router.attachDefault(PolyglotRestlet.class);

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

					if(!accounts.isEmpty() || (authentication_url != null)){
						ChallengeAuthenticator guard = new ChallengeAuthenticator(null, ChallengeScheme.HTTP_BASIC, "realm-NCSA");

						SecretVerifier verifier = new SecretVerifier(){
							@Override
							public int verify(String username, char[] password){
								//Try local accounts first
								if(!accounts.isEmpty()){
									if(accounts.containsKey(username) && compare(password, accounts.get(username).toCharArray())){
										return RESULT_VALID;
									}
								}

								//Try authentication URL next
								if(authentication_url != null){
									URLConnection connection = null;

									try{
										URL url = new URL(authentication_url);
										connection = url.openConnection();
										connection.setDoOutput(true);

										//Add basic auth header
										String auth = username + ":" + new String(password);
										connection.setRequestProperty("Authorization", "Basic " + DatatypeConverter.printBase64Binary(auth.getBytes()));
										BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
										String userinfo = br.readLine();
										br.close();

										return RESULT_VALID;
									}catch(Exception e){
										return RESULT_INVALID;
									}
								}

								//In case not in local account and authentication URL wasn't set
								return RESULT_INVALID;
							}
						};

						//Check for admin account, if found with no users make authentication optional	
      	  	boolean FOUND_ADMIN = false;
       			boolean FOUND_USER = false;
        
        		for(String username : accounts.keySet()){
          		if(username.toLowerCase().startsWith("admin")){
            		FOUND_ADMIN = true;
          		}else{
            		FOUND_USER = true;
          		}
						}

        		if(FOUND_ADMIN && !FOUND_USER) guard.setOptional(true);
						guard.setVerifier(verifier);
						guard.setNext(router);

						//Add a CORS filter to allow cross-domain requests
						CorsFilter corsfilter = new CorsFilter(getContext(), guard);
						corsfilter.setAllowedOrigins(new HashSet<String>(Arrays.asList("*")));
						//corsfilter.setAllowingAllRequestedHeaders(true);
						//corsfilter.setAllowedHeaders(new HashSet<String>(Arrays.asList("x-requested-with", "Content-Type")));
           	corsfilter.setAllowedCredentials(true);
						//corsfilter.setAllowedCredentials(false);
						corsfilter.setSkippingResourceForCorsOptions(true);

						return corsfilter;
					}else{
					 	//Add a CORS filter to allow cross-domain requests
  					CorsFilter corsfilter = new CorsFilter(getContext(), router);
  					corsfilter.setAllowedOrigins(new HashSet<String>(Arrays.asList("*")));
  					//corsfilter.setAllowedHeaders(new HashSet<String>(Arrays.asList("x-requested-with", "Content-Type")));
  					corsfilter.setAllowedCredentials(true);
					
						return corsfilter;
					}
				}
			};
		
			component.getDefaultHost().attach("/", application);
			component.start();
		}catch(Exception e) {e.printStackTrace();}
  	
		System.out.println("\nPolyglot restlet is running...\n");
	}
}

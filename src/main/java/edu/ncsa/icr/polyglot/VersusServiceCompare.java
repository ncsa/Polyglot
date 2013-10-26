package edu.ncsa.icr.polyglot;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.restlet.data.Form;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class VersusServiceCompare
{
  private static final String BOUNDRY      = "0123456789012345678901234567890123456789";
  private static Map<File, String> uploads = new HashMap<File, String>();
   
  public static String uploadFile(String server, File file) throws IOException {
  	// return previous upload
  	if (uploads.containsKey(file)) {
  		return uploads.get(file);
  	}
  	
  	 // Make a connect to the server
  	URL url = new URL(server + "files/upload");
  	HttpURLConnection conn = (HttpURLConnection) url.openConnection();
  	
    // make it a post request
    conn.setDoOutput(true);
    conn.setDoInput(true);
    conn.setUseCaches(false);
    conn.setRequestMethod("POST");
    conn.setRequestProperty("Accept", "text/xml, text/plain");

    // mark it as multipart
    conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDRY);

    // create output stream
    DataOutputStream dataOS = new DataOutputStream(conn.getOutputStream());

    // write data
    dataOS.writeBytes("--" + BOUNDRY + "\r\n");
    dataOS.writeBytes("Content-Disposition: form-data; name=\"fileToUpload\"; filename=\"" + file.getName() + "\"\r\n");
    dataOS.writeBytes("\r\n");

    // actual data to be written
    InputStream is = new FileInputStream(file);
    byte[] buf = new byte[10240];
    int len = 0;
    while ((len = is.read(buf)) > 0) {
        dataOS.write(buf, 0, len);
    }
    is.close();

    // write final boundary and done
    dataOS.writeBytes("\r\n--" + BOUNDRY + "--");
    dataOS.flush();
    dataOS.close();

    // Ensure we got the HTTP 200 response code
    int responseCode = conn.getResponseCode();
    if (responseCode != 200) {
        throw new IOException(String.format("Received the response code %d from the URL %s", responseCode, conn.getResponseMessage()));
    }

    // Read the response
    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
    String line = br.readLine();
    br.close();
    if (line.equals("no file uploaded")) {
    	throw(new IOException("Server responded no file uploaded."));
    }
    uploads.put(file, server + "files/" + line);
    return server + "files/" + line;
  }

	public static String compareRaw(String server, String dataset1, String dataset2, String adapter, String extractor, String measure) throws IOException
	{
		// create post message
		StringBuilder post = new StringBuilder();
		post.append("dataset1=").append(URLEncoder.encode(dataset1, "UTF-8"));
		post.append("&");
		post.append("dataset2=").append(URLEncoder.encode(dataset2, "UTF-8"));
		post.append("&");
		post.append("adapter=").append(URLEncoder.encode(adapter, "UTF-8"));
		post.append("&");
		post.append("extractor=").append(URLEncoder.encode(extractor, "UTF-8"));
		post.append("&");
		post.append("measure=").append(URLEncoder.encode(measure, "UTF-8"));
		
 	  // Make a connect to the server
  	URL url = new URL(server + "comparisons");
  	HttpURLConnection conn = (HttpURLConnection) url.openConnection();

    // make it a post request
    conn.setDoOutput(true);
    conn.setDoInput(true);
    conn.setUseCaches(false);
    conn.setRequestMethod("POST");
    conn.setRequestProperty("Accept", "text/xml, text/plain");

    // create output stream
    OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
    
    // write the data
    wr.write(post.toString());

    // done
    wr.flush();
    wr.close();

    // Ensure we got the HTTP 200 response code
    int responseCode = conn.getResponseCode();
    if (responseCode != 200) {
        throw new IOException(String.format("Received the response code %d from the URL %s", responseCode, conn.getResponseMessage()));
    }

    // Read the response
    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
    String line = br.readLine();
    br.close();
    if (line.equals("no file uploaded")) {
    	throw(new IOException("Server responded no file uploaded."));
    }
    return line;

	}
	
	public static String compare(String server, String dataset1, String dataset2, String adapter, String extractor, String measure) throws IOException
	{
		Form form = new Form();
		form.add("dataset1", dataset1);
		form.add("dataset2", dataset2);
		form.add("adapter", adapter);
		form.add("extractor", extractor);
		form.add("measure", measure);

		ClientResource comparisonsResource = new ClientResource(server + "comparisons");
		Representation result = comparisonsResource.post(form.getWebRepresentation());
		return result.getText();
	}

	public static String checkComparisonASync(String comparisonurl) throws IOException {		
 	  // Make a connect to the server
  	URL url = new URL(comparisonurl);
  	HttpURLConnection conn = (HttpURLConnection) url.openConnection();

    // make it a get request
    conn.setDoInput(true);
    conn.setUseCaches(false);
    conn.setRequestMethod("GET");
    conn.setRequestProperty("Accept", "text/xml, text/plain");
    
    // Ensure we got the HTTP 200 response code
    int responseCode = conn.getResponseCode();
    if (responseCode != 200) {
        throw new IOException(String.format("Received the response code %d from the URL %s", responseCode, conn.getResponseMessage()));
    }

    // Read the response
    try {
	    Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(conn.getInputStream());
	    NodeList nl = doc.getElementsByTagName("status");
	    if (nl.getLength() != 1) {
	    	return "N/A";
	    }
	    String status = nl.item(0).getTextContent();
	    if (!"DONE".equals(status)) {
	    	return status;
	    }
	    nl = doc.getElementsByTagName("value");
	    if (nl.getLength() != 1) {
	    	return "UNKNOWN";
	    }
	    return nl.item(0).getTextContent();
    } catch (SAXException e) {
    	throw(new IOException("Could not parse answer.", e));    	
    }catch(ParserConfigurationException e){
    	throw(new IOException("Could not parse answer.", e));    	
		}
	}
}

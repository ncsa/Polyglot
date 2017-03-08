package edu.illinois.ncsa.isda.softwareserver.datawolf;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import javax.xml.bind.DatatypeConverter;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.HttpClientBuilder;

/**
 * @author Chris Navarro
 */
public class DataWolf
{
	private static final String DATAWOLF_AUTHENTICATION = "DataWolfAuthentication";
	private static final String DATAWOLF_SERVER = "DataWolfServer";
	private static DataWolf datawolf;
	private String server;
	private String user;

	private DataWolf()
	{
		loadConfiguration("DataWolf.conf");
	}

	public void loadConfiguration(String filename)
	{
		BufferedReader br = null;
		try{
			br = new BufferedReader(new FileReader(filename));
			String line = null;
			String key, value = null;
			while((line = br.readLine()) != null){
				if(line.charAt(0) != '#'){
					key = line.substring(0, line.indexOf('='));
					value = line.substring(line.indexOf('=') + 1);
					if(key.equals(DATAWOLF_SERVER)){
						server = value;
					}else if(key.equals(DATAWOLF_AUTHENTICATION)){
						final String username = value.substring(0, value.indexOf(':')).trim();
						final String password = value.substring(value.indexOf(':') + 1).trim();

						// Gets a DataWolf user, we could store u/p for later
						HttpClientBuilder builder = HttpClientBuilder.create();
						HttpClient client = builder.build();

						String token = username + ":" + password;
						String auth = DatatypeConverter.printBase64Binary(token.getBytes());

						String loginURL = server + "/login?email=" + username;
						HttpGet httpGet = new HttpGet(loginURL);
						httpGet.setHeader("Content-type", "application/json");
						httpGet.setHeader("Authorization", "Basic " + auth);
						BasicResponseHandler responseHandler = new BasicResponseHandler();
						try{
							user = client.execute(httpGet, responseHandler);
						}catch(ClientProtocolException e){
							System.out.println("Error getting datawolf user.");
							e.printStackTrace();
						}catch(IOException e){
							System.out.println("Error getting datawolf user.");
							e.printStackTrace();
						}
					}else{
						System.out.println("Unknown key/value pair: key = " + key + " value = " + value);
					}
				}
			}
		}catch(FileNotFoundException e){
			System.out.println("Could not find DataWolf configuration file.");
			e.printStackTrace();
		}catch(IOException e){
			System.out.println("Error reading DataWolf configuration file.");
			e.printStackTrace();
		}finally{
			if(br != null){
				try{
					br.close();
				}catch(IOException e){
					System.out.println("Error closing DataWolf file reader.");
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Returns an object containing information about the configured DataWolf
	 * Server
	 * 
	 * @return an object representing the configured DataWolf server
	 */
	public static DataWolf getInstance()
	{
		if(datawolf == null){
			datawolf = new DataWolf();
		}

		return datawolf;
	}

	/**
	 * Returns the DataWolf server endpoint
	 * 
	 * @return DataWolf server endpoint
	 */
	public String getServer()
	{
		return server;
	}

	/**
	 * Returns JSON string representing a DataWolf user
	 * 
	 * @return JSON string representing a DataWolf user
	 */
	public String getUser()
	{
		return user;
	}
	
}

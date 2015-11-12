package edu.illinois.ncsa.isda.softwareserver.datawolf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class WorkflowUtilities
{
	public static void parseLogfile(String file)
	{
		// Information we need to create/execute software server tools
		String softwareServerURL = null;

		// List of tools
		List<String> softwareServerTools = new ArrayList<String>();
		// List of Tasks
		List<String> softwareServerTasks = new ArrayList<String>();
		// List of outputs
		List<String> softwareServerOutputs = new ArrayList<String>();
		// List of inputs
		List<String> softwareServerInputs = new ArrayList<String>();

		BufferedReader br = null;
		try{
			br = new BufferedReader(new FileReader(file));
			String line = null;
			while((line = br.readLine()) != null){
				// Find Server
				if(line.contains("http")){
					String urlPattern = "((https?|http):((//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)";
					Pattern p = Pattern.compile(urlPattern, Pattern.CASE_INSENSITIVE);
					Matcher m = p.matcher(line);
					int i = 0;
					String httpString = null;
					while(m.find()){
						httpString = m.group(i);
						i++;
					}
					if(httpString != null){
						softwareServerURL = httpString.split("file")[0];
					}

				}

				// Find Tool
				if(line.contains("Executing")){
					String[] executionLine = line.split("Executing,")[1].split(" ");
					// Software Server Tool and Task
					String[] toolScript = executionLine[1].split("/")[2].split("_");
					softwareServerTools.add(toolScript[0]);
					softwareServerTasks.add(toolScript[1].split("\\.", 2)[0]);

					String[] toolInput = executionLine[2].split("\\.");
					softwareServerInputs.add(toolInput[toolInput.length - 1]);

					// Software output
					String[] toolOutput = executionLine[3].split("\\.");
					softwareServerOutputs.add(toolOutput[toolOutput.length - 1]);

				}

			}

		}catch(FileNotFoundException e){
			e.printStackTrace();
		}catch(IOException e){
			e.printStackTrace();
		}finally{
			try{
				if(br != null){
					br.close();
				}
			}catch(IOException e){
				e.printStackTrace();
			}
		}
		JsonObject datawolfUser = getUser();
		String filename = new File(file).getName();

		// Create a workflow with the same name as the polyglot log file
		JsonObject workflow = createWorkflow(filename, "polyglot workflow from log file", datawolfUser);
		
		String datawolfURL = DataWolf.getInstance().getServer();
		saveWorkflow(workflow, datawolfURL, false);
		
		JsonArray workflowSteps = new JsonArray();
		JsonObject prevTool = null;
		JsonObject prevStep = null;
		for(int index = 0; index < softwareServerTools.size(); index++){
			String toolName = softwareServerTools.get(index);
			String toolTask = softwareServerTasks.get(index);
			String toolOutput = softwareServerOutputs.get(index);
			String toolInput = softwareServerInputs.get(index);
			
			String toolId = createDataWolfTool(toolName, toolTask, toolOutput, toolInput, softwareServerURL, datawolfUser);

			// Fetch the created tool, otherwise we get duplicate entries
			JsonObject tool = getWorkflowTool(datawolfURL, toolId);
			// Create Workflow step
			JsonObject workflowStep = createWorkflowStep(tool.get("title").getAsString(), datawolfUser, tool);
			if(index > 0 && prevTool != null){
				// Connect input of the current tool to the output of previous
				Object obj = prevTool.get("outputs");
				JsonArray outputArray = (JsonArray)obj;
				// Find correct output
				JsonObject output = null;
				for(int arrayIndex = 0; arrayIndex < outputArray.size(); arrayIndex++){
					output = (JsonObject)outputArray.get(arrayIndex);
					if(!output.get("title").getAsString().equals("stdout")){
						arrayIndex = outputArray.size();
					}
				}
				if(output != null && prevStep != null){
					String dataId = output.get("dataId").getAsString();
					JsonObject outputs = (JsonObject)prevStep.get("outputs");
					String outputMapId = outputs.get(dataId).getAsString();

					// Current step input
					JsonArray inputArray = (JsonArray)tool.get("inputs");
					JsonObject inputToolData = (JsonObject)inputArray.get(0);
					String toolInputDataId = inputToolData.get("dataId").getAsString();
					JsonObject inputs = (JsonObject)workflowStep.get("inputs");

					inputs.addProperty(toolInputDataId, outputMapId);
				}
			}
			workflowSteps.add(workflowStep);
			prevTool = tool;
			prevStep = workflowStep;
		}
		// Add to Workflow
		workflow.add("steps", workflowSteps);
		// Save workflow
		updateWorkflow(workflow, datawolfURL);
	}

	public static JsonObject createWorkflow(String title, String description, JsonObject creator)
	{
		JsonObject workflow = new JsonObject();
		workflow.addProperty("id", UUID.randomUUID().toString());
		workflow.addProperty("title", title);
		workflow.addProperty("description", description);
		workflow.add("creator", creator);

		return workflow;
	}

	/**
	 * @param workflow
	 * @param datawolfURL
	 * @return
	 */
	public static JsonObject updateWorkflow(JsonObject workflow, String datawolfURL)
	{
		return saveWorkflow(workflow, datawolfURL, true);
	}

	public static JsonObject saveWorkflow(JsonObject workflow, String datawolfURL, boolean update)
	{
		HttpClientBuilder builder = HttpClientBuilder.create();
		HttpClient client = builder.build();

		StringEntity stringEntity;
		JsonObject savedWorkflow = null;
		try{
			stringEntity = new StringEntity(workflow.toString());
			BasicResponseHandler responseHandler = new BasicResponseHandler();

			String response = null;
			HttpEntityEnclosingRequestBase request = null;
			if(!update){
				String toolURL = datawolfURL + "/workflows";
				request = new HttpPost(toolURL);
				request.setEntity(stringEntity);
				request.setHeader("Content-type", "application/json");
			}else{
				String toolURL = datawolfURL + "/workflows/" + workflow.get("id").getAsString();
				request = new HttpPut(toolURL);
				request.setEntity(stringEntity);
				request.setHeader("Content-type", "application/json");
			}
			response = client.execute(request, responseHandler);
			JsonParser parser = new JsonParser();
			Object obj = parser.parse(response);
			savedWorkflow = (JsonObject)obj;
		}catch(UnsupportedEncodingException e){
			e.printStackTrace();
		}catch(ClientProtocolException e){
			e.printStackTrace();
		}catch(IOException e){
			e.printStackTrace();
		}

		return savedWorkflow;
	}

	public static JsonObject createWorkflowStep(String title, JsonObject creator, JsonObject tool)
	{
		JsonObject workflowStep = new JsonObject();
		workflowStep.addProperty("id", UUID.randomUUID().toString());
		workflowStep.addProperty("title", title);
		workflowStep.add("creator", creator);
		workflowStep.add("tool", tool);

		JsonObject stepInputs = new JsonObject();
		JsonObject stepOutputs = new JsonObject();
		JsonObject stepParameters = new JsonObject();

		JsonArray toolInputs = (JsonArray)tool.get("inputs");
		for(int index = 0; index < toolInputs.size(); index++){
			JsonObject toolInput = (JsonObject)toolInputs.get(index);
			stepInputs.addProperty(toolInput.get("dataId").getAsString(), UUID.randomUUID().toString());
		}

		JsonArray toolOutputs = (JsonArray)tool.get("outputs");
		for(int index = 0; index < toolOutputs.size(); index++){
			JsonObject toolOutput = (JsonObject)toolOutputs.get(index);
			stepOutputs.addProperty(toolOutput.get("dataId").getAsString(), UUID.randomUUID().toString());
		}

		JsonArray toolParameters = (JsonArray)tool.get("parameters");
		for(int index = 0; index < toolParameters.size(); index++){
			JsonObject toolParameter = (JsonObject)toolParameters.get(index);
			stepParameters.addProperty(toolParameter.get("parameterId").getAsString(), UUID.randomUUID().toString());
		}

		workflowStep.add("inputs", stepInputs);
		workflowStep.add("outputs", stepOutputs);
		workflowStep.add("parameters", stepParameters);

		return workflowStep;
	}

	public static String createDataWolfTool(String toolName, String toolTask, String toolOutput, String toolInput,
			String softwareServerURL, JsonObject datawolfUser)
	{

		// String title = toolName + "-" + toolTask + "-" + toolInput;
		String title = toolName + "-" + toolInput + "-" + toolOutput;
		String description = "New Tool";
		String version = "1";
		String executor = "commandline";

		// Blobs associated with the tool
		JsonArray toolBlobs = new JsonArray();
		JsonArray toolInputs = new JsonArray();
		JsonArray toolOutputs = new JsonArray();
		JsonArray toolParameters = new JsonArray();

		JsonObject commandLineImpl = new JsonObject();
		JsonArray commandLineOptions = new JsonArray();

		// Add Software Server parameter and command line option
		JsonObject toolParameter = createWorkflowToolParameter("Host", "Software Server", false, "STRING", true,
				softwareServerURL);
		toolParameters.add(toolParameter);
		commandLineOptions.add(createCommandlineOption("PARAMETER", "", "", toolParameter.get("parameterId").getAsString(),
				null, null, true));

		// Add Application Parameter
		toolParameter = createWorkflowToolParameter("Host", "Application", false, "STRING", true, toolName);
		toolParameters.add(toolParameter);
		commandLineOptions.add(createCommandlineOption("PARAMETER", "", "", toolParameter.get("parameterId").getAsString(),
				null, null, true));

		// Add Task parameter
		toolParameter = createWorkflowToolParameter("Host", "Task", false, "STRING", true, toolTask);
		toolParameters.add(toolParameter);
		commandLineOptions.add(createCommandlineOption("PARAMETER", "", "", toolParameter.get("parameterId").getAsString(),
				null, null, true));

		// Add output type parameter
		toolParameter = createWorkflowToolParameter("Host", "Output", false, "STRING", true, toolOutput);
		toolParameters.add(toolParameter);
		commandLineOptions.add(createCommandlineOption("PARAMETER", "", "", toolParameter.get("parameterId").getAsString(),
				null, null, true));

		// Add Input file
		// TODO we could create a lookup table of mime types
		JsonObject toolData = createWorkflowToolData(toolInput, toolTask + " file to " + toolOutput, "");
		toolInputs.add(toolData);
		commandLineOptions.add(createCommandlineOption("DATA", "", "", toolData.get("dataId").getAsString(), "INPUT", "input."
				+ toolInput, true));

		// Add Output file
		toolData = createWorkflowToolData(toolOutput, toolOutput + " output file", "");
		toolOutputs.add(toolData);
		commandLineOptions.add(createCommandlineOption("DATA", "", "", toolData.get("dataId").getAsString(), "OUTPUT", "output."
				+ toolOutput, true));

		// Add StdOut
		toolData = createWorkflowToolData("stdout", "stdout log file", "");
		toolOutputs.add(toolData);

		commandLineImpl.add("commandLineOptions", commandLineOptions);
		commandLineImpl.addProperty("executable", "./script.sh");
		commandLineImpl.add("captureStdOut", toolData.get("dataId"));
		commandLineImpl.add("captureStdErr", null);
		commandLineImpl.addProperty("joinStdOutStdErr", false);

		JsonObject toolFileDescriptor = getShellScriptDescriptor();
		toolBlobs.add(toolFileDescriptor);

		// JSON object representing a new workflow tool
		JsonObject tool = new JsonObject();
		tool.addProperty("title", title);
		tool.addProperty("description", description);
		tool.addProperty("version", version);
		tool.addProperty("executor", executor);
		tool.add("creator", datawolfUser);
		tool.add("inputs", toolInputs);
		tool.add("outputs", toolOutputs);
		tool.add("parameters", toolParameters);
		tool.add("blobs", toolBlobs);
		tool.addProperty("implementation", commandLineImpl.toString());

		File toolzip = null;
		FileOutputStream fos = null;
		ZipOutputStream zos = null;
		try{
			toolzip = File.createTempFile("tool", ".zip");
			fos = new FileOutputStream(toolzip);
			zos = new ZipOutputStream(fos);
			ZipEntry entry = new ZipEntry("tool.json");

			zos.putNextEntry(entry);
			zos.write(tool.toString().getBytes());
			zos.closeEntry();

			zos.putNextEntry(new ZipEntry("blobs/"));
			zos.closeEntry();
			zos.putNextEntry(new ZipEntry("blobs/" + toolFileDescriptor.get("id").getAsString() + "/" + "script.sh/script"));
			zos.write(getShellScript().getBytes());
			zos.closeEntry();

		}catch(IOException ioe){
			ioe.printStackTrace();
		}finally{
			try{
				if(zos != null){
					zos.close();
				}
				if(fos != null){
					fos.close();
				}
			}catch(IOException e){
				System.out.println("Error closing tool zip file.");
				e.printStackTrace();
			}
		}

		String datawolfURL = DataWolf.getInstance().getServer();
		return createDataWolfTool(tool, toolzip, datawolfURL);
	}

	public static JsonObject getWorkflowTool(String datawolfURL, String toolId)
	{
		HttpClientBuilder builder = HttpClientBuilder.create();
		HttpClient client = builder.build();

		String toolURL = datawolfURL + "/workflowtools/" + toolId;
		HttpGet httpGet = new HttpGet(toolURL);
		httpGet.setHeader("Content-type", "application/json");
		
		BasicResponseHandler responseHandler = new BasicResponseHandler();
		JsonObject tool = null;
		
		try{
			String response = client.execute(httpGet, responseHandler);
			JsonParser parser = new JsonParser();
			tool = (JsonObject)parser.parse(response);
		}catch(ClientProtocolException e){
			e.printStackTrace();
		}catch(IOException e){
			e.printStackTrace();
		}

		return tool;
	}

	public static JsonObject createWorkflowToolParameter(String title, String description, boolean allowNull, String type,
			boolean hidden, String value)
	{
		JsonObject toolParameter = new JsonObject();
		toolParameter.addProperty("id", UUID.randomUUID().toString());
		toolParameter.addProperty("title", title);
		toolParameter.addProperty("description", description);
		toolParameter.addProperty("allowNull", allowNull);
		toolParameter.addProperty("type", type);
		toolParameter.addProperty("hidden", hidden);
		toolParameter.addProperty("value", value);
		toolParameter.addProperty("parameterId", UUID.randomUUID().toString());
		return toolParameter;
	}

	public static JsonObject createWorkflowToolData(String title, String description, String mimeType)
	{
		JsonObject toolData = new JsonObject();
		toolData.addProperty("id", UUID.randomUUID().toString());
		toolData.addProperty("title", title);
		toolData.addProperty("description", description);
		toolData.addProperty("mimeType", mimeType);
		toolData.addProperty("dataId", UUID.randomUUID().toString());
		return toolData;
	}

	public static JsonObject createCommandlineOption(String type, String value, String flag, String optionId,
			String inputOutput, String filename, boolean commandline)
	{
		JsonObject commandlineOption = new JsonObject();
		commandlineOption.addProperty("type", type);
		commandlineOption.addProperty("value", value);
		commandlineOption.addProperty("flag", flag);
		commandlineOption.addProperty("optionId", optionId);
		if(inputOutput != null){
			commandlineOption.addProperty("inputOutput", inputOutput);
		}
		if(filename != null){
			commandlineOption.addProperty("filename", filename);
		}
		commandlineOption.addProperty("commandline", commandline);
		return commandlineOption;
	}

	public static String createDataWolfTool(JsonObject tool, File toolzip, String datawolfURL)
	{
		HttpClientBuilder builder = HttpClientBuilder.create();
		HttpClient client = builder.build();

		String toolURL = datawolfURL + "/workflowtools";
		HttpPost httpPost = new HttpPost(toolURL);

		FileBody fileBody = new FileBody(toolzip);

		MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
		entityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
		entityBuilder.addPart("tool", fileBody);

		httpPost.setEntity(entityBuilder.build());
		BasicResponseHandler responseHandler = new BasicResponseHandler();
		String response;
		try{
			response = client.execute(httpPost, responseHandler);
			JsonParser parser = new JsonParser();
			JsonArray toolIds = (JsonArray)parser.parse(response);

			if(toolIds.size() >= 1){ return toolIds.get(0).getAsString(); }
		}catch(ClientProtocolException e){
			System.out.println("Error creating datawolf tool.");
			e.printStackTrace();
		}catch(IOException e){
			System.out.println("Error creating datawolf tool.");
			e.printStackTrace();
		}

		return null;

	}

	public static JsonObject getShellScriptDescriptor()
	{
		// ID of the script file so when DataWolf stores the blob, it gets
		// associated back with the tool
		String descriptorId = UUID.randomUUID().toString();

		JsonObject shellScript = new JsonObject();
		shellScript.addProperty("id", descriptorId);
		shellScript.addProperty("filename", "script.sh");
		shellScript.addProperty("mimeType", "application/x-shellscript");
		try{
			shellScript.addProperty("size", getShellScript().getBytes("UTF-8").length);
		}catch(UnsupportedEncodingException e){
			System.out.println("Problem encoding the datawolf shell script.");
			e.printStackTrace();
		}

		return shellScript;

	}

	/**
	 * Returns DataWolf tool shell script for calling SoftwareServers
	 * 
	 * @return DataWolf tool shell script for calling SoftwareServers
	 */
	public static String getShellScript()
	{
		String script = "#!/bin/bash" + "\n\n" + "host=$1 \n" + "application=$2 \n" + "task=$3 \n" + "output=$4 \n"
				+ "input_file=$5 \n" + "dw_output=$6 \n" + "url=$host/software/$application/$task/$output \n\n"
				+ "output_url=`curl -s -H \"Accept:text/plain\" -F \"file=@$input_file\" $url` \n"
				+ "output_file=${input_file%.*}.$output \n" + "echo \"Converting: $input_file to $output_file\" \n" + "sleep 1 \n"
				+ "wget -q -O $output_file $output_url \n" + "mv $output_file $dw_output";

		return script;
	}

	/**
	 * Returns DataWolf user as JSON object
	 * 
	 * @return DataWolf user as JSON object
	 */
	public static JsonObject getUser()
	{
		JsonParser jsonParser = new JsonParser();
		return (JsonObject)jsonParser.parse(DataWolf.getInstance().getUser());
	}
}

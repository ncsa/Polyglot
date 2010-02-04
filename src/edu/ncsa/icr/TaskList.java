package edu.ncsa.icr;
import edu.ncsa.icr.ICRAuxiliary.*;
import edu.ncsa.utility.*;

import java.util.*;

/**
 * A convenient class to create a sequence of tasks.
 * @author Kenton McHenry
 */
public class TaskList
{
	private ICRClient icr = null;
	private Vector<Application> applications = null;
	private Vector<Task> tasks = new Vector<Task>();
	private TreeMap<String,FileData> files = new TreeMap<String,FileData>();
	private TreeMap<String,CachedFileData> cached_files = new TreeMap<String,CachedFileData>();
	
	/**
	 * Class constructor.
	 * @param icr the ICR client we will create tasks for
	 */
	public TaskList(ICRClient icr)
	{
		this.icr = icr;
		applications = icr.getApplications();
	}
	
	/**
	 * Class constructor, create a sequence of tasks that will allow an application to go from the input to the output format.
	 * @param application_string an applications string representation (can be null)
	 * @param input_data the input file
	 * @param output_data the output file
	 */
	public TaskList(ICRClient icr, String application_string, Data input_data, Data output_data)
	{
		this(icr);
		add(application_string, input_data, output_data);
	}
	
	/**
	 * Get the number of tasks in the list.
	 * @return the number of tasks
	 */
	public int size()
	{
		return tasks.size();
	}
	
	/**
	 * Print information about the given tasks.
	 */
	public void print()
	{
		Task task;
		
		for(int i=0; i<tasks.size(); i++){
			task = tasks.get(i);
			
			System.out.println("Application: " + applications.get(task.application).alias);
			System.out.println("Operation: " + applications.get(task.application).operations.get(task.operation).name);
			System.out.println();
		}
	}

	/**
	 * Get the associated ICR client.
	 * @return the associated ICR client
	 */
	public ICRClient getICRClient()
	{
		return icr;
	}
	
	/**
	 * Get a task from the list.
	 * @param index the index of the desired task
	 * @return the task at the given index
	 */
	public Task get(int index)
	{
		return tasks.get(index);
	}

	/**
	 * Get the vector of tasks.
	 * @return the vector of tasks
	 */
	public Vector<Task> getTasks()
	{
		cache();
		return tasks;
	}

	/**
	 * Add a task to the list.
	 * @param task the task to add
	 */
	public void add(Task task)
	{
		tasks.add(task);
	}
	
	/**
	 * Create a new task for the given application and operation names.
	 * @param application_alias the application alias
	 * @param operation_name the operation name
	 * @param input_data the input data for the operation
	 * @param output_data the output data for the operation
	 */
	public void add(String application_alias, String operation_name, Data input_data, Data output_data)
	{
		Application application = null;
		int application_index = -1;
		int operation_index = -1;
		
		//Find the application
		for(int i=0; i<applications.size(); i++){
			if(applications.get(i).alias.equals(application_alias)){
				application_index = i;
				application = applications.get(i);
				break;
			}
		}
		
		//Find the operation
		for(int i=0; i<application.operations.size(); i++){
			if(application.operations.get(i).name.equals(operation_name)){
				operation_index = i;
				break;
			}
		}
		
		if(application_index != -1 && operation_index != -1){
			add(new Task(application_index, operation_index, input_data, output_data));
		}
	}

	/**
	 * Create a new task for the given application, operation, file, and format names.
	 * @param application_alias the application alias
	 * @param operation_name the operation name
	 * @param input_filename the input file name
	 * @param output_filename the output file name
	 */
	public void add(String application_alias, String operation_name, String input_filename, String output_filename)
	{
		Data input_file_data = new Data();
		Data output_file_data = new Data();
				
		if(!input_filename.isEmpty()){
			if(input_filename.contains("/") || input_filename.contains("\\")){	//Local file
				input_file_data = files.get(input_filename);
	
				if(input_file_data == null){
					input_file_data = new FileData(input_filename, true);
					files.put(input_filename, (FileData)input_file_data);
				}
			}else{																															//Cached file
				input_file_data = new CachedFileData(input_filename);
			}
		}
		
		if(!output_filename.isEmpty()){
			output_file_data = new CachedFileData(output_filename);
		}

		add(application_alias, operation_name, input_file_data, output_file_data);
	}
	
	/**
	 * Add a sequence of tasks that will allow an application to go from the input to the output format.
	 * @param application_string an applications string representation (can be null)
	 * @param input_data the input file
	 * @param output_data the output file
	 */
	public void add(String application_string, Data input_data, Data output_data)
	{
		Pair<Integer,Integer> apop, apop0, apop1;
		
		//Attempt a direct conversion operation
		apop = icr.getOperation(application_string, "convert", input_data, output_data);
		
		if(apop != null){
			tasks.add(new Task(apop.first, apop.second, input_data, output_data));
		}else{	//Attempt two part open/import -> save/export
			apop0 = icr.getOperation(application_string, "open", input_data, null);
			if(apop0 == null) apop0 = icr.getOperation(application_string, "import", input_data, null);
			
			if(apop0 != null){
				apop1 = icr.getOperation(applications.get(apop0.first).toString(), "save", null, output_data);
				if(apop1 == null) apop1 = icr.getOperation(applications.get(apop0.first).toString(), "export", null, output_data);
	
				if(apop1 != null){
					tasks.add(new Task(apop0.first, apop0.second, input_data, null));
					tasks.add(new Task(apop1.first, apop1.second, input_data, output_data));
				}
			}
		}
	}

	/**
	 * Merge this task list with another.
	 * @param task_list another task list (must have same ICR client!)
	 */
	public void add(TaskList task_list)
	{
		if(icr == task_list.icr){
			for(int i=0; i<task_list.size(); i++){
				add(task_list.get(i));
			}
		}
	}

	/**
	 * Cache all task input data if not already.
	 */
	public void cache()
	{
		Data data;
		FileData file_data;
		CachedFileData cached_file_data;
		
		for(int i=0; i<tasks.size(); i++){
			data = tasks.get(i).input_data;
			
			if(data instanceof FileData){
				file_data = (FileData)data;
				cached_file_data = cached_files.get(file_data.getAbsoluteName());
				
				if(cached_file_data == null){
					cached_file_data = icr.sendData(file_data);
					cached_files.put(file_data.getAbsoluteName(), cached_file_data);
				}
				
				tasks.get(i).input_data = cached_file_data;
			}
		}
	}
	
	/**
	 * Execute the this task list and return the result of the last task.
	 * @return the data resulting from the last task (will be cached!)
	 */
	public Data execute()
	{
		icr.executeTasks(getTasks());
		return tasks.lastElement().output_data;
	}
	
	/**
	 * Execute the this task list and save the result of the last task to the specified path.
	 * @param output_path the path to save results into
	 */
	public void execute(String output_path)
	{
		Data data = execute();
		FileData file_data;
		CachedFileData cached_file_data;
		
		if(data instanceof CachedFileData){
			cached_file_data = (CachedFileData)data;
			file_data = icr.retrieveData(cached_file_data);
			file_data.save(output_path, null);
		}
	}
}
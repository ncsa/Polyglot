package edu.ncsa.icr.polyglot;
import edu.ncsa.icr.*;
import edu.ncsa.icr.ICRAuxiliary.*;

/**
 * A simple structure to store a single conversion task.
 * @author Kenton McHenry
 */
public class Conversion<V extends Comparable,E>
{
	public V input;
	public V output;
	public E edge;
	
	public Conversion() {}
	
	/**
	 * Class constructor.
	 * @param input the input vertex
	 * @param output the output vertex
	 * @param edge the edge
	 */
	public Conversion(V input, V output, E edge)
	{
		this.input = input;
		this.output = output;
		this.edge = edge;
	}
	
	/**
	 * If of the correct type convert this conversion into a task list.
	 * @param input_name the name of the input file
	 * @return a task list to carry out this conversion over an ICR connection
	 */
	public TaskList getTaskList(String input_name)
	{
		TaskList task_list = null;
		Application application;
		Data input_data, output_data;
		
		if(input instanceof Data && output instanceof Data && edge instanceof Application){
			application = (Application)edge;
			input_data = (Data)input;
			output_data = (Data)output;
			
			task_list = new TaskList(application.icr, application.toString(), );
		}
		
		return task_list;
	}
}
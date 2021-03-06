package edu.illinois.ncsa.isda.softwareserver;
import java.io.*;

/**
 * A process with a time limit.  Allows a process to be executed pseudo-synchronously.
 * A program can wait for such a process to return for some amount of time.  Once the time
 * elapses we return and assume the process has failed.
 * @author Kenton McHenry
 */
public class TimedProcess implements Runnable
{
  private Process process;
	private String output;
  private boolean HANDLE_OUTPUT = false;
  private boolean SHOW_OUTPUT = false;
  private boolean RUNNING = false;

  /**
   * Class constructor.
   * @param p the process to monitor
   */
  public TimedProcess(Process p)
  {
    process = p;
    RUNNING = true;
  }
  
  /**
   * Class constructor.
   * @param p the process to monitor
   * @param HANDLE_OUTPUT true if the process output should be handled
   * @param SHOW_OUTPUT true if the process output should be shown
   */
  public TimedProcess(Process p, boolean HANDLE_OUTPUT, boolean SHOW_OUTPUT)
  {
    process = p;
    this.HANDLE_OUTPUT = HANDLE_OUTPUT;
    this.SHOW_OUTPUT = SHOW_OUTPUT;
    RUNNING = true;
  }

	/**
	 * Get the process output (stdout and stderr).
	 * @return the process output
	 */
	public String getOutput()
	{
		return output;
	}
  
  /**
   * The starting point for the thread that will monitor the process.
   */
  public void run()
  {
    try{
    	if(HANDLE_OUTPUT){
    		output = handleProcessOutput(process, SHOW_OUTPUT);
    	}else{
    		process.waitFor();
    	}
    }catch(Exception e) {e.printStackTrace();}
    
    RUNNING = false;
  }
  
  /**
   * Wait a specified number of milli-seconds for a process to complete.
   * @param n the number of milli-seconds to wait for
   * @return true if the process completed on its own
   */
  public boolean waitFor(int n)
  {
    Thread t = new Thread(this);
    long t0 = 0;
    long t1 = 0;
    
    t0 = System.currentTimeMillis();
    t.start();
    
    while(true){
      t1 = System.currentTimeMillis();
      
      if(!RUNNING){
				System.out.println();
        return true;
      }else if((t1-t0) > n){
      	process.destroy();
				System.out.println();
        return false;
      }

      try{
        Thread.sleep(500);
        System.out.print(".");
      }catch(Exception e) {e.printStackTrace();}
    }
  }

  /**
   * Handle the output of a process.
   * @param process the process
   * @param SHOW_OUTPUT true if the output should be printed
	 * @return the process output
   */
  public static String handleProcessOutput(Process process, boolean SHOW_OUTPUT)
  {
		String output = "";
		BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
		BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));
    String line = null;

		try{
			//Read output
			while((line = stdInput.readLine()) != null){
				output += line + "\n";
    		if(SHOW_OUTPUT) System.out.println(line);
			}

			//Read errors
			while((line = stdError.readLine()) != null){
				output += line + "\n";
				if(SHOW_OUTPUT) System.out.println(line);
			}
		}catch(IOException e){	//Do nothing, the process was killed
			e.printStackTrace();
    }catch(Exception e) {e.printStackTrace();}

		return output;
  }
}

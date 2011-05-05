package kgm.utility;

/**
 * An object container that can be maintained in memory as a watched object is being set asynchronously.
 * @param <T> the object type
 * @author Kenton McHenry
 */
public class AsynchronousObject<T>
{
	private T object = null;
	
	/**
	 * Class constructor.
	 */
	public AsynchronousObject() {}
	
	/**
	 * Determine if the watched object is available.
	 * @return true if the object is available
	 */
	public synchronized boolean isAvailable()
	{
		return object != null;
	}
	
	/**
	 * Wait until the watched object is available.
	 */
	public void waitUntilAvailable()
	{
		while(true){
			synchronized(this){
				if(object != null) break;
			}
			
			Utility.pause(100);
		}
	}
	
	/**
	 * Set the watched object.
	 * @param object the object
	 */
	public synchronized void set(T object)
	{
		this.object = object;
	}
	
	/**
	 * Get the watched object as soon as it becomes available.  
	 * If it is not currently valid wait until it is.
	 * @return the watched object
	 */
	public T get()
	{
		while(true){
			synchronized(this){
				if(object != null) return object;
			}
			
			Utility.pause(100);
		}			
	}
}
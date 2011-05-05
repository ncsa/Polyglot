package kgm.utility;
import java.util.*;

/**
 * An object that allows for conveniently comparing characters/strings at the front of 
 * the queue for the purpose of tokenizing.
 * @author Kenton McHenry
 */
public class StringQueue
{
	private Deque<Character> queue = new LinkedList<Character>();
	
	public StringQueue() {}
	
	/**
	 * Class constructor.
	 * @param string a string to place in the queue
	 */
	public StringQueue(String string)
	{
		addLast(string);
	}
	
	/**
	 * Check if the queue is empty.
	 * @return true if the queue is empty
	 */
	public boolean isEmpty()
	{
		return queue.isEmpty();
	}
	
	/**
	 * Get the current size of the queue.
	 * @return the size of the queue
	 */
	public int size()
	{
		return queue.size();
	}
	
	/**
	 * Clear contents.
	 */
	public void clear()
	{
		queue.clear();
	}
	
	/**
	 * Add a string to the front of the queue.
	 * @param string the string to add to the queue
	 */
	public void addFirst(String string)
	{
		for(int i=string.length()-1; i>=0; i--){
			queue.addFirst(string.charAt(i));
		}
	}
	
	/**
	 * Add a string to the back of the queue.
	 * @param string a string to add
	 */
	public void addLast(String string)
	{		
		for(int i=0; i<string.length(); i++){
			queue.addLast(string.charAt(i));
		}
	}

	/**
	 * Peek at the front character in the queue.
	 * @return the first character in the queue
	 */
	public char peekFirst()
	{
		return queue.peekFirst();
	}
	
	/**
	 * Get and remove the front character in the queue.
	 * @return the first character in the queue
	 */
	public char pollFirst()
	{
		return queue.pollFirst();
	}
	
	/**
	 * Retrieve a string of n characters from the front of the queue.
	 * @param n the number of characters to fetch
	 * @return a string made up of the n characters at the front of the queue
	 */
	public String pollFirst(int n)
	{
		String string = "";
		
		if(n > queue.size()) n = queue.size();
		
		for(int i=0; i<n; i++){
			string += queue.pollFirst();
		}
		
		return string;
	}

	/**
	 * Check if the front of the queue has the given string.
	 * @param string the string to look for at the beginning of the queue
	 * @return true if the string was found
	 */
	public boolean startsWith(String string)
	{
		String front;
		boolean RESULT;
		
		front = pollFirst(string.length());
		RESULT = string.equals(front);
		addFirst(front);
		
		return RESULT;
	}

	/**
	 * Retrieve the next number represented in the queue.
	 * @return the next number represented in the queue (null if next thing isn't a number)
	 */
	public Double nextNumber()
	{
		String string = "";
		char c;
		
		while(!queue.isEmpty()){
			 c = queue.peekFirst();
			 
			 if(Character.isDigit(c) || c == '.'){
				 string += queue.pollFirst();
			 }else{
				 break;
			 }
		}
		
		if(!string.isEmpty()){
			return Double.valueOf(string);
		}else{
			return null;
		}
	}
	
	/**
	 * Retrieve the contents within the next set of parentheses.
	 * @return the contents within the next set of parentheses (null if the next thing isn't an open parentheses)
	 */
	public String nextSet()
	{
		String string = "";
		int count = 0;
		char c;
		
		if(queue.peekFirst() == '('){
			queue.pollFirst();
			count++;
			
			while(!queue.isEmpty()){
				c = queue.pollFirst();
				
				if(c == '('){
					count++;
				}else if(c == ')'){
					count--;
				}
				
				if(count > 0){
					string += c;
				}else{
					return string;
				}
			}
		}
		
		return null;
	}
}
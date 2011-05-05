package kgm.utility;
import java.util.*;

/**
 * A class representing a mathematical function.
 * @author Kenton McHenry
 */
public class Function
{
	private Object[] tokens;
	private final Character[] operators = {'^', '*', '/', '%', '+', '-'};
	private final String[] functions = {"sin", "cos", "tan", "sqrt", "log", "abs"};
	private Vector<String> variables = new Vector<String>();
	private boolean DEBUG = false;
	
	public Function() {}
	
	/**
	 * Class constructor.
	 * @param variables the variables to use in this function
	 * @param expression the expression this function is to evaluate
	 */
	public Function(String[] variables, String expression)
	{
		for(int i=0; i<variables.length; i++){
			add(variables[i]);
		}
		
		set(expression);
	}
	
	/**
	 * Add a variable to this function.
	 * @param variable the variable to add
	 */
	public void add(String variable)
	{
		variables.add(variable);
	}
	
	/**
	 * Tokenize the given algebraic expression.
	 * @param expression the expression to tokenize
	 * @return an array of tokens
	 */
	private Object[] tokenize(String expression)
	{
		Vector<Object> tokens = new Vector<Object>();
		StringQueue queue;
		String sub_expression;
		Object[] sub_tokens;
		Double number;
		boolean FOUND;
		
		//Tokenize the expression
		expression = expression.replaceAll("\\s+", "");
		queue = new StringQueue(expression);
		
		while(!queue.isEmpty()){
			number = queue.nextNumber();
			
			if(number != null){																						//Check for numbers
				tokens.add(number);				
			}else if(isOperator(queue.peekFirst())){											//Check for operators
				tokens.add(queue.pollFirst());
			}else{
				sub_expression = queue.nextSet();
				
				if(sub_expression != null){																	//Check for parentheses
					sub_tokens = tokenize(sub_expression);
					
					if(sub_tokens != null){
						tokens.add(sub_tokens);
					}else{
						return null;
					}
				}else{
					FOUND = false;
					
					for(int i=0; i<functions.length; i++){										//Check for functions
						if(queue.startsWith(functions[i])){
							tokens.add(functions[i]);
							queue.pollFirst(functions[i].length());
							FOUND = true;
							break;
						}
					}
					
					if(!FOUND){
						for(int i=0; i<variables.size(); i++){									//Check for variables
							if(queue.startsWith(variables.get(i))){
								tokens.add(variables.get(i));
								queue.pollFirst(variables.get(i).length());
								FOUND = true;
								break;
							}
						}
					}
					
					if(!FOUND){
						System.out.println("Invalid token at position " + (expression.length()-queue.size()) + ": " + expression);
						return null;
					}
				}
			}
		}
		
		return tokens.toArray();
	}

	/**
	 * Set the expression that this function evaluates.
	 * @param expression the expression
	 */
	public void set(String expression)
	{
		tokens = tokenize(expression);
	}

	/**
	 * Display the given array of tokens.
	 */
	private void printTokens(Object[] tokens)
	{
		for(int i=0; i<tokens.length; i++){
			if(tokens[i] instanceof Object[]){
				System.out.print("(");
				printTokens((Object[])tokens[i]);
				System.out.print(")");
			}else{
				System.out.print(tokens[i]);
			}
		}
	}
	
	/**
	 * Display the current expression from the stored expression tokens.
	 */
	public void print()
	{
		printTokens(tokens);
	}
	
	/**
	 * Determine if the given object is an operator.
	 * @param object an object
	 * @return true if the given object is an operator
	 */
	private boolean isOperator(Object object)
	{
		return Utility.contains(operators, object);
	}

	/**
	 * Determine if the given object is a function.
	 * @param object an object
	 * @return true if the given object is a function
	 */
	private boolean isFunction(Object object)
	{
		for(int i=0; i<functions.length; i++){
			if(functions[i].equals(object)){
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Perform the requested arithmetic operation.
	 * @param operator the operator
	 * @param left the left side operand
	 * @param right the right side operand
	 * @return the result of the operation
	 */
	private Double evaluateOperation(char operator, double left, double right)
	{
		Double result = null;
		
		switch(operator){
		case '^':
			result = Math.pow(left, right);
			break;
		case '*':
			result = left * right;
			break;
		case '/':
			result = left / right;
			break;
		case '%':
			result = left % right;
			break;
		case '+':
			result = left + right;
			break;
		case '-':
			result = left - right;
			break;
		default:
			break;
		}
				
		if(DEBUG) System.out.println("Evaluating: " + left + " " + operator + " " + right + " -> " + result);

		return result;
	}

	/**
	 * Perform the requested function evaluation.
	 * @param function the function
	 * @param parameter the parameter value
	 * @return the result of the function evaluation
	 */
	private Double evaluateFunction(String function, double parameter)
	{
		Double result = null;
		
		if(function.equals("sin")){
			result = Math.sin(parameter);
		}else if(function.equals("cos")){
			result = Math.cos(parameter);
		}else if(function.equals("tan")){
			result = Math.tan(parameter);
		}else if(function.equals("sqrt")){
			result = Math.sqrt(parameter);
		}else if(function.equals("log")){
			result = Math.log(parameter);
		}else if(function.equals("abs")){
			result = Math.abs(parameter);
		}
		
		if(DEBUG) System.out.println("Evaluating: " + function + "(" + parameter + ") -> " + result);
		
		return result;
	}

	/**
	 * Evaluate the given list of tokens with the given variable values
	 * NOTE: Destructive to given tokens and not the most efficient implementation!
	 * @param tokens the expression tokens to evaluate
	 * @param values the variable values to use during evaluation
	 * @return the result of the evaluation
	 */
	private Double evaluateTokens(Vector<Object> tokens, double[] values)
	{		
		//Evaluate all parenthesis
		for(int i=0; i<tokens.size(); i++){			
			if(tokens.get(i) instanceof Object[]){
				tokens.set(i, evaluateTokens(new Vector(Arrays.asList((Object[])tokens.get(i))), values));
			}
		}
		
		//Evaluate all variables
		for(int i=0; i<tokens.size(); i++){			
			for(int j=0; j<variables.size(); j++){
				if(tokens.get(i).equals(variables.get(j))){
					tokens.set(i, values[j]);
					break;
				}
			}
		}
		
		//Evaluate negations
		for(int i=0; i<tokens.size(); i++){		
			if(tokens.get(i).equals('-')){
				if((i==0 || !(tokens.get(i-1) instanceof Double)) && tokens.get(i+1) instanceof Double){
					tokens.set(i, -(Double)tokens.get(i+1));
					tokens.remove(i+1);
				}
			}
		}
		
		//Evaluate functions
		for(int i=0; i<tokens.size(); i++){			
			if(isFunction(tokens.get(i))){
				tokens.set(i, evaluateFunction((String)tokens.get(i), (Double)tokens.get(i+1)));
				tokens.remove(i+1);
			}
		}
		
		//Evaluate negations again (after functions evaluated!)
		for(int i=0; i<tokens.size(); i++){		
			if(tokens.get(i).equals('-')){
				if((i==0 || !(tokens.get(i-1) instanceof Double)) && tokens.get(i+1) instanceof Double){
					tokens.set(i, -(Double)tokens.get(i+1));
					tokens.remove(i+1);
				}
			}
		}
		
		//Evaluate operations
		for(int i=0; i<tokens.size(); i++){		//Exponents
			if(tokens.get(i).equals('^')){
				tokens.set(i-1, evaluateOperation((Character)tokens.get(i), (Double)tokens.get(i-1), (Double)tokens.get(i+1)));
				tokens.remove(i);
				tokens.remove(i);
			}
		}
		
		for(int i=0; i<tokens.size(); i++){		//Multiplication, division, modulus
			if(tokens.get(i).equals('*') || tokens.get(i).equals('/') || tokens.get(i).equals('%')){
				tokens.set(i-1, evaluateOperation((Character)tokens.get(i), (Double)tokens.get(i-1), (Double)tokens.get(i+1)));
				tokens.remove(i);
				tokens.remove(i);
			}
		}
		
		for(int i=0; i<tokens.size(); i++){		//Addition, subtraction, negation
			if(tokens.get(i).equals('+') || tokens.get(i).equals('-')){
				if(tokens.get(i-1) instanceof Double){
					tokens.set(i-1, evaluateOperation((Character)tokens.get(i), (Double)tokens.get(i-1), (Double)tokens.get(i+1)));
					tokens.remove(i);
					tokens.remove(i);
				}else{
					tokens.set(i, -(Double)tokens.get(i+1));
					tokens.remove(i+1);
				}
			}
		}
		
		if(tokens.size() == 1 && tokens.get(0) instanceof Double){
			return (Double)tokens.get(0);
		}else{
			return null;
		}
	}
	
	/**
	 * Evaluate the function at the given values
	 * @param values an array of variable values
	 * @return the result at the given values
	 */
	public double get(double[] values)
	{
		return evaluateTokens(new Vector(Arrays.asList(tokens)), values);
	}
	
	/**
	 * A simple main for debug purposes
	 * @param args command line arguments
	 */
	public static void main(String args[])
	{
		Function f = new Function(new String[]{"x"}, "-(-sin(x)^2+10.1)/-2");
		double x = 2;
		
		System.out.print("f(x) = "); f.print();
		System.out.println();
		System.out.println("f(" + x + "): " + f.get(new double[]{x}));
	}
}
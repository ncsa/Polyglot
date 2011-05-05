package kgm.matrix;
import java.util.*;
import kgm.utility.*;
import Jama.*;

/**
 * Utility functions used to manipulate matrices (utilizes JAMA library).
 *  @author Kenton McHenry
 */
public class JAMAMatrixUtility extends MatrixUtility
{
  public static double[][] pca(Vector<Vector<Double>> X)
  {
    int d = X.get(0).size();
    
    //Calculate center of mass
    double[] mean = new double[d];
    
    for(int i=0; i<X.size(); i++){
      for(int j=0; j<d; j++){
        mean[j] += X.get(i).get(j);    
      }
    }
    
    for(int i=0; i<d; i++){
      mean[i] /= (double)X.size();
    }
    
    //Center the points
    for(int i=0; i<X.size(); i++){
      for(int j=0; j<d; j++){
        X.get(i).set(j, X.get(i).get(j)-mean[j]);
      }
    }
    
    //Build 2nd moment matrix
    Matrix M2 = new Matrix(to2D(X));
    Matrix M1 = M2.transpose();
    Matrix XX = M1.times(M2);
    
    //System.out.println("\n\nX:"); XX.print(4, 2);
    
    //Compute Eigen vectors
    //EigenvalueDecomposition op = new EigenvalueDecomposition(XX);
    SingularValueDecomposition op = new SingularValueDecomposition(XX);
    Matrix V = op.getV();
    
    //System.out.println("\nV:"); V.print(4, 2);
    
    return V.getArray();
  }
  
  /**
   * Invert the given matrix.
   *  @param A the matrix to invert
   *  @return the inverted matrix
   */
  public static double[][] inverse(double[][] A)
  {
    return (new Matrix(A)).inverse().getArray();
  }
  
  /**
   * Compute the pseudo-inverse of the given matrix.
   *  @param A a matrix
   *  @return the pseudo-inverse of the given matrix
   */
  public static double[][] pseudoInverse(double[][] A)
  {
  	double[][] At = transpose(A);
  	double[][] AtA = mtimes(At, A);
  	double[][] AtAi = inverse(AtA);
  	
  	return mtimes(AtAi, At);
  }
  
  /**
   * Solve a linear least squares problem.
   *  @param A the A matrix
   *  @param b the b matrix
   *  @return the solution x
   */
  public static double[] ldivide(double[][] A, double[] b)
  {
  	double[][] B = transpose(new double[][]{b});
    double[][] X = mtimes(pseudoInverse(A), B);
    
    return to1D(X);
  }
  
  /**
   * Solve a homogenous linear least squares problem where b = [0, 0, ..., 0].
   *  @param A the A matrix
   *  @return the solution x
   */
  public static double[] ldivide(double[][] A)
  {
  	SingularValueDecomposition svd = new SingularValueDecomposition(new Matrix(A));
  	double[][] S = svd.getS().getArray();
  	int m = A.length;
  	int n = A[0].length; 
    double minw = Double.MAX_VALUE;
    int mini = 0;
    
    for(int i=0; i<((m<n)?m:n); i++){
    	if(S[i][i] <= minw){
    		minw = S[i][i];
    		mini = i;
    	}
    }
    
    return svd.getV().transpose().getArray()[mini];
  }
  
  /**
   * Singular Value Decomposition
   * @param A the matrix to decompose
   * @return the U, S, and V components of the matrix
   */
  public static Triple<double[][],double[][],double[][]> svd(double[][] A)
  {
  	double[][] U, S, V;
  	SingularValueDecomposition svd = new SingularValueDecomposition(new Matrix(A));
  	
  	U = svd.getU().getArray();	
  	S = svd.getS().getArray();	
  	V = svd.getV().getArray();	
  	
  	return new Triple<double[][],double[][],double[][]>(U, S, V);
  }
  
  /**
   * Test the SVD method.
   *  @param A a matrix
   */
  public static void printSVD(double[][] A)
  {
  	SingularValueDecomposition svd = new SingularValueDecomposition(new Matrix(A));
  	
    println(svd.getU().getArray());
    println(svd.getS().getArray());
    println(svd.getV().getArray());
  }

	/**
   * Debug tests for the various methods in this class.
   */
  public static void main(String args[])
  {
  	double[][] A = new double[][]{{ 1,  2,  3,  4},
  		                            { 5,  6,  7,  8},
  		                            { 9, 10, 11, 12},
  		                            {13, 14, 15, 16}};
  	
  	double[][] B = new double[][]{{ 1,  2,  3,  4},
        													{ 5,  6,  7,  8}};
  	
  	double[][] C = new double[][]{{ 1,  0,  0,  0},
													        { 0,  6,  0,  8},
													        { 9,  0, 11, 12},
													        {13, 14, 15, 16}};
  	
  	double[] D = new double[]{1, 2, 3, 4};  	
  	
  	double[][] E = new double[][]{{ 1,  0,  0,  0},
													        { 0,  6,  0,  8},
													        { 9,  0, 11, 12},
													        {13, 14, 15, 16},
													        {25,  9,  0,  3},
													        { 2,  8, 10,  0}};
  	
  	//println(A);
  	//println(transpose(A));
  	//println(multiply(A, A));
  	//println(multiply(A, transpose(B)));
  	//println(inverse(C));
  	//println(pseudoInverse(C));
  	//printSVD(C);
  	println(ldivide(C, D));
  	println(ldivide(C));
  	//println(pseudoInverse(E));
  }
}
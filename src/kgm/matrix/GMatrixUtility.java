package kgm.matrix;
import javax.vecmath.*;
import java.util.*;

/**
 * Utility functions used to manipulate matrices (utilizes Java3D's GMatrix class).
 *  @author Kenton McHenry
 */
public class GMatrixUtility extends MatrixUtility
{
  /**
   * Convert a 2D array into a matrix
   *  @param mat the 2D array
   *  @return the matrix version of the 2D array
   */
  public static GMatrix toMatrix(double[][] mat)
  {
  	int m = mat.length;
  	int n = mat[0].length;
  	
  	return new GMatrix(m, n, to1D(mat));
  }	
  
  /**
   * Convert a vector of vectors into a matrix.
   *  @param v the vector of vectors
   *  @return the resulting matrix
   */
  public static GMatrix toMatrix(Vector<Vector<Double>> v)
  {
    GMatrix M = new GMatrix(v.size(), v.get(0).size());
    
    for(int j=0; j<v.size(); j++){
      for(int i=0; i<v.get(j).size(); i++){
        M.setElement(j, i, v.get(j).get(i));
      }
    }
    
    return M;
  }
  
  /**
   * Convert a matrix into a 2D array.
   *  @param M the matrix
   *  @return the resulting 2D array
   */
  public static double[][] to2D(GMatrix M)
  {
    double[][] mat = new double[M.getNumRow()][M.getNumCol()];
    
    for(int j=0; j<M.getNumRow(); j++){
      for(int i=0; i<M.getNumCol(); i++){
        mat[j][i] = M.getElement(j,i);
      }
    }
    
    return mat;
  }
  
  /**
   * Return the diagonal of the given matrix in a 1D array.
   *  @param M the matrix
   *  @return a 1D array containg the diaganol of the matrix
   */
  public static double[] to1D(GMatrix M)
  {
    double[] arr = new double[M.getNumRow()];
    
    for(int i=0; i<M.getNumRow(); i++){
      if(i < M.getNumCol()){
        arr[i] = M.getElement(i,i);
      }
    }
    
    return arr;
  }
  
  /**
   * Apply a transformation to another transformation.
   *  @param A the transformation to transform
   *  @param B the transformation
   *  @return the resulting transformation
   */
  public static double[][] transform(double[][] A, double[][] B)
  {
    GMatrix GMa = new GMatrix(4, 4, to1D(A));
    GMatrix GMb = new GMatrix(4, 4, to1D(B));
    GMatrix GMc = new GMatrix(4, 4);
    GMc.mul(GMa, GMb);
    
    double[][] C = new double[4][4];
    
    for(int i=0; i<4; i++){
      for(int j=0; j<4; j++){
        C[j][i] = GMc.getElement(j, i);
      }
    }
    
    return C;
  }
  
  /**
   * Determine if the given matrix is a diaganal matrix or not.
   *  @param M the matrix to check
   *  @return true if the matrix was diagonal
   */
  public static boolean diag(GMatrix M)
  {
    if(M.getNumRow() != M.getNumCol()) return false;
    
    for(int j=0; j<M.getNumRow(); j++){
      for(int i=0; i<M.getNumCol(); i++){
        if(i==j){
          if(i > 0){
            if(Math.abs(M.getElement(j,i)-M.getElement(j-1,i-1)) > 0.0001) return false;
          }
        }else{
          if(Math.abs(M.getElement(j,i)) > 0.0001) return false;
        }
      }
    }
    
    return true;  
  }
  
  /**
   * Obtain the principal components of the given cloud of points.
   *  @param X a vector of points
   *  @return a matrix containing the principal components
   */
  public static double[][] pca(Vector<Vector<Double>> X)
  {
    int d = X.get(0).size();
    double tmpd;
    
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
    GMatrix M1 = toMatrix(X);
    GMatrix M2 = toMatrix(X);
    M1.transpose();
    GMatrix XX = new GMatrix(d,d);
    XX.mul(M1, M2);
    
    for(int j=0; j<d; j++){    
      for(int i=0; i<d; i++){
        tmpd = Math.sqrt(XX.getElement(j,i)/(double)(X.size()-1));
        if(Double.isNaN(tmpd)) tmpd = 0;
        XX.setElement(j,i,tmpd);
      }
    }
    
    System.out.println("\n\nXX:\n" + XX.toString());
    
    //Compute Eigen vectors
    GMatrix W;
    GMatrix V;
    
    if(!diag(XX)){
      GMatrix U = new GMatrix(d,d);
      W = new GMatrix(d,d);
      V = new GMatrix(d,d);
      int rank = XX.SVD(U, W, V);
      System.out.println("Rank: " + rank + "\n");
      
      //Computer Eigen values
      for(int i=0; i<d; i++){
        tmpd = W.getElement(i,i);
        tmpd = tmpd*tmpd;
        //if(Double.isNaN(tmpd)) tmpd = 0;
        //if(Double.isInfinite(tmpd)) tmpd = 0;
        W.setElement(i,i,tmpd);
      }
    
      //Checks for 3D points only!
      if(d == 3){
        //Check for rank deficient matrices
        if(W.getElement(0,0)*W.getElement(1,1)*W.getElement(2,2) == 0){
          V = new GMatrix(d, d, to1D(eye(d)));
        }
    
        //Make sure results is a proper rotation matrix
        if(V.getElement(0,1) == V.getElement(1,0)){
          tmpd = V.getElement(0,0);
          V.setElement(0,0,V.getElement(0,1));
          V.setElement(0,1,tmpd);
          
          tmpd = V.getElement(1,0);
          V.setElement(1,0,V.getElement(1,1));
          V.setElement(1,1,tmpd);
          
          tmpd = W.getElement(0,0);
          W.setElement(0,0,W.getElement(1,1));
          W.setElement(1,1,tmpd);
        }
      }
    }else{
      W = new GMatrix(d,d,to1D(eye(d)));
      V = new GMatrix(d,d,to1D(eye(d)));
    }
    
    System.out.println("V:\n" + V.toString());
    
    return to2D(V);
  }
  
  /**
   * Invert the given matrix.
   *  @param A the matrix to invert
   *  @return the inverted matrix
   */
  public static double[][] inverse(double[][] A)
  {
  	int m = A.length;
  	int n = A[0].length;
    double[][] Ai = new double[m][n];    
    GMatrix M = new GMatrix(m, n, MatrixUtility.to1D(A));
    
    M.invert();
    
    for(int j=0; j<m; j++){
    	for(int i=0; i<n; i++){
        Ai[j][i] = M.getElement(j, i);
      }
    }
    
    return Ai;
  }
  
  /**
   * Apply a translation to the right side of the given transformation matrix.
   *  @param A the transformation matrix
   *  @param tx the translation along the x axis
   *  @param ty the translation along the y axis
   *  @param tz the translation along the z axis
   *  @return the new transformation matrix
   */
  public static double[][] translate(double[][] A, double tx, double ty, double tz)
  {
    double[][] T = eye(4);
    
    T[0][3] = tx;
    T[1][3] = ty;
    T[2][3] = tz;
    
    return transform(A, T);
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
   * Test the GMatrix class's SVD method.
   * Notes: GMatrix SVD seems to give different results that matlab...
   *  @param A a matrix
   */
  public static void printSVD(double[][] A)
  {
  	int m = A.length;
  	int n = A[0].length;
  	GMatrix M = toMatrix(A);
    GMatrix U = new GMatrix(m,m);
    GMatrix W = new GMatrix(m,n);
    GMatrix V = new GMatrix(n,n);
    int rank = M.SVD(U, W, V);
    
    println(to2D(U));
    println(to2D(W));
    println(to2D(V));
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
  	int m = A.length;
  	int n = A[0].length;
  	GMatrix M = toMatrix(A);
    GMatrix U = new GMatrix(m,m);
    GMatrix W = new GMatrix(m,n);
    GMatrix V = new GMatrix(n,n);
    int rank = M.SVD(U, W, V);
    
    double minw = -Double.MAX_VALUE;
    int mini = 0;
    double[] x = new double[n];
    
    for(int i=0; i<((m<n)?m:n); i++){
    	if(W.getElement(i, i) <= minw){
    		minw = W.getElement(i, i);
    		mini = i;
    	}
    }
    
    V.getColumn(mini, x);
    
    return x;
  }
  
  /**
   * Debug tests for the various methods in this class.
   */
  public static void debug()
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
  	
  	//println(A);
  	//println(transpose(A));
  	//println(multiply(A, A));
  	//println(multiply(A, transpose(B)));
  	//println(inverse(A));
  	//println(pseudoInverse(C));
  	printSVD(C);
  	//println(ldivide(C, D));
  	println(ldivide(C));
  }
}
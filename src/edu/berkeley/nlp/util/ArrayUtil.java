package edu.berkeley.nlp.util;

import java.util.Arrays;

import fig.basic.ListUtils;
import fig.basic.ListUtils.Generator;

public class ArrayUtil {
	

	public static int[] clone(int[] original) {
		// TODO Sort out which of these we should use.
		// return Arrays.copyOf(original, original.length);
		// return original.clone(); 
		int[] copy = new int[original.length];
		System.arraycopy(original, 0, copy, 0, original.length);
		return copy;
	}
	
	public static boolean[][] clone(boolean[][] a) {
	  	boolean[][] res = new boolean[a.length][];
	    for (int i=0; i<a.length; i++){
	    	if (a[i]!=null) res[i] = a[i].clone();
	    }
	    return res;
	  }

	  public static boolean[][][] clone(boolean[][][] a) {
	  	boolean[][][] res = new boolean[a.length][][];
	    for (int i=0; i<a.length; i++){
	    	if (a[i]!=null) res[i] = clone(a[i]);
	    }
	    return res;
	  }
		
	  public static boolean[][][][] clone(boolean[][][][] a) {
	  	boolean[][][][] res = new boolean[a.length][][][];
	    for (int i=0; i<a.length; i++){
	    	res[i] = clone(a[i]);
	    }
	    return res;
	  }
	  
	  public static int[][] clone(int[][] a) {
	  	int[][] res = new int[a.length][];
	    for (int i=0; i<a.length; i++){
	    	res[i] = a[i].clone();
	    }
	    return res;
	  }

	  public static double[][] clone(double[][] a) {
	  	double[][] res = new double[a.length][];
	    for (int i=0; i<a.length; i++){
	    	if (a[i]!=null) res[i] = a[i].clone();
	    }
	    return res;
	  }

	  public static double[][][] clone(double[][][] a) {
	  	double[][][] res = new double[a.length][][];
	    for (int i=0; i<a.length; i++){
	    	if (a[i]!=null) res[i] = clone(a[i]);
	    }
	    return res;
	  }
		
	  public static double[][][][] clone(double[][][][] a) {
	  	double[][][][] res = new double[a.length][][][];
	    for (int i=0; i<a.length; i++){
	    	res[i] = clone(a[i]);
	    }
	    return res;
	  }

	public static void fill(float[][] a, float val) {
		for (int i = 0; i < a.length; i++) {
			Arrays.fill(a[i], val);
		}
	}

	public static void fill(float[][][] a, float val) {
		for (int i = 0; i < a.length; i++) {
			fill(a[i], val);
		}
	}

	public static void fill(float[][][][] a, float val) {
		for (int i = 0; i < a.length; i++) {
			fill(a[i], val);
		}
	}
	
	public static void fill(float[][][][][] a, float val) {
		for (int i = 0; i < a.length; i++) {
			fill(a[i], val);
		}
	}

	public static void fill(double[][] a, double val) {
		for (int i = 0; i < a.length; i++) {
			Arrays.fill(a[i], val);
		}
	}
	
	public static void fill(double[][] a, int until1, int until2, double val)
	{
		for (int i = 0; i < until1; ++i)
		{
			Arrays.fill(a[i],0,until2 == Integer.MAX_VALUE ? a[i].length : until2,val);
		}
	}

	public static void fill(double[][][] a, double val) {
		for (int i = 0; i < a.length; i++) {
			fill(a[i], val);
		}
	}
	
	public static void fill(double[][][] a, int until, double val) {
	    for (int i=0; i<until; i++){
	    	fill(a[i],val);
	    }
	  }
	
	public static void fill(double[][][] a, int until1, int until2, double val) {
	    for (int i=0; i<until1; i++){
	    	fill(a[i],until2,Integer.MAX_VALUE,val);
	    }
	  }
	
	public static void fill(double[][][] a, int until1, int until2, int until3, double val) {
	    for (int i=0; i<until1; i++){
	    	fill(a[i],until2,until3,val);
	    }
	  }

	public static void fill(double[][][][] a, double val) {
		for (int i = 0; i < a.length; i++) {
			fill(a[i], val);
		}
	}
	
	public static void fill(double[][][][] a, int until1, int until2, int until3, int until4, double val) {
	    for (int i=0; i<until1; i++){
	    	fill(a[i],until2,until3,until4,val);
	    }
	  }
	
	public static void fill(double[][][][][] a, double val) {
		for (int i = 0; i < a.length; i++) {
			fill(a[i], val);
		}
	}
	
	public static void fill(boolean[][] a, boolean val) {
		for (int i = 0; i < a.length; i++) {
			Arrays.fill(a[i], val);
		}
	}
	
	public static void fill(boolean[][] a, int until1, int until2, boolean val)
	{
		for (int i = 0; i < until1; ++i)
		{
			Arrays.fill(a[i],0,until2 == Integer.MAX_VALUE ? a[i].length : until2,val);
		}
	}

	public static void fill(boolean[][][] a, boolean val) {
		for (int i = 0; i < a.length; i++) {
			fill(a[i], val);
		}
	}
	
	public static void fill(boolean[][][] a, int until, boolean val) {
	    for (int i=0; i<until; i++){
	    	fill(a[i],val);
	    }
	  }
	
	public static void fill(boolean[][][] a, int until1, int until2, boolean val) {
	    for (int i=0; i<until1; i++){
	    	fill(a[i],until2,Integer.MAX_VALUE,val);
	    }
	  }
	
	public static void fill(boolean[][][] a, int until1, int until2, int until3, boolean val) {
	    for (int i=0; i<until1; i++){
	    	fill(a[i],until2,until3,val);
	    }
	  }

	public static void fill(boolean[][][][] a, boolean val) {
		for (int i = 0; i < a.length; i++) {
			fill(a[i], val);
		}
	}
	
	public static void fill(boolean[][][][] a, int until1, int until2, int until3, int until4, boolean val) {
	    for (int i=0; i<until1; i++){
	    	fill(a[i],until2,until3,until4,val);
	    }
	  }
	
	public static void fill(int[][] a, int until1, int until2, int val)
	{
		for (int i = 0; i < until1; ++i)
		{
			Arrays.fill(a[i],0,until2,val);
		}
	}
	
	public static void fill(int[][][] a, int until1, int until2, int until3, int val) {
	    for (int i=0; i<until1; i++){
	    	fill(a[i],until2,until3,val);
	    }
	  }
	
	public static void fill(int[][][][] a, int until1, int until2, int until3, int until4,  int val) {
	    for (int i=0; i<until1; i++){
	    	fill(a[i],until2,until3,until4,val);
	    }
	  }
	
	public static void fill(Object[][] a, int until1, int until2, Object val)
	{
		for (int i = 0; i < until1; ++i)
		{
			Arrays.fill(a[i],0,until2 == Integer.MAX_VALUE ? a[i].length : until2,val);
		}
	}
	
	public static void fill(Object[][][] a, int until1, int until2, int until3, Object val)
	{
		for (int i = 0; i < until1; ++i)
		{
			fill(a[i],until2 == Integer.MAX_VALUE ? a[i].length : until2,until3, val);
		}
	}
	
	public static void fill(Object[][][][] a, int until1, int until2, int until3, int until4, Object val)
	{
		for (int i = 0; i < until1; ++i)
		{
			fill(a[i],until2 == Integer.MAX_VALUE ? a[i].length : until2,until3, until4, val);
		}
	}
	
	public static <T> void fill(T[] array, Generator<T> gen)
	{
		for (int i = 0; i < array.length; ++i)
		{
			array[i] = gen.generate(i);
		}
	}
	
	public static <T> void fill(T[][] array, Generator<T> gen)
	{
		for (int i = 0; i < array.length; ++i)
		{
			ArrayUtil.fill(array[i],gen);
		}
	}
	
	public static <T> void fill(T[][][] array, Generator<T> gen)
	{
		for (int i = 0; i < array.length; ++i)
		{
			ArrayUtil.fill(array[i],gen);
		}
	}

	public static String toString(float[][] a) {
		String s = "[";
		for (int i = 0; i < a.length; i++) {
			s = s.concat(Arrays.toString(a[i]) + ", ");
		}
		return s + "]";
	}

	public static String toString(float[][][] a) {
		String s = "[";
		for (int i = 0; i < a.length; i++) {
			s = s.concat(toString(a[i]) + ", ");
		}
		return s + "]";
	}

	public static String toString(double[][] a) {
		String s = "[";
		for (int i = 0; i < a.length; i++) {
			s = s.concat(Arrays.toString(a[i]) + ", ");
		}
		return s + "]";
	}

	public static String toString(double[][][] a) {
		String s = "[";
		for (int i = 0; i < a.length; i++) {
			s = s.concat(toString(a[i]) + ", ");
		}
		return s + "]";
	}

	public static String toString(boolean[][] a) {
		String s = "[";
		for (int i = 0; i < a.length; i++) {
			s = s.concat(Arrays.toString(a[i]) + ", ");
		}
		return s + "]";
	}

	public static double[] copy(double[] mat) {
		int m = mat.length;
		double[] newMat = new double[m];
		System.arraycopy(mat, 0, newMat, 0, mat.length);
		return newMat;
	}

	public static double[][] copy(double[][] mat) {
		int m = mat.length, n = mat[0].length;
		double[][] newMat = new double[m][n];
		for (int r = 0; r < m; r++)
			System.arraycopy(mat[r], 0, newMat[r], 0, mat[r].length);
		return newMat;
	}

	public static double[][][] copy(double[][][] mat) {
		int m = mat.length, n = mat[0].length, p = mat[0][0].length;
		double[][][] newMat = new double[m][n][p];
		for (int r = 0; r < m; r++)
			for (int c = 0; c < n; c++)
				System.arraycopy(mat[r][c], 0, newMat[r][c], 0, mat[r][c].length);
		return newMat;
	}

	public static double[][][][] copy(double[][][][] mat) {
		int m = mat.length, n = mat[0].length, p = mat[0][0].length, q = mat[0][0][0].length;
		double[][][][] newMat = new double[m][n][p][q];
		for (int r = 0; r < m; r++)
			for (int c = 0; c < n; c++)
				for (int i = 0; i < p; i++)
					System.arraycopy(mat[r][c][i], 0, newMat[r][c][i], 0, mat[r][c][i].length);
		return newMat;
	}

	public static double[][] subMatrix(double[][] ds, int i, int ni, int j, int nj)
	{
		double[][] retVal = new double[ni][nj];
		for (int k = i; k < ni; ++k)
		{
			for (int l = j; l < nj; ++l)
			{
				retVal[k-i][j-l] = ds[i][j];
			}
		}
		return retVal;
	}
	
	/**
	 * If array is less than the minimum length (or null), a new array is allocated with length minLength;
	 * Otherwise, the argument is returned
	 * @param array
	 * @param minLength
	 * @return
	 */
	public static double[] reallocArray(double[] array, int minLength)
	{
		if (array == null || array.length < minLength)
		{
			return new double[minLength];
		}
		return array;
		
	}
	
	public static double[] reallocArray(double[] array, int minLength, double fillVal)
	{
		double[] newArray = reallocArray(array,minLength);
		Arrays.fill(newArray,fillVal);
		return newArray;
		
	}
	
	public static double[][] reallocArray(double[][] array, int minLength1, int minLength2)
	{
		if (array == null || array.length < minLength1)
		{
			return new double[minLength1][minLength2];
		}
		return array;
		
	}
	
	public static double[][] reallocArray(double[][] array, int minLength1, int minLength2, double fillVal)
	{
		double[][] newArray = reallocArray(array,minLength1,minLength2);
		ArrayUtil.fill(newArray,minLength1,minLength2, fillVal);
		return newArray;
		
	}
	
	public static double[][][] reallocArray(double[][][] array, int minLength1, int minLength2, int minLength3)
	{
		if (array == null || array.length < minLength1)
		{
			return new double[minLength1][minLength2][minLength3];
		}
		return array;
		
	}
	
	public static double[][][] reallocArray(double[][][] array, int minLength1, int minLength2, int minLength3, double fillVal)
	{
		double[][][] newArray = reallocArray(array,minLength1,minLength2,minLength3);
		ArrayUtil.fill(newArray,minLength1,minLength2,minLength3, fillVal);
		return newArray;
		
	}
	
	public static double[][][][] reallocArray(double[][][][] array, int minLength1, int minLength2, int minLength3, int minLength4)
	{
		if (array == null || array.length < minLength1 || array[0].length < minLength2 || array[0][0].length < minLength3 || array[0][0][0].length < minLength4)
		{
			return new double[minLength1][minLength2][minLength3][minLength4];
		}
		return array;
		
	}
	
	public static double[][][][] reallocArray(double[][][][] array, int minLength1, int minLength2, int minLength3, int minLength4, double fillVal)
	{
		double[][][][] newArray = reallocArray(array,minLength1,minLength2,minLength3,minLength4);
		ArrayUtil.fill(newArray,minLength1,minLength2,minLength3, minLength4,fillVal);
		return newArray;
		
	}
	
	public static boolean[] reallocArray(boolean[] array, int minLength)
	{
		if (array == null || array.length < minLength)
		{
			return new boolean[minLength];
		}
		return array;
		
	}
	
	public static boolean[] reallocArray(boolean[] array, int minLength, boolean fillVal)
	{
		boolean[] newArray = reallocArray(array,minLength);
		Arrays.fill(newArray,fillVal);
		return newArray;
		
	}
	
	public static boolean[][] reallocArray(boolean[][] array, int minLength1, int minLength2)
	{
		if (array == null || array.length < minLength1)
		{
			return new boolean[minLength1][minLength2];
		}
		return array;
		
	}
	
	public static boolean[][] reallocArray(boolean[][] array, int minLength1, int minLength2, boolean fillVal)
	{
		boolean[][] newArray = reallocArray(array,minLength1,minLength2);
		ArrayUtil.fill(newArray,minLength1,minLength2, fillVal);
		return newArray;
		
	}
	
	public static boolean[][][] reallocArray(boolean[][][] array, int minLength1, int minLength2, int minLength3)
	{
		if (array == null || array.length < minLength1)
		{
			return new boolean[minLength1][minLength2][minLength3];
		}
		return array;
		
	}
	
	public static boolean[][][] reallocArray(boolean[][][] array, int minLength1, int minLength2, int minLength3, boolean fillVal)
	{
		boolean[][][] newArray = reallocArray(array,minLength1,minLength2,minLength3);
		ArrayUtil.fill(newArray,minLength1,minLength2,minLength3, fillVal);
		return newArray;
		
	}
	
	public static boolean[][][][] reallocArray(boolean[][][][] array, int minLength1, int minLength2, int minLength3, int minLength4)
	{
		if (array == null || array.length < minLength1 || array[0].length < minLength2 || array[0][0].length < minLength3 || array[0][0][0].length < minLength4)
		{
			return new boolean[minLength1][minLength2][minLength3][minLength4];
		}
		return array;
		
	}
	
	public static boolean[][][][] reallocArray(boolean[][][][] array, int minLength1, int minLength2, int minLength3, int minLength4, boolean fillVal)
	{
		boolean[][][][] newArray = reallocArray(array,minLength1,minLength2,minLength3,minLength4);
		ArrayUtil.fill(newArray,minLength1,minLength2,minLength3, minLength4,fillVal);
		return newArray;
		
	}
	
	public static int[] reallocArray(int[] array, int minLength)
	{
		if (array == null || array.length < minLength)
		{
			return new int[minLength];
		}
		return array;
		
	}
	
	public static int[] reallocArray(int[] array, int minLength, int fillVal)
	{
		int[] newArray = reallocArray(array,minLength);
		Arrays.fill(newArray,fillVal);
		return newArray;
		
	}
	
	public static int[][] reallocArray(int[][] array, int minLength1, int minLength2)
	{
		if (array == null || array.length < minLength1)
		{
			return new int[minLength1][minLength2];
		}
		return array;
		
	}
	
	public static int[][] reallocArray(int[][] array, int minLength1, int minLength2, int fillVal)
	{
		int[][] newArray = reallocArray(array,minLength1,minLength2);
		ArrayUtil.fill(newArray,minLength1,minLength2, fillVal);
		return newArray;
		
	}
	
	public static int[][][] reallocArray(int[][][] array, int minLength1, int minLength2, int minLength3)
	{
		if (array == null || array.length < minLength1)
		{
			return new int[minLength1][minLength2][minLength3];
		}
		return array;
		
	}
	
	public static int[][][] reallocArray(int[][][] array, int minLength1, int minLength2, int minLength3, int fillVal)
	{
		int[][][] newArray = reallocArray(array,minLength1,minLength2,minLength3);
		ArrayUtil.fill(newArray,minLength1,minLength2,minLength3, fillVal);
		return newArray;
		
	}
	
	public static int[][][][] reallocArray(int[][][][] array, int minLength1, int minLength2, int minLength3, int minLength4)
	{
		if (array == null || array.length < minLength1 || array[0].length < minLength2 || array[0][0].length < minLength3 || array[0][0][0].length < minLength4)
		{
			return new int[minLength1][minLength2][minLength3][minLength4];
		}
		return array;
		
	}
	
	public static int[][][][] reallocArray(int[][][][] array, int minLength1, int minLength2, int minLength3, int minLength4, int fillVal)
	{
		int[][][][] newArray = reallocArray(array,minLength1,minLength2,minLength3,minLength4);
		ArrayUtil.fill(newArray,minLength1,minLength2,minLength3, minLength4,fillVal);
		return newArray;
		
	}
	
	

	private static void printMatrix(double[][] a) {
		final int len = 5;
		for (int i = 0; i < len; ++i) {
			System.out.print("[");
			for (int j = 0; j < len; ++j) {
				System.out.print(a[i][j] + "\t,");
			}
			System.out.println("]");
		}
	}
	
	
	
	public static <T> T[] reallocArray(T[] array, int minLength, Class<T> klass, Generator<T> gen)
	{
		if (array == null || array.length < minLength)
		{
			array = ListUtils.newArray(minLength, klass, gen);
		}
		
			fill(array,gen);
			
		return array;
		
		
	}
	
	public static <T> T[][] reallocArray(T[][] array, int minLength1, final int minLength2,final Class<T> klass, final Generator<T> gen)
	{
		
		
		if (array == null || array.length < minLength1 || array[0].length < minLength2)
		{
			array = ListUtils.newArray(minLength1, ListUtils.newArray(minLength2, klass,gen));
		}
		
			fill(array,new Generator<T[]>()
				{

					public T[] generate(int i)
					{
						return  ListUtils.newArray(minLength2, klass,gen);
					}
				
				});
			
		return array;
		
		
	}
	
	
	
	

}

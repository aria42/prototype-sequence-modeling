package edu.berkeley.nlp.math;

import java.util.Arrays;

/**
 */
public class DoubleArrays {
	public static double[] clone(double[] x) {
		double[] y = new double[x.length];
		assign(y, x);
		return y;
	}

	public static void assign(double[] y, double[] x) {
		if (x.length != y.length)
			throw new RuntimeException("diff lengths: " + x.length + " "
					+ y.length);
		System.arraycopy(x, 0, y, 0, x.length);
	}

	public static double innerProduct(double[] x, double[] y) {
		if (x.length != y.length)
			throw new RuntimeException("diff lengths: " + x.length + " "
					+ y.length);
		double result = 0.0;
		for (int i = 0; i < x.length; i++) {
			result += x[i] * y[i];
		}
		return result;
	}

	public static double[] addMultiples(double[] x, double xMultiplier,
			double[] y, double yMuliplier) {
		if (x.length != y.length)
			throw new RuntimeException("diff lengths: " + x.length + " "
					+ y.length);
		double[] z = new double[x.length];
		for (int i = 0; i < z.length; i++) {
			z[i] = x[i] * xMultiplier + y[i] * yMuliplier;
		}
		return z;
	}

	public static double[] constantArray(double c, int length) {
		double[] x = new double[length];
		Arrays.fill(x, c);
		return x;
	}

	public static double[] pointwiseMultiply(double[] x, double[] y) {
		if (x.length != y.length)
			throw new RuntimeException("diff lengths: " + x.length + " "
					+ y.length);
		double[] z = new double[x.length];
		for (int i = 0; i < z.length; i++) {
			z[i] = x[i] * y[i];
		}
		return z;
	}

	public static String toString(double[] x) {
		return toString(x, x.length);
	}

	public static String toString(double[] x, int length) {
		StringBuffer sb = new StringBuffer();
		sb.append("[");
		for (int i = 0; i < SloppyMath.min(x.length, length); i++) {
			sb.append(x[i]);
			if (i + 1 < SloppyMath.min(x.length, length))
				sb.append(", ");
		}
		sb.append("]");
		return sb.toString();
	}

	public static void scale(double[] x, double s) {
		if (s == 1.0)
			return;
		for (int i = 0; i < x.length; i++) {
			x[i] *= s;
		}
	}

	public static double[] multiply(double[] x, double s) {
		double[] result = new double[x.length];
		if (s == 1.0) {
			System.arraycopy(x, 0, result, 0, x.length);
			return result;
		}
		for (int i = 0; i < x.length; i++) {
			result[i] = x[i] * s;
		}
		return result;
	}

	public static int argMax(double[] v) {
		int maxI = -1;
		double maxV = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < v.length; i++) {
			if (v[i] > maxV) {
				maxV = v[i];
				maxI = i;
			}
		}
		return maxI;
	}

	public static double max(double[] v) {
		double maxV = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < v.length; i++) {
			if (v[i] > maxV) {
				maxV = v[i];
			}
		}
		return maxV;
	}

	public static int argMin(double[] v) {
		int minI = -1;
		double minV = Double.POSITIVE_INFINITY;
		for (int i = 0; i < v.length; i++) {
			if (v[i] < minV) {
				minV = v[i];
				minI = i;
			}
		}
		return minI;
	}

	public static double min(double[] v) {
		double minV = Double.POSITIVE_INFINITY;
		for (int i = 0; i < v.length; i++) {
			if (v[i] < minV) {
				minV = v[i];
			}
		}
		return minV;
	}

	public static double maxAbs(double[] v) {
		double maxV = 0;
		for (int i = 0; i < v.length; i++) {
			double abs = (v[i] <= 0.0d) ? 0.0d - v[i] : v[i];
			if (abs > maxV) {
				maxV = abs;
			}
		}
		return maxV;
	}

	public static double[] add(double[] a, double b) {
		double[] result = new double[a.length];
		for (int i = 0; i < a.length; i++) {
			double v = a[i];
			result[i] = v + b;
		}
		return result;
	}

	public static double add(double[] a) {
		double sum = 0.0;
		for (int i = 0; i < a.length; i++) {
			sum += a[i];
		}
		return sum;
	}

	public static double add(double[] a, int first, int last) {
		if (last >= a.length)
			throw new RuntimeException("last beyond end of array");
		if (first < 0)
			throw new RuntimeException("first must be at least 0");
		double sum = 0.0;
		for (int i = first; i <= last; i++) {
			sum += a[i];
		}
		return sum;
	}

	public static double vectorLength(double[] x) {
		return Math.sqrt(innerProduct(x, x));
	}

	public static double[] add(double[] x, double[] y) {
		if (x.length != y.length)
			throw new RuntimeException("diff lengths: " + x.length + " "
					+ y.length);
		double[] result = new double[x.length];
		for (int i = 0; i < x.length; i++) {
			result[i] = x[i] + y[i];
		}
		return result;
	}

	public static double[] subtract(double[] x, double[] y) {
		if (x.length != y.length)
			throw new RuntimeException("diff lengths: " + x.length + " "
					+ y.length);
		double[] result = new double[x.length];
		for (int i = 0; i < x.length; i++) {
			result[i] = x[i] - y[i];
		}
		return result;
	}

	public static double[] exponentiate(double[] pUnexponentiated) {
		double[] exponentiated = new double[pUnexponentiated.length];
		for (int index = 0; index < pUnexponentiated.length; index++) {
			exponentiated[index] = SloppyMath.exp(pUnexponentiated[index]);
		}
		return exponentiated;
	}

	public static void truncate(double[] x, double maxVal) {
		for (int index = 0; index < x.length; index++) {
			if (x[index] > maxVal) 
				x[index]=maxVal;
			else if (x[index]<-maxVal)
				x[index]=-maxVal;
		}
	}

	public static void initialize(double[] x, double d) {
		Arrays.fill(x, d);
	}

	public static void initialize(Object[] x, double d) {
		for (int i = 0; i < x.length; i++) {
			Object o = x[i];
			if (o instanceof double[])
				initialize((double[]) o, d);
			else
				initialize((Object[]) o, d);
		}
	}
	
	public static void addInPlace(double[] x, double c) {
		for (int i = 0; i < x.length; i++) {
			x[i] += c;
		}
	}

	public static void addInPlace(double[] x, double[] y) {
		assert y.length >= x.length;
		for (int i=0; i < x.length; ++i) {
			x[i] += y[i];
		}		
	}

	public static void addInPlace2D(double[][] x, double[][] y) {
		// TODO Auto-generated method stub
		assert y.length >= x.length; 
		for (int i=0; i < x.length; ++i) {
			DoubleArrays.addInPlace(x[i],y[i]);
		}
	}

	public static double[] average(double[][] x) {
		if (x.length == 0) {
			return null;
		}
		double[] sum = x[0];
		for (int i=1; i<x.length; i++) {
			sum = add(sum, x[i]);
		}
		double[] avg = multiply(sum, (1.0 / x.length));
		return avg;
	}

}

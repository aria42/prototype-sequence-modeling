package edu.berkeley.nlp.math;

import fig.basic.LogInfo;

/**
 */
public class FigBacktrackingLineSearcher implements GradientLineSearcher {
  private double EPS = 1e-10;
  double stepSizeMultiplier = 0.9;
  private double sufficientDecreaseConstant = 1e-4;
  boolean verbose = false;

  public double[] minimize(DifferentiableFunction function, double[] initial, double[] direction) {
	if (verbose) LogInfo.track("Backtracking Line Search with stepSizeMultiplier=%.5f",stepSizeMultiplier);
    double stepSize = 1.0;
    double initialFunctionValue = function.valueAt(initial);
    double initialDirectionalDerivative = DoubleArrays.innerProduct(function.derivativeAt(initial), direction);
    double[] guess = null;
    double guessValue = 0.0;
    boolean sufficientDecreaseObtained = false;
    if (true) {
      if (verbose) LogInfo.track("Nudge Test",true);
      guess = DoubleArrays.addMultiples(initial, 1.0, direction, EPS);
      guessValue = function.valueAt(guess);
      double sufficientDecreaseValue = initialFunctionValue + sufficientDecreaseConstant * initialDirectionalDerivative * EPS;
      if (guessValue > initialFunctionValue) {
    	  if (verbose) {
    		  LogInfo.logs("FAILED!!");
    		  LogInfo.logs("  Trying step size:  %.5f",EPS);
    		  LogInfo.logs("  Required value is: %.5f",sufficientDecreaseValue);
    		  LogInfo.logs("  Value is:    %.5f",guessValue);
    		  LogInfo.logs("  Initial was: %.5f",initialFunctionValue);
    		  LogInfo.logs("  Gradient Norm: %.5f",DoubleArrays.vectorLength(direction));
    		  LogInfo.end_track();
    		  LogInfo.end_track();
    	  }
        System.exit(0);        
        return initial;
      } 
      if (verbose) LogInfo.end_track();
    }
    while (! sufficientDecreaseObtained) {
      if (verbose ) LogInfo.track("Backtracking Line Search: Step Size: %.5f",stepSize);
      guess = DoubleArrays.addMultiples(initial, 1.0, direction, stepSize);
      guessValue = function.valueAt(guess);
      double sufficientDecreaseValue = initialFunctionValue + sufficientDecreaseConstant * initialDirectionalDerivative * stepSize;
      if (verbose) {
    	  LogInfo.logs("Trying step size:  "+stepSize);
    	  LogInfo.logs("Required value is: "+sufficientDecreaseValue);
    	  LogInfo.logs("Value is:          "+guessValue);
    	  LogInfo.logs("Initial was:       "+initialFunctionValue);
      }
      sufficientDecreaseObtained = (guessValue <= sufficientDecreaseValue);
      if (! sufficientDecreaseObtained) {
        stepSize *= stepSizeMultiplier;        
        if (stepSize < EPS) {
          //throw new RuntimeException("BacktrackingSearcher.minimize: stepSize underflow.");
        	if (verbose) {
        		LogInfo.logs("BacktrackingSearcher.minimize: stepSize underflow.");
        		LogInfo.end_track();
        		LogInfo.end_track();
        	}
          return initial;
        }
      } else {
    	 if (verbose) LogInfo.logs("Sufficient Decrease found");    	  
      }
      if (verbose) LogInfo.end_track();
    }
    if (verbose) LogInfo.end_track();
    return guess;
  }
   
 }

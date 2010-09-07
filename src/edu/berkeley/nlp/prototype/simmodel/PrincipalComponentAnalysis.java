package edu.berkeley.nlp.prototype.simmodel;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import edu.berkeley.nlp.util.Counters;
import fig.basic.NumUtils;


public class PrincipalComponentAnalysis implements Serializable {
  /**
   * 
   */
  private static final long serialVersionUID = -5166092187742168842L;
  private final double[][] projection;
  private final int projectedDimension;
  
  public PrincipalComponentAnalysis(SmallSparseVector[] sparseX, int kPrincipalComponents) {
     projectedDimension = kPrincipalComponents;
     SVDMatlabScript svd = new SVDMatlabScript();
     this.projection = svd.getProjection(sparseX);   
  }
 
  private class SVDMatlabScript {  
    private double[][] getProjection(SmallSparseVector[] sparseX)  {
      try {
        File matrixFile = writeSparseMatrix(sparseX);
        File scriptFile = File.createTempFile("context_svd", ".m");
        scriptFile.deleteOnExit();
        File outfile = File.createTempFile("projection", ".dat"); 
        outfile.deleteOnExit();
        PrintStream printStream = new PrintStream(scriptFile);
        printStream.printf("M=spconvert(load('%s'));\n",matrixFile);              
        printStream.printf("[U,S,V] = svds(M,%d);\n",projectedDimension);     
        printStream.printf("save '%s' U -ascii\n",outfile.getAbsolutePath());
        printStream.flush();
        printStream.close();
        String command = "matlab -nojvm < " + scriptFile.getAbsolutePath() ;
        Process p = Runtime.getRuntime().exec(command);
        p.waitFor();
        return readMatrix(outfile);
      } catch (Exception e) {
        e.printStackTrace();        
      }
      return null;
    }
    
    private double[][] readMatrix(File matrixFile) {
      try {
        BufferedReader br = new BufferedReader(new FileReader(matrixFile));
        List<String> lines = new ArrayList<String>();
        while (true) {
          String line = br.readLine();
          if (line == null) break;
          if (line.length() > 0) lines.add(line.trim());
        }
        double[][] matrix = new double[lines.size()][];        
        for (int i=0; i < lines.size(); ++i) {         
          String[] pieces = lines.get(i).split("\\s+");
          matrix[i] = new double[pieces.length];         
          for (int j=0; j < pieces.length; ++j) {
            matrix[i][j] = Double.parseDouble(pieces[j]);         
          }
        }
        for (double[] row: matrix) {
          NumUtils.l2NormalizedMut(row);
        }
        return matrix;
      } catch (Exception e) {
        e.printStackTrace();
        System.exit(0);
      }
      return null;
    }
    
    private File writeSparseMatrix(SmallSparseVector[] sparseX)  {
      try {
        File matrixFile = File.createTempFile("context_vector", ".dat");
        matrixFile.deleteOnExit();       
        BufferedWriter bw = new BufferedWriter(new FileWriter(matrixFile));
        for (int row=0; row < sparseX.length; ++row) {
          SmallSparseVector sv =  sparseX[row];
          // Skip the Zero Vector
          if (sv.l2Norm() == 0.0) {
        	  if (sv.size() == 0.0) {
        		  continue;
        	  }
        	  assert false;
        	  throw new RuntimeException();
          }
          for (int i=0; i < sv.size(); ++i) {
        	int dim = sv.getActiveDimension(i);
            double count = sv.getCount(dim);
            String string = (row+1) + " " + (dim+1) + " " + count + "\n";
			bw.write(string);
          }
        }               
        bw.flush();
        bw.close();
        return matrixFile;
      }  catch (Exception e) {
         e.printStackTrace();       
      }
      return null;
    }
  }

  public double[][] getProjectionMatrix() {
    return projection;
  }
  
}

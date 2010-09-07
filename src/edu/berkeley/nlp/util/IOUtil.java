package edu.berkeley.nlp.util;

import java.io.File;
import java.io.IOException;

public class IOUtil
{
	
	 public static File createTempDirectory(String prefix)
     throws IOException
     {
         File tempFile = File.createTempFile(prefix, "");
         if (!tempFile.delete())
             throw new IOException();
         if (!tempFile.mkdir())
             throw new IOException();
         return tempFile;        
     }


}

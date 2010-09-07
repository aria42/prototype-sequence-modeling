package edu.berkeley.nlp.prototype;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;

import edu.berkeley.nlp.prototype.PrototypeSequenceModel.Tagger;
import fig.basic.IOUtils;
import fig.basic.ListUtils;
import fig.basic.LogInfo;
import fig.basic.Option;
import fig.exec.Execution;

public class PrototypeSequenceModelTester implements Runnable {
	@Option(gloss="Path to Sequence Model",required=true)
	public String modelPath ;
	@Option(gloss="Directory to read data",required=true)
	public String inDirRoot ;
	@Option(gloss="Delimeter to use between words and tags")
	public String delimeter = "##";
	@Option(gloss="Extensions of files to read")
	public String inExtension = ".txt";
	@Option(gloss="Prefix for data files [Defaults to none]")
	public String inPrefix = "";
	@Option(gloss="Directory to write tagged data",required=true)
	public String outDir ;
	@Option(gloss="Extension to append to output file")
	public String outExtension = ".tagged";
	
	public void setOutExtension(String outExtension) {
		if (!outExtension.startsWith(".")) {
			outExtension = "." + outExtension;
		}
		this.outExtension = outExtension;
	}

	public void run() {
		PrototypeSequenceModel seqModel = loadModel();
		File[] files = getFiles();
		Tagger tagger = seqModel.getTagger();
		for (File f: files) {			
			List<String> lines = IOUtils.readLinesHard(f.getAbsolutePath());
			File outF = new File(outDir, f.getName() + outExtension);
			PrintWriter out = IOUtils.openOutHard(outF);
			for (String line: lines) {
				List<String> sent = Arrays.asList(line.split("\\s+"));
				List<String> tags = tagger.getLabelsPosterior(sent);
				List<String> pieces = new ArrayList<String>();
				for (int i=0; i < sent.size(); ++i) {
					String word = sent.get(i);
					String tag = tags.get(i);
					String piece = String.format("%s%s%s",word,delimeter,tag);
					pieces.add(piece);
				}
				out.println(edu.berkeley.nlp.util.StringUtils.join(pieces));
			}		
			out.close();
		}
	}

	private File[] getFiles() {
		File dir = new File(inDirRoot);
		if (!dir.isDirectory()) {
			LogInfo.error("%s not a directory",inDirRoot);
		}
		File[] files = dir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {				
				return name.endsWith(inExtension) && name.startsWith(inPrefix);
			}			
		});
		return files;
	}

	private PrototypeSequenceModel loadModel() {
		PrototypeSequenceModel seqModel  = null;
		try {
			seqModel = (PrototypeSequenceModel) IOUtils.readObjFile(modelPath);
		} catch (Exception e) {
			LogInfo.error("Error Reading Sequence Model from %s", modelPath);
			e.printStackTrace();
			System.exit(2);
		}
		return seqModel;
	}
	/**
	 * Each output file have one sentence per-line, each
	 * line will be tab seperated formatted as follows
	 * word1##tag1  
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		Execution.run(args, new PrototypeSequenceModelTester());
	}
}

package edu.berkeley.nlp.prototype.pos;

import java.io.PrintWriter;
import java.util.Collection;


import edu.berkeley.nlp.syntax.Tree;
import edu.berkeley.nlp.syntax.Trees;
import edu.berkeley.nlp.treebank.TreebankFetcher;
import edu.berkeley.nlp.util.StringUtils;
import fig.basic.IOUtils;
import fig.basic.LogInfo;
import fig.basic.Option;
import fig.exec.Execution;

public class TreebankTextExtractor implements Runnable {
	@Option(required=true)
	public String treebankPath = null;
	@Option
	public int startSection = 0;
	@Option
	public int endSection = 25;
	@Option
	public int maxNumSentences = Integer.MAX_VALUE;
	@Option(required=true)
	public String outfile = null;
	
	public void run() {
		TreebankFetcher fetcher = new TreebankFetcher();
		fetcher.addTransformer(new Trees.StandardTreeNormalizer());
		fetcher.setMaxLength(maxNumSentences);
		Iterable<Tree<String>> trees = fetcher.getTrees(treebankPath, startSection, endSection);
		PrintWriter pw = IOUtils.openOutHard(outfile);
		int count = 0;
		for (Tree<String> t: trees) {
			if (++count%10000 == 0) {
				LogInfo.logs("processed %d sentences",count);
			}
			pw.write(StringUtils.join(t.getYield())+"\n");			
		}
		pw.flush();
		pw.close();
	}
	
	public static void main(String[] args) {
		Execution.run(args, new TreebankTextExtractor());
	}
}

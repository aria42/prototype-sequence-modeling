package edu.berkeley.nlp.prototype.pos;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.berkeley.nlp.syntax.Tree;
import edu.berkeley.nlp.syntax.Trees;
import edu.berkeley.nlp.treebank.TreebankFetcher;
import edu.berkeley.nlp.util.CollectionUtils;
import edu.berkeley.nlp.util.Counter;
import edu.berkeley.nlp.util.CounterMap;
import fig.basic.IOUtils;
import fig.basic.Option;
import fig.basic.Pair;
import fig.exec.Execution;

public class TreebankPOSPrototypeExtractor implements Runnable {
	
	@Option(required=true,gloss="Path to treebank")
	public String treebankPath;
	@Option(gloss="Number of prototypes to extract per-tag")
	public int numProtosPerTag = 4;
	@Option(gloss="Where to write prototype file")
	public String outfile = null;
	@Option(gloss="Start section")
	public int startSection = 2;
	@Option(gloss="Stop section")
	public int stopSection = 23;
	@Option(gloss="Number of sentences")
	public int numSentences = 2000;
	@Option(gloss="To be a prototype how often do you need to have that tag")
	public double thresh = 0.75;
		
	private CounterMap<String, String> getWordTagCounter(Collection<TaggedSentence> sents) {
		CounterMap<String, String> wordTagCounter = new CounterMap<String, String>();
		for (TaggedSentence taggedSent: sents) {
			List<String> words = taggedSent.getWords();
			List<String> tags = taggedSent.getTags();			
			for (Pair<String,String> wordTagPair: CollectionUtils.getPairIterable(words, tags)) {
				wordTagCounter.incrementCount(wordTagPair.getFirst(), wordTagPair.getSecond(), 1.0);
			}
		}
		return wordTagCounter;
	}
	
	public Map<String, Set<String>> extractPrototype(Collection<TaggedSentence> sents) {
		CounterMap<String, String> wordTagCounter = getWordTagCounter(sents);
		CounterMap<String, String> tagWordCounter = wordTagCounter.invert();
		wordTagCounter.normalize();
		Map<String, Set<String>> protoMap = new HashMap<String, Set<String>>();
		for (String tag: tagWordCounter.keySet()) {
			Counter<String> wordCounter = tagWordCounter.getCounter(tag);
			List<String> topWords = wordCounter.getSortedKeys();
			Set<String> protoWords = new HashSet<String>();
			for (String word: topWords) {
				if (protoWords.size() >= numProtosPerTag) {
					break;
				}
				double tagProb = wordTagCounter.getCount(word,tag);
				if (tagProb >= thresh) {
					protoWords.add(word);
				}								
			}
			protoMap.put(tag,protoWords);			
		}
		return protoMap;
	}
	
	public Map<String, Set<String>> extractPrototype(Iterable<Tree<String>> trees) {
		return extractPrototype(getTaggedSentences(trees));
	}
	
	private Collection<TaggedSentence> getTaggedSentences() {
		TreebankFetcher fetcher = new TreebankFetcher();
		fetcher.addTransformer(new Trees.StandardTreeNormalizer());
		fetcher.setMaxTrees(numSentences);
		Iterable<Tree<String>> trees = fetcher.getTrees(treebankPath, startSection, stopSection);
		Collection<TaggedSentence> taggedSents = getTaggedSentences(trees);
		return taggedSents;		
	}

	private Collection<TaggedSentence> getTaggedSentences(
			Iterable<Tree<String>> trees) {
		Collection<TaggedSentence> taggedSents = new ArrayList<TaggedSentence>();
		for (Tree<String> t: trees) {
			List<String> words = t.getYield();
			List<String> tags = t.getPreTerminalYield();
			taggedSents.add(new TaggedSentence(words,tags));
		}
		return taggedSents;
	}
	
	public void run() {
		// TODO Auto-generated method stub
		Collection<TaggedSentence> taggedSents = getTaggedSentences();
		Map<String,Set<String>> protoMap = extractPrototype(taggedSents);
		if (outfile != null) {
			writePrototypeFile(protoMap);
		}
	}
	
	private void writePrototypeFile(Map<String,Set<String>> protoMap) {
		try {
			PrintWriter writer = IOUtils.openOut(outfile);
			for (Map.Entry<String, Set<String>> entry : protoMap.entrySet()) {
				String tag = entry.getKey();
				Set<String> protos = entry.getValue();
				writer.write(tag + "\t");
				Iterator<String> it = protos.iterator();
				while (it.hasNext()) {
					String word = it.next();
					writer.write(word);
					if (it.hasNext()) {
						writer.write("\t");
					}
				}
				writer.write("\n"); writer.flush();
			}
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		Execution.run(args, new TreebankPOSPrototypeExtractor());
	}
	
}

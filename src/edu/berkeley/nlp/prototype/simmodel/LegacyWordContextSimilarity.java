package edu.berkeley.nlp.prototype.simmodel;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import edu.berkeley.nlp.math.DoubleArrays;
import edu.berkeley.nlp.prototype.PrototypeSimilarityModel;
import edu.berkeley.nlp.util.ConcatenationIterable;
import edu.berkeley.nlp.util.Counter;
import edu.berkeley.nlp.util.IterableAdapter;
import edu.berkeley.nlp.util.experiments.SentenceIterable;
import edu.berkeley.nlp.util.IterableAdapter.Convertor;
import fig.basic.IOUtils;
import fig.basic.Indexer;
import fig.basic.LogInfo;
import fig.basic.Option;
import fig.exec.Execution;

public class LegacyWordContextSimilarity extends PrototypeSimilarityModel implements Serializable, Runnable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private transient Indexer<String> contextWordIndexer = new Indexer<String>();

	@Option(gloss="Should we care about distance for context feature")
	public transient boolean appendDistance = false;
	@Option(gloss="Should we care about direction?")
	public transient boolean directional = false;
	@Option(gloss="How large is context window?")
	public transient int contextWindow = 2;
	@Option(gloss="File with words to use for context")
	public String contextWordFile ;
	@Option
	public String dataDirectory = null;
	@Option
	public String primaryPrefix = null;
	@Option
	public String auxPrefix = null;
	@Option
	public String suffix = null;
	
	@Option()
	public transient int minContextCount = 100;
	@Option(gloss="If you use doSVD how many dimensions to project down to")
	public transient int reducedDimension = 50;
	@Option(gloss="Where to write model")
	public String outfile ;
	@Option(gloss="Whether or not to do SVD")
	public transient boolean doSVD = false;
	@Option(gloss="Whether or not to do Random Projection")
	public transient boolean doRandProj = false;

	
	// Actual Data
	private double[][] denseContextVectors;
	private SmallSparseVector[] sparseContextVectors;
	private Indexer<String> wordIndexer  = new Indexer<String>();
	private enum DataRepn implements Serializable { SPARSE, DENSE }
	private DataRepn dataRepn = DataRepn.SPARSE;

	private int getNumContextTypes() {
		int n = 1;
		if (directional) n *= 2; // LEFT OR RIGHT
		if (appendDistance) n *= contextWindow ;  
		return n;
	}

	private int getContext(int rawDist) {
		if (!directional && !appendDistance) {
			return 0; // All the same
		}
		if (directional && !appendDistance) {
			return rawDist > 0 ? 1: 0;
		}
		if (!directional && appendDistance) {
			return Math.abs(rawDist)-1;
		}
		if (directional && appendDistance) {
			return (rawDist > 0 ? rawDist-1 : rawDist) + contextWindow;
		}
		throw new RuntimeException();
	}

	private int getFeatureIndex(int wordIndex, int rawDist) {
		int contextOffset = getContext(rawDist);
		return getNumContextTypes() * wordIndex + contextOffset;
	}

	private Counter<String> getWordCounter(Iterable<List<String>> sentences) {
		Counter<String> topWordCounter = new Counter<String>();    
		for (List<String> sentence: sentences) {
			topWordCounter.incrementAll(sentence,1.0);
		}  			  	
		return topWordCounter;
	}


	public double[] getContextVector(String word) {
		int wordIndex = wordIndexer.indexOf(word);
		if (wordIndex == -1) return null;
		return denseContextVectors[wordIndex];
	}

//	private void normalizeSparseVector(SparseVector vec) {
//		double norm = 0.0;
//		for (Integer i: vec.keySet()) {
//			double v = vec.getCount(i);
//			norm += v * v;
//		}
//		norm = Math.sqrt(norm);
//		for (Integer i: vec.keySet()) {
//			double v = vec.getCount(i) / norm;
//			vec.setCount(i, v);
//		}
//	}
	
	public double getSimilarity(String word1, String word2) {
		int index1 = wordIndexer.indexOf(word1);
		int index2 = wordIndexer.indexOf(word2);
		if (index1 == -1 || index2 == -1) return -1;
		switch (dataRepn) {
		case DENSE: return getSimilarityDense(index1, index2);
		case SPARSE: return getSimilaritySparse(index1, index2);
		}		
		throw new RuntimeException();
	}

	public double getSimilarityDense(int index1, int index2) {
		double[] vec1= denseContextVectors[index1];
		double[] vec2 = denseContextVectors[index2];
		assert vec1 != null && vec2 != null;
		double sim = DoubleArrays.innerProduct(vec1, vec2);
		return sim;
	}

	public double getSimilaritySparse(int index1, int index2) {
		SmallSparseVector vec1= sparseContextVectors[index1];
		SmallSparseVector vec2 = sparseContextVectors[index2];
		assert vec1 != null && vec2 != null;
		double sim = vec1.dotProduct(vec2);
		return sim;
	}

	public int getContextDimension( ) {
		return 2 * contextWindow * contextWordIndexer.size();
	}

	private void fillSentenceCounts(List<String> sentence, SmallSparseVector[] contextVectors) {
		int sentLength = sentence.size();
		for (int pos=1; pos+1 < sentLength; ++pos) {      
			int wordIndex = wordIndexer.indexOf(sentence.get(pos));
			if (wordIndex == -1) continue;
			for (int i= -contextWindow; i <= contextWindow; ++i) {
				if (i == 0) continue;
				int contextPosition = pos + i;
				if (contextPosition >= 0 && contextPosition < sentence.size()) {
					String contextWord =  sentence.get(contextPosition);   
					int contextIndex = contextWordIndexer.indexOf(contextWord);
					if (contextIndex == -1) continue;
					int featureIndex = getFeatureIndex(contextIndex,i);
					assert featureIndex >= 0;
					assert contextVectors[wordIndex] != null;
					contextVectors[wordIndex].incrementCount(featureIndex, 1.0);
				}
			}        
		}
	}

	private SmallSparseVector[] getContextVectors(Iterable<List<String>> coreSentences) {    
		SmallSparseVector[] contextVectors = new SmallSparseVector[wordIndexer.size()];
		for (int i=0; i < contextVectors.length; ++i) {
			contextVectors[i] = new SmallSparseVector();
		}
		for (List<String> sentence: coreSentences) {
			fillSentenceCounts(sentence, contextVectors);        
		}
		for (SmallSparseVector vector: contextVectors) {
			double norm = vector.l2Norm();
			if (norm > 0.) vector.scale(1.0/norm);
//			normalizeSparseVector(vector);
		}
		return contextVectors;
	}

	public  Iterable<List<String>> getSentences(SentenceIterable processor) {		
		return IterableAdapter.adapt(processor, new Convertor<List<String>, List<String>>() {
			public List<String> convert(List<String> s) {
				List<String> t = new ArrayList<String>(s);
				t.add(0,"<S>");
				t.add("</S>");
				return t;
			}		
		});
	}
	
	public void train(Iterable<List<String>> data, Iterable<List<String>> auxData) { 		
		LogInfo.track("Building Indexers");
		setup(data);
		LogInfo.track("Creating Context Vectors");
		data = new ConcatenationIterable<List<String>>(data,auxData);
		SmallSparseVector[] sparseContextVectors = getContextVectors(data);
		LogInfo.end_track();
		if (doSVD) {
			LogInfo.track("Doing SVD");		
			PrincipalComponentAnalysis pca = new PrincipalComponentAnalysis(sparseContextVectors,reducedDimension);
			denseContextVectors = pca.getProjectionMatrix();
			this.dataRepn = DataRepn.DENSE;
			LogInfo.end_track();
		} else if (doRandProj) {
			LogInfo.track("Doing Random Projection");		
			RandomProjection randProj = new RandomProjection();
			randProj.setInput(sparseContextVectors, reducedDimension);
			denseContextVectors = randProj.getProjectedMatrix();
			this.dataRepn = DataRepn.DENSE;
			LogInfo.end_track();
		}
		else {
			this.sparseContextVectors = sparseContextVectors;
			this.dataRepn = DataRepn.SPARSE;
		}
	}

	private void setup(Iterable<List<String>> data) {
		Counter<String> topWords = getWordCounter(data);
		this.wordIndexer = new Indexer<String>(topWords.getSortedKeys());
		LogInfo.logs("Collecting WordIndexer");
		LogInfo.logs("Size: " + wordIndexer.size());
		LogInfo.logs("Collecting TopWordIndexer");
		topWords.pruneKeysBelowThreshold(minContextCount);
		List<String> contextWords = new ArrayList<String>(topWords.keySet()); 
		this.contextWordIndexer = new Indexer<String>(contextWords);
		LogInfo.logs("Size: " + contextWordIndexer.size());			
		LogInfo.end_track();
	}

	public void run() {
		SentenceIterable primaryProcessor = new SentenceIterable();
		primaryProcessor.dataRoot = dataDirectory;
		primaryProcessor.prefix = primaryPrefix;
		primaryProcessor.extension  = suffix;
		Iterable<List<String>> data = getSentences(primaryProcessor);
		SentenceIterable auxProcessor = new SentenceIterable();
		auxProcessor.dataRoot = dataDirectory;
		auxProcessor.prefix = auxPrefix;
		auxProcessor.extension  = suffix;
		Iterable<List<String>> auxData = getSentences(auxProcessor);		
		train(data,auxData);
		writeModel();
		String[] testWords = {"the","company","bought","green"};
		for (String word: testWords) {
			Counter<String> nn = getNearestNeighbors(word, 10);
			LogInfo.logs("%s => %s",word,nn);
		}
	}

	private void writeModel() {
		if (outfile == null) {
			outfile = (new File(Execution.getVirtualExecDir(),"context_sim.model.gz")).getAbsolutePath(); 
		}
		IOUtils.writeObjFileHard(outfile, this);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		Execution.run(args, new LegacyWordContextSimilarity()); //,"auxData",auxProcessor);
	}

	@Override
	public Collection<String> getVocab() {		
		return wordIndexer;
	}

}

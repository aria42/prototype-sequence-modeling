package edu.berkeley.nlp.prototype.simmodel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.sun.tools.javac.comp.Lower;

import edu.berkeley.nlp.math.DoubleArrays;
import edu.berkeley.nlp.prototype.PrototypeSimilarityModel;
import edu.berkeley.nlp.util.CollectionUtils;
import edu.berkeley.nlp.util.ConcatenationIterable;
import edu.berkeley.nlp.util.Counter;
import edu.berkeley.nlp.util.GeneralPriorityQueue;
import edu.berkeley.nlp.util.PriorityQueue;
import edu.berkeley.nlp.util.IterableAdapter;
import edu.berkeley.nlp.util.experiments.SentenceIterable;
import edu.berkeley.nlp.util.IterableAdapter.Convertor;
import fig.basic.IOUtils;
import fig.basic.Indexer;
import fig.basic.LogInfo;
import fig.basic.Option;
import fig.exec.Execution;

public class WordContextSimilarity extends PrototypeSimilarityModel implements Serializable, Runnable {
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
	@Option(gloss="Lowercase all data")
	public boolean lowercase = false;
	@Option(gloss="File with words to build model over, one per line")
	public String wordFile = null ;
	@Option(gloss="File with words to use for context")
	public String contextWordFile = null ;


	@Option(gloss="How many times do you need to appear to include")
	public transient int numWords = 40000; 
	@Option(gloss="How many times to occur to consider a context word")
	public transient int numContextWords = 1300;
	@Option(gloss="If you use doSVD how many dimensions to project down to")
	public transient int reducedDimension = 50;
	@Option(gloss="Where to write model")
	public String outfile ;

	public enum ReduceDimensionType { SVD, RAND_PROJ, NONE }
	@Option(gloss="What (if any) type of reduction to use")
	public ReduceDimensionType reduceType = ReduceDimensionType.NONE;

	// Actual Data
	private double[][] denseContextVectors;
	private SmallSparseVector[] sparseContextVectors;
	private Indexer<String> wordIndexer  = new Indexer<String>();
	private enum DataRepn implements Serializable { SPARSE, DENSE }
	private DataRepn dataRepn = DataRepn.SPARSE;


	// Constants
	private static final String START_TAG = "<S>";
	private static final String STOP_TAG = "</S>";

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
	//		if (norm > 0.0) {
	//			for (Integer i: vec.keySet()) {
	//				double v = vec.getCount(i) / norm;
	//				vec.setCount(i, v);
	//			}
	//		}
	//	}

	@Override
	public double getSimilarity(String word1, String word2) {
		if (lowercase) {
			word1 = word1.toLowerCase();
			word2 = word2.toLowerCase();
		}
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
			normalizeSparseVector(vector);
		}
		return contextVectors;
	}

	private void normalizeSparseVector(SmallSparseVector vector) {
		double norm = vector.l2Norm();

		if (norm > 0.0) {
			vector.scale(1.0/norm);
			assert Math.abs(vector.l2Norm()-1.0) < 1.0e-4;
		}
	}

	public  Iterable<List<String>> getSentences(SentenceIterable processor) {		
		return IterableAdapter.adapt(processor, new Convertor<List<String>, List<String>>() {
			public List<String> convert(List<String> s) {
				List<String> t = new ArrayList<String>(s);				
				t.add(0,START_TAG);
				t.add(STOP_TAG);
				if (lowercase) {
					for (int i=0; i < t.size(); ++i) {
						t.set(i, t.get(i).toLowerCase());
					}
				}				
				return t;
			}		
		});
	}

	public void train(Iterable<List<String>> data) { 		
		LogInfo.track("Building Indexers");
		setup(data);
		LogInfo.track("Creating Context Vectors");
		SmallSparseVector[] sparseContextVectors = getContextVectors(data);
		LogInfo.end_track();
		reduceDimension(sparseContextVectors);
	}

	private void reduceDimension(SmallSparseVector[] sparseContextVectors) {
		LogInfo.track("Doing reducing dimension: %s",reduceType);
		switch (reduceType) {
		case SVD:		
			PrincipalComponentAnalysis pca = new PrincipalComponentAnalysis(sparseContextVectors,reducedDimension);			
			denseContextVectors = pca.getProjectionMatrix();
			this.dataRepn = DataRepn.DENSE;
			break;
		case RAND_PROJ:		
			RandomProjection randProj = new RandomProjection();
			randProj.setInput(sparseContextVectors, reducedDimension);
			denseContextVectors = randProj.getProjectedMatrix();
			this.dataRepn = DataRepn.DENSE;
			break;
		case NONE:
			this.sparseContextVectors = sparseContextVectors;
			this.dataRepn = DataRepn.SPARSE;
			break;
		default: break;
		}
		LogInfo.end_track();
	}

	private void setup(Iterable<List<String>> data) {
		List<String> topWords = null;
		if (wordFile == null || contextWordFile == null) {
			topWords = getWordCounter(data).getSortedKeys();
		}
		List<String> words = wordFile != null ? IOUtils.readLinesHard(wordFile) :
			topWords.subList(0, Math.min(topWords.size(), numWords));
		this.wordIndexer = new Indexer<String>(words);
		LogInfo.logs("Collecting WordIndexer");
		LogInfo.logs("Size: " + wordIndexer.size());
		LogInfo.logs("Collecting TopWordIndexer");
		List<String> contextWords = contextWordFile != null ?
				IOUtils.readLinesHard(contextWordFile) :
					topWords.subList(0, Math.min(topWords.size(), numContextWords));
				this.contextWordIndexer = new Indexer<String>(contextWords);
				LogInfo.logs("Size: " + contextWordIndexer.size());			
				LogInfo.end_track();
	}

	private static SentenceIterable dataProcessor = new SentenceIterable();

	public void run() {
		Iterable<List<String>> data = getSentences(dataProcessor);
		train(data);
		writeModel();
		LogInfo.logs("Example words and most similar words\n-------------------------");
		String[] testWords = {"the","company","bought","green"};
		for (String word: testWords) {
			if (getVocab().contains(word)) {
				Counter<String> nn = getNearestNeighbors(word, 10);
				LogInfo.logs("%s => %s",word,nn);
			}
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
		Execution.run(args, new WordContextSimilarity(), dataProcessor); //,"auxData",auxProcessor);
	}

	@Override
	public Collection<String> getVocab() {		
		return wordIndexer;
	}

}

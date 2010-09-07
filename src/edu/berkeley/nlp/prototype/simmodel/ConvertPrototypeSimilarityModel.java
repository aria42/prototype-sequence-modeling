package edu.berkeley.nlp.prototype.simmodel;

import java.io.Serializable;
import java.util.Collection;

import edu.berkeley.nlp.math.DoubleArrays;
//import edu.berkeley.nlp.posinduction.WordContextSimilarity;
import edu.berkeley.nlp.prototype.PrototypeSimilarityModel;
import fig.basic.Indexer;

public class ConvertPrototypeSimilarityModel extends PrototypeSimilarityModel implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private double[][] contextVectors;
	private Indexer<String> wordIndexer ;

	public ConvertPrototypeSimilarityModel(double[][] contextVectors, Indexer<String> wordIndexer) {
		this.contextVectors = contextVectors;
		this.wordIndexer = wordIndexer;
	}

	@Override
	public double getSimilarity(String word, String proto) {
		// TODO Auto-generated method stub
		int index1 = wordIndexer.indexOf(word);
		int index2 = wordIndexer.indexOf(proto);
		if (index1 == -1 || index2 == 0) return 0.0;
		return DoubleArrays.innerProduct(contextVectors[index1], contextVectors[index2]);
	}

	@Override
	public Collection<String> getVocab() {
		// TODO Auto-generated method stub
		return wordIndexer;
	}

public static void main(String[] args) {
//	try {
//		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(args[0]));
//		final WordContextSimilarity wcs = (WordContextSimilarity) ois.readObject();
//		PrototypeSimilarityModel simModel = new 
//		ConvertPrototypeSimilarityModel(wcs.contextVectors, new Indexer<String>(wcs.getWordIndexer()));
//		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(args[1]));
//		oos.writeObject(simModel);
//	} catch (Exception e) {
//		e.printStackTrace();
//	}
}
}

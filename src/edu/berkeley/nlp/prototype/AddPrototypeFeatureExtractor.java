package edu.berkeley.nlp.prototype;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.berkeley.nlp.classify.FeatureExtractor;
import edu.berkeley.nlp.util.CollectionUtils;
import edu.berkeley.nlp.util.Counter;
import edu.berkeley.nlp.util.IOUtil;
import fig.basic.IOUtils;
import fig.basic.LogInfo;
import fig.basic.Option;


public class AddPrototypeFeatureExtractor implements FeatureExtractor<String, String> {


	private static final long serialVersionUID = 1L;
	
	@Option(gloss="Number of prototype features per word")
	public int numSimilarWords = 3;
	@Option(gloss="Threshold for similarity")
	public double simThreshold = 0.35;

	private FeatureExtractor<String, String> baseFeatureExtractor ;
	private Set<String> protoWords ;
	private PrototypeSimilarityModel simModel;
	
	public void init(FeatureExtractor<String, String> baseFeatureExtractor,
					PrototypeSimilarityModel simModel,
					Map<String,Set<String>> labelToProtoMap) {
		this.baseFeatureExtractor = baseFeatureExtractor;
		this.protoWords = getPrototypeWords(labelToProtoMap);
		this.simModel = simModel;
	}
	
	private Set<String> getPrototypeWords(Map<String, Set<String>> labelToProtoMap) {
		Set<String> protoWords = new HashSet<String>();
		for (Set<String> s: labelToProtoMap.values()) {
			protoWords.addAll(s);
		}
		return protoWords;
	}
	
	private Set<String> getSimilarPrototypeWords(String word) {
		Counter<String> simScores = new Counter<String>();		
		if (protoWords.contains(word)) {
			return Collections.singleton(word);
		} 
		for (String proto: protoWords) {
			double sim = simModel.getSimilarity(word, proto);
			simScores.setCount(proto,sim);
		}	
		simScores.pruneKeysBelowThreshold(simThreshold);
		List<String> protoFeats = new ArrayList<String>(simScores.getSortedKeys());
		protoFeats = protoFeats.subList(0, Math.min(protoFeats.size(),numSimilarWords));
		return new HashSet<String>(protoFeats);
	}

	public Counter<String> extractFeatures(String word) {
		Counter<String> featCounts = baseFeatureExtractor.extractFeatures(word);
		if (simModel != null) {		
			Set<String> protoWords = getSimilarPrototypeWords(word);//CollectionUtils.getValueSet(protoFeatures,word);
			for (String protoFeat: protoWords) {
				featCounts.incrementCount("proto="+protoFeat, 1.0);
			}
		}
		return featCounts;
	}

}


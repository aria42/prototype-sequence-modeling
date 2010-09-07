package edu.berkeley.nlp.prototype;

import java.util.Collection;
import java.util.Set;

import edu.berkeley.nlp.util.Counter;

public abstract class PrototypeSimilarityModel {
	public abstract double getSimilarity(String word, String proto) ;
	public abstract Collection<String> getVocab();
	public Counter<String> getNearestNeighbors(String word, int n) {
		Counter<String> nearestNeighbors = new Counter<String>();
		for (String other: getVocab()) {
			if (!word.equals(other)) {
				nearestNeighbors.setCount(other, getSimilarity(word, other));
			}
		}
		nearestNeighbors.keepTopNKeys(n);
		return nearestNeighbors;		
	}
	
}

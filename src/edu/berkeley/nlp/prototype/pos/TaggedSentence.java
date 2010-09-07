package edu.berkeley.nlp.prototype.pos;

import java.util.ArrayList;
import java.util.List;

public class TaggedSentence {
	private final List<String> words;
	private final List<String> tags; 
	
	public TaggedSentence(List<String> words, List<String> tags) {
		this.words = words;
		this.tags = tags;
	}
	
	public List<String> getWords() {
		return words;
	}
	
	public List<String> getTags() {
		return tags;
	}
	
	public String toString() {
		List<String> lst = new ArrayList<String>();
		for (int i=0; i < words.size(); ++i) {
			lst.add(words.get(i) + "_" + tags.get(i));		
		}
		return lst.toString();
	}
}

package edu.berkeley.nlp.prototype;

import edu.berkeley.nlp.classify.FeatureExtractor;
import edu.berkeley.nlp.util.Counter;
import fig.basic.Option;

public class BasicWordFeatureExtractor implements FeatureExtractor<String, String> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 42L;
	
	@Option
	public boolean useSuffixFeatures = false;
	@Option
	public int suffixLength = 3;
	@Option
	public boolean useInitialCapital = false;
	@Option
	public boolean useHasDigit = false;
	@Option
	public boolean useHasHyphen = false;

	public Counter<String> extractFeatures(String word) {
		// TODO Auto-generated method stub
		Counter<String> feats = new Counter<String>();
		feats.setCount("word=" + word, 1.0);
		if (useSuffixFeatures) {
			for (int s=1; s <= suffixLength && word.length() > s; ++s) {
				String suffix = word.substring(word.length()-s);
				feats.setCount("suffix-"+(s)+"="+suffix, 1.0);
			}
		}
		if (useHasHyphen) {
			if (word.contains("-")) {
				feats.setCount("hasHypen", 1.0);
			}
		}
		if (useInitialCapital) {
			if (word.matches("^[A-Z].*")) {
				feats.setCount("initCapital", 1.0);
			}
		}
		if (useHasDigit) {
			if (word.matches(".*[0-9].*")) {
				feats.setCount("hasDigit", 1.0);
			}
		}
		return feats;
	} 
	
}

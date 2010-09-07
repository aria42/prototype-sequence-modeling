package edu.berkeley.nlp.prototype;

import java.io.File;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.berkeley.nlp.util.CollectionUtils;
import edu.berkeley.nlp.util.Counter;
import fig.basic.IOUtils;
import fig.basic.LogInfo;
import fig.basic.Option;
import fig.exec.Execution;

public class PrototypeSimilarityListExtractor implements Runnable {

	@Option(gloss="Path to protoype file",required=true)
	public String protoFile ;
	@Option(gloss="Path to PrototypeSimiliarityModel",required=true)
	public String simModelFile ;
	@Option(gloss="Number of prototype features per word")
	public int numSimilarWords = 3;
	@Option(gloss="Threshold for similarity")
	public double simThreshold = 0.35;
	@Option(gloss="Where to write List")
	public String outfile;

	public Map<String, Set<String>> extractProtoFeatures(PrototypeSimilarityModel simModel, Set<String> protoWords) {
		Map<String,Set<String>> protoFeatures = new HashMap<String, Set<String>>();
		Collection<String> simModelVocab = simModel.getVocab();
		for (String word: simModelVocab) {
			Counter<String> simScores = new Counter<String>();
			if (protoWords.contains(word)) {
				protoFeatures.put(word, Collections.singleton(word));
				continue;
			} 
			for (String proto: protoWords) {
				double sim = simModel.getSimilarity(word, proto);
				simScores.setCount(proto,sim);
			}	
			for (String proto: simScores.getSortedKeys()) {
				double sim = simScores.getCount(proto);
				if (sim > simThreshold) {
					CollectionUtils.addToValueSet(protoFeatures, word, proto);
				} else {
					break;
				}
				if (CollectionUtils.getValueSet(protoFeatures, word).size() >= numSimilarWords) {
					break;
				}
			}			
		}
		int numWithProtoFeatures = 0;
		int total = 0; 
		for (String word: simModelVocab) {
			Collection<String> w = CollectionUtils.getValueSet(protoFeatures, word);
			if (!w.isEmpty()) numWithProtoFeatures++;
			total++;
		}
		double coverage = ((double) numWithProtoFeatures) / ((double) total);
		LogInfo.logs("Coverage: %.5f (%d/%d)",coverage,numWithProtoFeatures,total);
		return protoFeatures;
	}

	public void run() {
		LogInfo.track("Reading prototypes from %s",protoFile);
		Map<String, Set<String>> protoMap = PrototypeReader.readProtoypeMap(protoFile);
		LogInfo.end_track();		
		Set<String> protoWords = new HashSet<String>();
		for (Set<String> vals: protoMap.values()) {
			protoWords.addAll(vals);
		}
		LogInfo.track("Loading PrototypeSimModel from %s",simModelFile);
		PrototypeSimilarityModel simModel = (PrototypeSimilarityModel) IOUtils.readObjFileHard(simModelFile);
		LogInfo.end_track();
		LogInfo.track("Extracting Prototype Features");
		Map<String, Set<String>> similarityMap = extractProtoFeatures(simModel, protoWords);
		LogInfo.end_track();		
		if (outfile == null) {
			outfile = (new File(Execution.getVirtualExecDir(),"proto-sim-list.txt.gz")).getAbsolutePath();
		}
		LogInfo.track("Writting Prototype Similarity File to %s",outfile);
		writePrototypeSimilarityFile(similarityMap);
		LogInfo.end_track();
	}

	private void writePrototypeSimilarityFile(Map<String, Set<String>> similarityMap) {
		try {
			PrintWriter pw = IOUtils.openOutHard(outfile);			
			for (Map.Entry<String, Set<String>> entry : similarityMap.entrySet()) {
				String word = entry.getKey();
				Set<String> simWords = entry.getValue();
				StringBuilder builder = new StringBuilder();
				builder.append(word);
				for (String w: simWords) {
					builder.append("\t" + w);
				}
				pw.println(builder.toString());
			}
			pw.flush(); pw.close();
		} catch (Exception e) {			
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		Execution.run(args, new PrototypeSimilarityListExtractor());
	}
	
}

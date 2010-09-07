package edu.berkeley.nlp.prototype;

import java.util.Map;
import java.util.Set;

import edu.berkeley.nlp.classify.FeatureExtractor;
import edu.berkeley.nlp.util.experiments.SentenceIterable;
import fig.basic.IOUtils;
import fig.basic.LogInfo;
import fig.basic.Option;
import fig.exec.Execution;

public class PrototypeSequenceModelTrainer implements Runnable {
		
	@Option(gloss="Prototype File",required=true)
	public transient String protoFile ;
	@Option(gloss="Sequence Model Outfile")
	public transient String outfile ;	
	@Option(gloss="Prototype Similarity Model")
	public transient String simModelPath ;
	
	public void run() {		
		Map<String, Set<String>> prototypeMap = PrototypeReader.readProtoypeMap(protoFile);
		FeatureExtractor<String, String> featExtractor = basicWordExtracotor;
		PrototypeSimilarityModel simModel = null;
		if (simModelPath != null) {
			simModel = (PrototypeSimilarityModel) IOUtils.readObjFileHard(simModelPath);
			addProtoFeatExtractor.init(basicWordExtracotor, simModel, prototypeMap);
		}
		seqModel.train(processor, featExtractor, prototypeMap);
		if (outfile != null) {
			LogInfo.track("Writing Sequence Model to %s",outfile);
			IOUtils.writeObjFileHard(outfile, seqModel);
			LogInfo.end_track();
		}
	}
	
	private static SentenceIterable processor = new SentenceIterable();
	private static FeatureExtractor<String, String> basicWordExtracotor = new BasicWordFeatureExtractor();
	private static PrototypeSequenceModel seqModel = new PrototypeSequenceModel();
	private static AddPrototypeFeatureExtractor addProtoFeatExtractor = new AddPrototypeFeatureExtractor(); 

	public static void main(String[] args) {
		Execution.run(args, new PrototypeSequenceModelTrainer(), processor, basicWordExtracotor, seqModel, addProtoFeatExtractor);
	}

}

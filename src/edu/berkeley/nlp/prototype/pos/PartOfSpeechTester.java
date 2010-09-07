package edu.berkeley.nlp.prototype.pos;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import edu.berkeley.nlp.classify.FeatureExtractor;
import edu.berkeley.nlp.mapper.MapWorker;
import edu.berkeley.nlp.mapper.MapWorkerFactory;
import edu.berkeley.nlp.mapper.Mapper;
import edu.berkeley.nlp.prototype.AddPrototypeFeatureExtractor;
import edu.berkeley.nlp.prototype.BasicWordFeatureExtractor;
import edu.berkeley.nlp.prototype.PrototypeSequenceModel;
import edu.berkeley.nlp.prototype.PrototypeSimilarityModel;
import edu.berkeley.nlp.syntax.Tree;
import edu.berkeley.nlp.syntax.Trees;
import edu.berkeley.nlp.treebank.TreebankFetcher;
import edu.berkeley.nlp.util.CallbackFunction;
import edu.berkeley.nlp.util.IterableAdapter;
import fig.basic.IOUtils;
import fig.basic.LogInfo;
import fig.basic.Option;
import fig.exec.Execution;
import fig.servlet.FileUtils;

public class PartOfSpeechTester implements Runnable {

	@Option(gloss="Which section to start with in Treebank")
	public int startSection = 2;
	@Option(gloss="Which section to stop in Treebank")
	public int stopSection = 24;	
	@Option(gloss="How many sentences to train on")
	public int maxNumSentences = 2000;
	@Option(gloss="Path to wsj/ directory of Treebank",required=true)
	public String treebankPath ;
	@Option(gloss="What fraction of a word usage do we need to have to be a prototype")
	public double protoThresh = 0.50;
	@Option(gloss="How many prototypes per tag")
	public int numProtosPerTag = 3;
	@Option(gloss="Where to write SequenceModel")
	public String outfile ;
	@Option
	public String simModelPath ;


	private boolean isOneToOne(Map<String, Set<String>> protoMap) {
		Set<String> seenWords = new HashSet<String>();
		for (Map.Entry<String, Set<String>> entry : protoMap.entrySet()) {
			Set<String> val = entry.getValue();
			for (String w: val) {
				if (seenWords.contains(w)) {
					LogInfo.logs("The word: '" + w + "' is playing two roles");
					return false;
				}
				seenWords.add(w);
			}
		}
		return true;
	}

	private static FeatureExtractor<String, String> basicWordExtracotor = new BasicWordFeatureExtractor();
	private static AddPrototypeFeatureExtractor addProtoFeatureExtractor = new AddPrototypeFeatureExtractor();
	private static PrototypeSequenceModel seqModel = new PrototypeSequenceModel();

	public void run() {		
		final Iterable<Tree<String>> trees = getTrees();
		final Iterable<List<String>> data = getData(trees);
		Map<String, Set<String>> prototypeMap = getPrototypeMap(trees);
		LogInfo.logs("Is one-to-one => %s", isOneToOne(prototypeMap));
		FeatureExtractor<String, String> featExtractor = getFeatureExtractor(prototypeMap);		
		seqModel.setIterationCallbackFunction(new CallbackFunction() {
			public void callback(final Object... args) {
				LogInfo.track("Evaluation");
				doAccuracy(trees, args);
				LogInfo.end_track();
			}				
		});
		seqModel.train(data, featExtractor, prototypeMap);
		LogInfo.track("Final Evaluation (Pre-Writing Model Out");		
		doAccuracy(trees, new double[0],0);
		LogInfo.end_track();
		doWriteTest(trees);	
	}

	private void doWriteTest(final Iterable<Tree<String>> trees) {
		if (outfile != null) {
			// Test I/O of model
			LogInfo.track("Writing Model to %s",outfile);
			IOUtils.writeObjFileHard(outfile, seqModel);
			LogInfo.end_track();
			// Read Model
			LogInfo.track("Reading Model from %s",outfile);
			seqModel = (PrototypeSequenceModel) IOUtils.readObjFileHard(outfile);
			LogInfo.end_track();
			LogInfo.track("Final Evaluation (Post-Writing Model Out");		
			doAccuracy(trees, new double[0],0);
			LogInfo.end_track();
		}
	}

	private FeatureExtractor<String, String> getFeatureExtractor(
			Map<String, Set<String>> prototypeMap) {
		FeatureExtractor<String, String> featExtractor = basicWordExtracotor;
		if (simModelPath != null) {
			PrototypeSimilarityModel simModel = (PrototypeSimilarityModel) IOUtils.readObjFileHard(simModelPath);
			addProtoFeatureExtractor.init(featExtractor, simModel, prototypeMap);
			featExtractor = addProtoFeatureExtractor;
		}
		return featExtractor;
	}

	private Map<String, Set<String>> getPrototypeMap(
			final Iterable<Tree<String>> trees) {
		Map<String, Set<String>> prototypeMap  = null;
		TreebankPOSPrototypeExtractor protoExtractor = new TreebankPOSPrototypeExtractor();
		protoExtractor.thresh = protoThresh;
		protoExtractor.numProtosPerTag = numProtosPerTag;
		prototypeMap = protoExtractor.extractPrototype(trees);			

		return prototypeMap;
	}

	private Iterable<List<String>> getData(final Iterable<Tree<String>> trees) {
		final Iterable<List<String>> data = IterableAdapter.adapt(trees, new IterableAdapter.Convertor<Tree<String>, List<String>>() {
			public List<String> convert(Tree<String> s) {
				return s.getTerminalYield();
			}		
		});
		return data;
	}

	private Iterable<Tree<String>> getTrees() {
		TreebankFetcher fetcher = new TreebankFetcher();
		fetcher.addTransformer(new Trees.StandardTreeNormalizer());
		fetcher.setMaxTrees(maxNumSentences);		
		if (!FileUtils.isDirectory(treebankPath)) {
			throw new RuntimeException(String.format("%s doesn't exist",treebankPath));
		}
		Iterable<Tree<String>> treesIterable = fetcher.getTrees(treebankPath, startSection, stopSection);		
		return treesIterable;
	}

	private void doAccuracy(final Iterable<Tree<String>> trees, final Object... args) {
		int iter = (Integer) args[1];
		if (iter == 0 ||  (iter+1) % 10 == 0) {
			final AtomicInteger correctViterbi = new AtomicInteger(0);
			final AtomicInteger correctMarginal = new AtomicInteger(0);
			final AtomicInteger total = new AtomicInteger(0);
			Mapper<Tree<String>> mapper = new Mapper<Tree<String>>(new MapWorkerFactory<Tree<String>>() {
				public MapWorker<Tree<String>> newMapWorker() {
					return new MapWorker<Tree<String>>() {
						private PrototypeSequenceModel.Tagger tagger = seqModel.getTagger();						
						@Override
						public void map(Tree<String> tree) {
							List<String> goldLabels = tree.getPreTerminalYield();
							List<String> guessViterbiLabels = tagger.getLabels(tree.getYield());
							List<String> guessMarginalLabels = tagger.getLabelsPosterior(tree.getYield());
							for (int i=0; i < goldLabels.size(); ++i) {
								if (goldLabels.get(i).equals(guessViterbiLabels.get(i))) {
									correctViterbi.incrementAndGet();
								}
								if (goldLabels.get(i).equals(guessMarginalLabels.get(i))) {
									correctMarginal.incrementAndGet();
								}
								total.incrementAndGet();
							}
						}						
					};
				}				
			});			
			mapper.doMapping(trees.iterator());
			double viterbiAccuracy = ((double) correctViterbi.get()) / ((double) total.get());
			double marginalAccuracy = ((double) correctMarginal.get()) / ((double) total.get());
			LogInfo.logsForce("Viterbi Decoding Accuracy: %.3f",viterbiAccuracy);
			LogInfo.logsForce("Marginal Decoding Accuracy: %.3f",marginalAccuracy);
		}
	}		


	public static void main(String[] args) {
		Execution.run(args, new PartOfSpeechTester(), basicWordExtracotor, seqModel,addProtoFeatureExtractor);
	}
}

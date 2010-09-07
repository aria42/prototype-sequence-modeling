package edu.berkeley.nlp.prototype;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import edu.berkeley.nlp.classify.Feature;
import edu.berkeley.nlp.classify.FeatureExtractor;
import edu.berkeley.nlp.classify.FeatureManager;
import edu.berkeley.nlp.classify.LabelFeatureWeightsManager;
import edu.berkeley.nlp.mapper.MapWorker;
import edu.berkeley.nlp.mapper.MapWorkerFactory;
import edu.berkeley.nlp.mapper.Mapper;
import edu.berkeley.nlp.math.CachingDifferentiableFunction;
import edu.berkeley.nlp.math.DoubleArrays;
import edu.berkeley.nlp.math.FigLBFGSMinimizer;
import edu.berkeley.nlp.math.SloppyMath;
import edu.berkeley.nlp.prototype.MarkovStateEncoder.MarkovState;
import edu.berkeley.nlp.sequence.stationary.StationaryForwardBackward;
import edu.berkeley.nlp.sequence.stationary.StationarySequenceInstance;
import edu.berkeley.nlp.sequence.stationary.StationarySequenceModel;
import edu.berkeley.nlp.util.CallbackFunction;
import edu.berkeley.nlp.util.CollectionUtils;
import edu.berkeley.nlp.util.Counter;
import fig.basic.Indexer;
import fig.basic.LogInfo;
import fig.basic.Option;
import fig.basic.Pair;

public class PrototypeSequenceModel implements PrototypeConstants, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 42L;

	// Configurable options
	@Option(gloss="Markov order of sequence model")
	public int order = 2;
	@Option(gloss="Number of iterations")
	public transient int numIters = 100;
	@Option(gloss="Minimum number of iterations")
	public transient int minIters = 50;
	@Option(gloss="L2 Penalty Parameter")
	public double sigmaSquared = 0.5;
	@Option(gloss="What weight is max. absolute value")
	public double maxWeight = 25.0;
	@Option(gloss="Should we use edge transition features")
	public boolean useEdgeFeatures = true;
	@Option(gloss="Use all lengths partition function instead of Length Neighborhood approx")
	public boolean useAllLengthsPartition = true;
	@Option(gloss="Only if you want to see lots of data go by")
	public transient boolean verbose = false;
	@Option(gloss="How many CPUs to use. Defaults to number Available")
	public int numCPUS = Runtime.getRuntime().availableProcessors();

	// Serialized Data				
	private FeatureExtractor<String, String> wordFeatureExtractor ;
	private Map<String, Set<String>> labelToProtoMap ;			
	private double[] weights ;
	private Indexer<String> vocab ;		
	private int averageLength ;
	private int minLength ;
	private Counter<Integer> sentLengthHistogram ;

	// Transient Data
	private transient MarkovStateEncoder<String> markovStateEncoder ;
	private transient FeatureManager featManager ;
	private transient Indexer<String> labels ;
	private transient Indexer<MarkovState<String>> markovStates ;
	private transient Map<String, Set<String>> protoToLabelMap ;
	private transient LabelFeatureWeightsManager<String> weightsManager;
	private transient List<List<String>> data ;	 
	private transient double[][] edgeForwardPotentials; 
	private transient double[][] edgeBackwardPotentials;
	private transient double[][] wordPotentials ;  
	private transient double[][] wordGivenLabel;
	private transient double[] sumWordPotentials;
	private transient int[] stateToLabelIndex ;
	private transient Pair<Integer,Double>[][] cachedWordFeatures ;
	private transient int[][] cachedEdgeFeature ;
	private transient CallbackFunction iterCallbackFunction ;

	private void debug(String fmt, Object...args) {
		if (verbose) {
			LogInfo.logs(fmt,args);
		}
	}

	private void debug_track(String fmt, Object...args) {
		if (verbose) {
			LogInfo.track(fmt,args,true);
		}
	}

	private void debug_end_track() {
		if (verbose) {
			LogInfo.end_track();
		}
	}

	public void setIterationCallbackFunction(CallbackFunction callback) {
		this.iterCallbackFunction = callback;
	}

	private List<List<String>> addStartStopIterable(Iterable<List<String>> data) {
		List<List<String>> modData = new ArrayList<List<String>>();
		for (List<String> sent: data) {
			modData.add(addStartStop(sent));
		}
		return modData;
	}

	private List<String> addStartStop(List<String> s) {
		List<String> t = new ArrayList<String>(s);
		if (!s.get(0).equals(START_WORD)) {
			t.add(0, START_WORD);
		}
		if (!s.get(s.size()-1).equals(STOP_WORD)) {
			t.add(STOP_WORD);
		}
		return t;
	}

	/**
	 * Add start and stop tags with start and stop words as prototypes
	 * @param labelToProtoMap
	 * @return
	 */
	private Map<String, Set<String>> addStartStopToProtoMap(Map<String, Set<String>> labelToProtoMap) {
		labelToProtoMap = new HashMap<String, Set<String>>(labelToProtoMap);
		labelToProtoMap.put(START_LABEL, Collections.singleton(START_WORD));
		labelToProtoMap.put(STOP_LABEL, Collections.singleton(STOP_WORD));
		return labelToProtoMap;
	}
	
	
	
	/**
	 * 
	 * @param data
	 * @param wordFeatureExtractor Feature Extractor (possibly including prototype similarty features)
	 * @param labelToProtoMap Map of labels to set of prototype words
	 */
	public void train(Iterable<List<String>> data,  
			FeatureExtractor<String, String> wordFeatureExtractor,
			Map<String, Set<String>> labelToProtoMap) 
	{
		this.data = addStartStopIterable(data);
		this.wordFeatureExtractor = wordFeatureExtractor;
		this.labelToProtoMap = addStartStopToProtoMap(labelToProtoMap);
		setup();		
		LogInfo.track("Training");
		LogInfo.logs("PrototypeMap: %s",labelToProtoMap);
		LogInfo.logs("Number of prototypes: %d",protoToLabelMap.keySet().size());
		
		FigLBFGSMinimizer minimizer = new FigLBFGSMinimizer(numIters);
		if (iterCallbackFunction != null) {
			minimizer.setIterationCallbackFunction(iterCallbackFunction);
		}
		minimizer.setMinIteratons(minIters);
		ObjectiveFunction objFn = new ObjectiveFunction();			
		weights = minimizer.minimize(objFn, new double[objFn.dimension()], 1.0e-4);
		LogInfo.end_track();
	}

	/**
	 * Invert the label -> prototype map
	 * @return
	 */
	private Map<String, Set<String>> makeProtoToLabelMap() {
		Map<String, Set<String>> protoToLabelMap = new HashMap<String, Set<String>>();
		for (Map.Entry<String, Set<String>> entry : labelToProtoMap.entrySet()) {
			String label = entry.getKey();
			Set<String> protos = entry.getValue();
			for (String proto: protos) {
				CollectionUtils.addToValueSet(protoToLabelMap, proto, label);			
			}
		}
		return protoToLabelMap;
	}

	private Counter<String> getWordFeatureCounter(String word) {
		return word.equals(START_WORD) || word.equals(STOP_WORD) ?
				new Counter<String>() :
					wordFeatureExtractor.extractFeatures(word);
	}

	private void extractFeatures() {		
		extractVocab();
		LogInfo.track("Feature Extraction");
		LogInfo.logs("Vocab Size (including start and stop): %d",vocab.size());
		if (useEdgeFeatures) LogInfo.logs("Using Edge Features");
		extractNodePredicates();		
		if (useEdgeFeatures) extractEdgePredicates();		
		featManager.lock();
		weightsManager = new LabelFeatureWeightsManager<String>(featManager, labels);
		LogInfo.logs("There are %d labels and %d predicates yielding %d features",
				labels.size(),featManager.getNumFeatures(),weightsManager.getNumWeights());		
		cacheFeatures();
		LogInfo.end_track();		
	}

	@SuppressWarnings("unchecked")
	private void cacheFeatures() {
		cachedWordFeatures = new Pair[vocab.size()][];
		int numCovered = 0, total = 0;
		for (int w=0; w < vocab.size(); ++w) {
			Counter<String> featCounts = getWordFeatureCounter(vocab.getObject(w));
			cachedWordFeatures[w] = new Pair[featCounts.size()];
			int fIndex=0; 
			boolean protoCovered = false;
			for (Map.Entry<String, Double> entry : featCounts.getEntrySet()) {
				String featStr = entry.getKey();
				if (featStr.contains("proto=")) {
					protoCovered = true;
				}
				double count = entry.getValue();
				Feature f = featManager.getFeature(featStr);
				cachedWordFeatures[w][fIndex++] = Pair.newPair(f.getIndex(), count);
			}
			if (protoCovered) {
				numCovered++;
			}
			total++;
		}
		LogInfo.logs("Prototype Covered: %.3f",((double)numCovered)/((double)total));
		if (useEdgeFeatures) {
			cachedEdgeFeature = new int[markovStates.size()][];
			for (int s=0; s < markovStates.size(); ++s) {
				MarkovState<String> state = markovStates.getObject(s);
				if (isStopState(state)) {
					continue;
				}
				cachedEdgeFeature[s] = new int[labels.size()];
				for (int l=0; l < labels.size(); ++l) {										
					String markovState = markovStates.getObject(s).toString();
					String label = labels.getObject(l);
					cachedEdgeFeature[s][l] =  weightsManager.getWeightIndex(markovState, label);
				}
			}
		}
	}

	private void extractEdgePredicates() {
		for (MarkovState<String> state: markovStates) {
			if (!isStopState(state)) {
				featManager.addFeature(state.toString());
			}

		}
	}

	private void extractNodePredicates() {
		featManager = new FeatureManager();
		Set<String> wordPreds = new HashSet<String>();
		int numPreds = 0; int total = 0;
		for (String word: vocab) {
			Counter<String> wordFeatures = getWordFeatureCounter(word);
			for (String feat: wordFeatures.keySet()) {
				featManager.addFeature(feat);
				wordPreds.add(feat);				
			}
			numPreds += wordFeatures.size();
			total++;
		}
		LogInfo.logs("Number of node predicates: %d",wordPreds.size());
		//		LogInfo.logs("Word Predicates: %s", wordPreds);
		LogInfo.logs("Avg. # of preds: %.3f",((double) numPreds)/((double) total-2.0));
	}

	private void extractVocab() {
		if (vocab != null) { // When we serialize in, we already have a vocab
			return;
		}
		vocab = new Indexer<String>();
		LogInfo.track("Extracting voab and featueres");
		int sumLength = 0, numSents = 0;
		int numTokens = 0;
		minLength = Integer.MAX_VALUE;
		sentLengthHistogram = new Counter<Integer>();
		for (List<String> sent: data) {
			minLength = Math.min(sent.size(), minLength);
			vocab.addAll(sent);
			sumLength += sent.size();
			numSents++;
			numTokens += (sent.size()-2);
			sentLengthHistogram.incrementCount(sent.size(), 1.0);
		}
		averageLength = (int) (((double) sumLength)/ ((double) numSents));
		LogInfo.logs("Extracted vocab from %d sentences and %d tokens excluding START and STOP",numSents,numTokens);
		if (verbose) LogInfo.logs("All Length Partition from %d to %d",minLength,averageLength) ;
		LogInfo.end_track();
	}
	
	private void setup() {
		LogInfo.track("setup",true);
		List<String> labelsList = new ArrayList<String>(labelToProtoMap.keySet());		
		Collections.sort(labelsList);
		LogInfo.track("Extracting State Space");
		this.labels = new Indexer<String>(labelsList);
		this.protoToLabelMap = makeProtoToLabelMap();
		this.markovStateEncoder = new MarkovStateEncoder<String>(labels,START_LABEL,STOP_LABEL,order);		
		this.markovStates = markovStateEncoder.getStates();
		this.stateToLabelIndex = new int[markovStates.size()];
		initStateToLabelIndex();		
		LogInfo.end_track();
		LogInfo.logs("Labels (%d): %s",labelsList.size(), labelsList);
		LogInfo.logs("Number of Markov States: %d",markovStates.size());
		extractFeatures();	
		LogInfo.end_track();
	}

	private void initStateToLabelIndex() {
		for (int s=0; s < markovStates.size(); ++s) {
			int labelIndex = labels.indexOf(markovStates.getObject(s).getCurrentLabel());
			assert labelIndex >= 0;
			stateToLabelIndex[s] = labelIndex;
		}
	}

	private void fillWordPotentials() {
		if (wordPotentials == null) {
			wordPotentials = new double[vocab.size()][labels.size()];
		}
		for (int w=0; w < vocab.size(); ++w) {
			for (int l=0; l < labels.size(); ++l) {
				double logPotential = getWordLabelLogPotential(w, l);//SloppyMath.exp(logPotential);
				double potential = SloppyMath.exp(logPotential);
				assert !Double.isInfinite(potential) && potential >= 0.0 : String.format("Bad potential: %.5f",potential);
				wordPotentials[w][l] = potential; 
			}				
		}		
	}

	private double getWordLabelLogPotential(int w, int l) {
		String word = vocab.getObject(w);
		String label = labels.getObject(l);
		Set<String> protoLabels = protoToLabelMap.get(word);
		if (protoLabels != null) {  // Word has a protoype
			// IF the label isn't the prototype label, kill it
			if(!protoLabels.contains(label)) return Double.NEGATIVE_INFINITY;
			// If the word is a START or STOP word, fix logPotential to 0.0
			if(word.equals(START_WORD) || word.equals(STOP_WORD)) {
				return 0.0;
			}
		}
		// No words should be allowed to go with START or STOP label
		if (label.equals(START_LABEL) || label.equals(STOP_LABEL)) {
			return Double.NEGATIVE_INFINITY;
		}

		double sum = 0.0;		
		for (Pair<Integer, Double> entry : cachedWordFeatures[w]) {
			int f = entry.getFirst();
			Double val = entry.getSecond();
			int weightIndex = weightsManager.getFeatureLabelWeightIndex(f,l);
			sum += val * weights[weightIndex];
		}

		assert !Double.isInfinite(sum) && !Double.isNaN(sum);
		return sum;
	}

	private class LengthSequenceInstance implements StationarySequenceInstance {
		int len ;
		double[] startPotential = DoubleArrays.constantArray(0.0, labels.size());
		double[] stopPotential = DoubleArrays.constantArray(0.0, labels.size());

		public LengthSequenceInstance(int len) {
			this.len = len;
			startPotential[labels.indexOf(START_LABEL)] = 1.0;//.setCount(START_LABEL, 1.0);
			stopPotential[labels.indexOf(STOP_LABEL)] = 1.0;//.setCount(STOP_LABEL, 1.0);
		}
		public void fillNodePotentials(double[][] potentials) {
			for (int i=0; i < getSequenceLength(); ++i) {
				if (i == 0) fillNodePotentialsAbstract(potentials[i], startPotential);
				else if (i == len-1) fillNodePotentialsAbstract(potentials[i], stopPotential);
				else fillNodePotentialsAbstract(potentials[i], sumWordPotentials);
			}				
		}
		public int getSequenceLength() {
			return len ;
		}
	}

	private void fillEdgePotentials() {

		if (edgeForwardPotentials == null) {
			this.edgeForwardPotentials = new double[markovStates.size()][];
			this.edgeBackwardPotentials = new double[markovStates.size()][];
		}

		// All legal transitions have potential of 1.0
		if (!useEdgeFeatures) {
			for (int s=0; s < markovStates.size(); ++s) {
				int[] successors = markovStateEncoder.getForwardTransitionMatrix()[s];
				edgeForwardPotentials[s] = DoubleArrays.constantArray(1.0, successors.length);
				int[] parents = markovStateEncoder.getBackwardTransitionMatrix()[s];
				edgeBackwardPotentials[s] = DoubleArrays.constantArray(1.0, parents.length);
			}
			return;
		}

		for (int s=0; s < markovStates.size(); ++s) {			
			int[] successors = markovStateEncoder.getForwardTransitionMatrix()[s];
			edgeForwardPotentials[s] = new double[successors.length];
			MarkovState<String> state = markovStates.getObject(s);			
			for (int tIndex=0; tIndex < successors.length; ++tIndex) {
				int t = successors[tIndex];				
				MarkovState<String> next = markovStates.getObject(t);
				if (isStopState(state)) {
					edgeForwardPotentials[s][tIndex] = 0.0;
				} else {
					int weightIndex = weightsManager.getWeightIndex(state.toString(), next.getCurrentLabel());
					edgeForwardPotentials[s][tIndex] = SloppyMath.exp(weights[weightIndex]);
				}
			}
			int[] parents = markovStateEncoder.getBackwardTransitionMatrix()[s];
			edgeBackwardPotentials[s] = new double[parents.length];
			for (int tIndex=0; tIndex < parents.length; ++tIndex) {
				int t = parents[tIndex];
				MarkovState<String> prev = markovStates.getObject(t);
				if (isStopState(prev)) {
					edgeBackwardPotentials[s][tIndex] = 0.0;
				} else {
					int weightIndex = weightsManager.getWeightIndex(prev.toString(),state.getCurrentLabel());
					edgeBackwardPotentials[s][tIndex] = SloppyMath.exp(weights[weightIndex]);
				}
			}
		}
	}

	private boolean isStopState(MarkovState<String> state) {
		return state.getCurrentLabel().equals(STOP_LABEL);
	}

	private void fillNodePotentialsAbstract(double[] potentials, double[] labelPotentals) {
		assert potentials.length == markovStates.size();
		for (int s=0; s < markovStates.size(); ++s) {
			//			MarkovState<String> state = markovStates.get(s);
			int labelIndex = stateToLabelIndex[s];
			double p = labelPotentals[labelIndex];
			assert p >= 0.0 && p < Double.POSITIVE_INFINITY;
			potentials[s] = p;
		}
	}

	private class PrototypeSequenceInstance implements StationarySequenceInstance {

		private List<String> sent;

		public PrototypeSequenceInstance(List<String> sent0) {
			this.sent = sent0;
		}

		public void fillNodePotentials(double[][] potentials) {
			for (int i=0; i < sent.size() ; ++i) {
				Arrays.fill(potentials[i],0.0);
				String word = sent.get(i);				
				int wordIndex = vocab.indexOf(word);
				fillNodePotentialsAbstract(potentials[i],  wordPotentials[wordIndex]);
			}
		}

		public int getSequenceLength() {
			return sent.size();
		}

	}

	private class PrototypeSequenceModelInternal implements StationarySequenceModel {
		public int[][] getAllowableBackwardTransitions() {
			return markovStateEncoder.getBackwardTransitionMatrix();
		}

		public int[][] getAllowableForwardTransitions() {
			return markovStateEncoder.getForwardTransitionMatrix();
		}

		public double[][] getBackwardEdgePotentials() {
			return edgeBackwardPotentials;
		}

		public double[][] getForwardEdgePotentials() {
			return edgeForwardPotentials;
		}

		public int getMaximumSequenceLength() {
			return 200;
		}

		public int getNumStates() {
			return markovStates.size();
		}				

	}

	private double[][] getNodeLabelMarginals(double[][] nodeStateMarginals, int curLength) {
		double[][] nodeMarginals = new double[curLength][labels.size()];
		for (int i=0; i < curLength; ++i) {
			assert Math.abs(DoubleArrays.add(nodeStateMarginals[i])-1.0) < 1.0e-4 : 
				String.format("nodeMarginals[%d] = %.5f",i,DoubleArrays.add(nodeStateMarginals[i]));
			for (int s=0; s < markovStates.size(); ++s) {
				int l = stateToLabelIndex[s];//markovStateEncoder.getLabelIndex(s); 
				nodeMarginals[i][l] += nodeStateMarginals[i][s];
			}
		}
		return nodeMarginals;
	}

	private interface PartitionFunction {		
		double update(List<String> sent, double[] derivative);
	}

	private class StandardPartitionFunction implements PartitionFunction {
		StationaryForwardBackward fb = new StationaryForwardBackward(new PrototypeSequenceModelInternal());

		public double update(List<String> sent, double[] derivative) {			
			StationarySequenceInstance inst = new PrototypeSequenceInstance(sent);
			fb.setInput(inst);

			double[][] nodeStateMarginals = fb.getNodeMarginals();
			double[][] edgeMarginals = fb.getEdgeMarginalSums();
			// Node Update
			for (int i=0; i < inst.getSequenceLength(); ++i) {
				if (i == 0 || i+1 == inst.getSequenceLength()) continue;
				String word = sent.get(i);
				int wordIndex = vocab.indexOf(word);				
				for (int s=0; s < markovStates.size(); ++s) {
					int labelIndex = stateToLabelIndex[s];
					double posterior = nodeStateMarginals[i][s];
					if (posterior == 0.0) continue;
					for (Pair<Integer, Double> pair: cachedWordFeatures[wordIndex]) {
						int featureIndex = pair.getFirst();
						double count = pair.getSecond();						
						int weightIndex = weightsManager.getFeatureLabelWeightIndex(featureIndex, labelIndex);
						derivative[weightIndex] += (count * posterior) ;
					}
				}
			}
			// Edge Update
			updateEdgeFeatures(derivative, edgeMarginals, true);
			return fb.getLogNormalizationConstant();
		}
	}

	private void updateEdgeFeatures(double[] derivative, double[][] edgeMarginals, boolean add) {
		if (!useEdgeFeatures) return; 
		for (int s=0; s < markovStates.size(); ++s) {
			MarkovState<String> state = markovStates.getObject(s);
			if (state.getCurrentLabel().equals(START_LABEL)) {
				continue;
			}
			int[] nextStates = markovStateEncoder.getForwardTransitionMatrix()[s];			
			for (int tIndex=0; tIndex < nextStates.length; ++tIndex) {
				int t = nextStates[tIndex];
				double edgePosterior = edgeMarginals[s][tIndex];		
				int labelIndex = stateToLabelIndex[t];
				assert !labels.getObject(labelIndex).equals(START_LABEL);
				int weightIndex = cachedEdgeFeature[s][labelIndex];
				derivative[weightIndex] += (add ? edgePosterior : -edgePosterior);
			}
		}
	}

	private class LengthPartitionFunction implements PartitionFunction {

		StationaryForwardBackward fb = new StationaryForwardBackward(new PrototypeSequenceModelInternal());

		public double update(List<String> sent, double[] derivative) {
			Pair<Double, double[]> cache = doLength(sent.size());
			DoubleArrays.addInPlace(derivative, cache.getSecond());
			return cache.getFirst();
		}

		private Pair<Double, double[]> doLength(int len) {
			double[] cacheDerivative = new double[weightsManager.getNumWeights()];
			StationarySequenceInstance inst = new LengthSequenceInstance(len);
			fb.setInput(inst);

			double[][] nodeMarginals = fb.getNodeMarginals();
			double[][] edgeMarginals = fb.getEdgeMarginalSums();
			double[][] nodeLabelMarginals = getNodeLabelMarginals(nodeMarginals,len);
			double[] nodeLabelSumMarginals = new double[labels.size()];
			for (int i=1; i+1 < inst.getSequenceLength(); ++i) {
				assert Math.abs(DoubleArrays.add(nodeLabelMarginals[i])-1.0) < 1.0e-4;
				DoubleArrays.addInPlace(nodeLabelSumMarginals, nodeLabelMarginals[i]);
			}

			// Node Update
			for (int l=0; l < labels.size(); ++l) {
				double[] wordDistr = wordGivenLabel[l];//.getCounter(labels.getObject(l));
				assert Math.abs(DoubleArrays.add(wordDistr)-1.0) < 1.0e-4;
				String label = labels.getObject(l);
				if (label.equals(START_LABEL) || label.equals(STOP_LABEL)) {
					continue;
				}
				double nodeLabelMarginal = nodeLabelSumMarginals[l];
				for (int w=0; w < vocab.size(); ++w) {						
					double wordGivenLabelProb = wordDistr[w];//.getCount(vocab.getObject(w));
					String word = vocab.getObject(w);
					if (word.equals(START_WORD) || word.equals(STOP_WORD)) continue;
					for (Pair<Integer,Double> p: cachedWordFeatures[w]) {
						int f = p.getFirst().intValue();
						double count = p.getSecond();
						int weightIndex = weightsManager.getFeatureLabelWeightIndex(f, l);
						cacheDerivative[weightIndex] -= (count * nodeLabelMarginal) * wordGivenLabelProb;
					}
				}
			}
			// Edge Update
			updateEdgeFeatures(cacheDerivative, edgeMarginals, false);

			double cacheValue = fb.getLogNormalizationConstant();
			return Pair.newPair(cacheValue, cacheDerivative);						
		}
	}

	public class Tagger {

		private StationaryForwardBackward taggingForwardBackwards = new StationaryForwardBackward(new PrototypeSequenceModelInternal());

		
		private class TestingSequenceInstance implements StationarySequenceInstance  {
		
			private List<String> sent; 
			
			private TestingSequenceInstance(List<String> sent) {
				this.sent = sent;
			}

			public void fillNodePotentials(double[][] potentials) {
				// TODO Auto-generated method stub
				for (int i=0; i < sent.size(); ++i) {
					String word = sent.get(i);
					int wordIndex = vocab.indexOf(word);
					double[] labelPotentials = null;
					if (wordIndex >= 0) {
						labelPotentials = wordPotentials[wordIndex];
					} else {
						Counter<String> feats = getWordFeatureCounter(word);
						labelPotentials = new double[labels.size()];
						for (int l=0; l < labelPotentials.length; ++l) {
							double sum = 0.0;
							for (Map.Entry<String, Double> entry : feats.getEntrySet()) {
								String f = entry.getKey();
								if (!featManager.hasFeature(f)) continue;
								Feature feat = featManager.getFeature(f);
								double count = entry.getValue();
								int featIndex = feat.getIndex();								
								int weightIndex = weightsManager.getFeatureLabelWeightIndex(featIndex, l);
								double term  = weights[weightIndex];
								if (count != 1.0) {
									term = Math.pow(term,count);
								}
								sum += term;
							}	
							labelPotentials[l] = SloppyMath.exp(sum); 
						}						
					}
					fillNodePotentialsAbstract(potentials[i], labelPotentials);
				}
			}

			public int getSequenceLength() {
				// TODO Auto-generated method stub
				return sent.size();
			}
			
		}
		
		public List<String> getLabelsViterbi(List<String> sent) {
			sent = addStartStop(sent);
			taggingForwardBackwards.setInput(new TestingSequenceInstance(sent),true);
			int[] states = taggingForwardBackwards.viterbiDecode();
			List<String> tags = new ArrayList<String>();
			for (int s: states) {
				MarkovState<String> state = markovStates.getObject(s);
				tags.add(state.getCurrentLabel());
			}
			tags.remove(0);
			tags.remove(tags.size()-1);
			return tags;
		}

		public List<String> getLabelsPosterior(List<String> sent) {
			sent = addStartStop(sent);
			taggingForwardBackwards.setInput(new TestingSequenceInstance(sent));
			int[] states = taggingForwardBackwards.nodePosteriorDecode();
			List<String> tags = new ArrayList<String>();
			for (int s: states) {
				MarkovState<String> state = markovStates.getObject(s);
				tags.add(state.getCurrentLabel());
			}
			tags.remove(0);
			tags.remove(tags.size()-1);
			return tags;
		}

		public List<String> getLabels(List<String> sent) {
			return getLabelsViterbi(sent);
		}

	}

	public Tagger getTagger() {
		return new Tagger();
	}

	private void fillSumWordsPotenials() {
		if (wordGivenLabel == null) {
			sumWordPotentials = new double[labels.size()];//new Counter<String>();
			wordGivenLabel = new double[labels.size()][vocab.size()];//new CounterMap<String, String>();
		}
		for (int l=0; l < labels.size(); ++l) {
			double sum = 0.0;
			for (int w=0; w < vocab.size(); ++w) {				
				wordGivenLabel[l][w] = wordPotentials[w][l];
				sum += wordGivenLabel[l][w];
			}
			assert sum > 0.0;
			sumWordPotentials[l] = sum ;// .setCount(label, sum);
			DoubleArrays.scale(wordGivenLabel[l], 1.0/sum);
		}			
	}

	private double clipWeight(double weight) { 
		if (weight > maxWeight) { return maxWeight; }
		if (weight < -maxWeight) { return -maxWeight; }
		return weight;
	}

	private double[] clipWeights(double[] weights) {
		double[] clippedWeights = new double[weights.length];
		for (int i=0; i < weights.length; ++i) {
			clippedWeights[i] = clipWeight(weights[i]);
		}
		return clippedWeights;
	}

	private void setWeights(double[] weights) {
		this.weights = clipWeights(weights);
		fillWordPotentials();
		fillEdgePotentials();
		fillSumWordsPotenials();
	}

	class MyWorker extends MapWorker<List<String>> {				
		double[] gradient ;
		double logProb = 0.0;
		PartitionFunction numeratorPartFunction, denomnatorPartitionFunction;

		MyWorker( ) {
			numeratorPartFunction = new StandardPartitionFunction();
			gradient = new double[weightsManager.getNumWeights()];
		}

		@Override
		public void map(List<String> item) {
			// TODO Auto-generated method stub
			try {
				double numerLogProb = numeratorPartFunction.update(item, gradient);
				logProb += numerLogProb;
			} catch (Exception e) {
				LogInfo.logs("Error processing: %s",item);				
			}
		}

	}

	private double doDenominator(double[] gradient) {
		LengthPartitionFunction lengthPartitionFunction = new LengthPartitionFunction();		
		if (useAllLengthsPartition) {
			int numLengths = averageLength-minLength+1;
			double[] logSumVals = DoubleArrays.constantArray(Double.NEGATIVE_INFINITY, numLengths);
			double[][] derivatives = new double[numLengths][];
			for (int len=minLength; len <= averageLength; ++len) {
				int i = len - minLength;
				Pair<Double, double[]> p = lengthPartitionFunction.doLength(len);
				logSumVals[i] = p.getFirst();
				derivatives[i] = p.getSecond();
			}
			double allLogSum = SloppyMath.logAdd(logSumVals);
			for (int len=minLength; len <= averageLength; ++len) logSumVals[len-minLength] -= allLogSum;
			double[] lengthProbs = DoubleArrays.exponentiate(logSumVals);
			assert Math.abs(DoubleArrays.add(lengthProbs)-1.0) < 1.0e-4;			
			int numDatums = data.size();
			for (int len=minLength; len <= averageLength; ++len) {
				int i = len-minLength;			
				for (int j=0; j < gradient.length; ++j) {
					gradient[j] += numDatums * lengthProbs[i] * derivatives[i][j];
				}
			}
			return numDatums * allLogSum;
		} else {
			double finalValue = 0.0;
			for (Map.Entry<Integer, Double> entry : sentLengthHistogram.getEntrySet()) {
				Integer len = entry.getKey();
				double count = entry.getValue();
				Pair<Double, double[]> p = lengthPartitionFunction.doLength(len);
				finalValue += p.getFirst() * count;
				double[] lenDeriv = p.getSecond();
				for (int i=0; i < lenDeriv.length; ++i) {
					gradient[i] += lenDeriv[i] * count;
				}				
			}
			return finalValue;
		}
	}


	private class ObjectiveFunction extends CachingDifferentiableFunction {
		@Override
		protected Pair<Double, double[]> calculate(double[] x) {

			debug_track("calculate()",true);							

			setWeights(x);			

			debug("Weight Norm: %.10f",DoubleArrays.vectorLength(weights));

			Mapper<List<String>> mapper = new Mapper<List<String>>(new MapWorkerFactory<List<String>>(){
				public MapWorker<List<String>> newMapWorker() {
					return new MyWorker(); 					
				}
			});			
			mapper.setNumWorkers(numCPUS);
			List<MapWorker<List<String>>> workers = mapper.doMapping(data);
			double logProb = 0.0;
			double[] derivative = new double[dimension()];
			for (MapWorker<List<String>> worker: workers) {
				MyWorker myWorker = (MyWorker) worker;
				logProb += myWorker.logProb;
				DoubleArrays.addInPlace(derivative, myWorker.gradient);
			}
			double allDenomProb = doDenominator(derivative);
			logProb -= allDenomProb;

			// Negate, so we minimize negative log-likelihood
			logProb *= -1.0;
			DoubleArrays.scale(derivative,-1.0);

			debug("Log Likelihood: %.15f",logProb);			
			double l2Penalty = addL2Penalty(x,derivative);
			debug("L2 Penalty: %.5f",l2Penalty);
			logProb += l2Penalty;
			debug("Final Value: %.15f",logProb);
			debug("Gradient Norm: %.15f",DoubleArrays.vectorLength(derivative));
			debug_end_track();

			// Regularize
			return Pair.newPair(logProb, derivative);
		}

		private double addL2Penalty(double[] x, double[] derivative) {
			double regularization = 0.0;
			if (sigmaSquared > 0.0 && sigmaSquared < Double.POSITIVE_INFINITY) {
				for (int w=0; w < x.length; ++w) {
					double weight = x[w];
					regularization += weight * weight / (2 * sigmaSquared);
					derivative[w] += weight / sigmaSquared;
				}
			}
			return regularization;
		}

		@Override
		public int dimension() {
			return weightsManager.getNumWeights();
		}
	}


	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
	{
		in.defaultReadObject() ;
		setup();
		setWeights(weights);
	}

}

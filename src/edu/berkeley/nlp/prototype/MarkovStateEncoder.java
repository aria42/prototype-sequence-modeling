package edu.berkeley.nlp.prototype;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


import edu.berkeley.nlp.syntax.Tree;
import edu.berkeley.nlp.util.CollectionUtils;
import fig.basic.Indexer;

public class MarkovStateEncoder<L> implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 42;
	
	private transient int order;
	private transient Collection<L> labels ;
	private transient L startLabel, stopLabel ; 
	private Indexer<MarkovState<L>> stateIndexer;
	private int[][] forwardTransitionMatrix ;
	private int[][] backwardTransitionMatrix ;

	public static class MarkovState<L> implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = 42;
		
		private L current ;
		private List<L> history;
		private int index ;		
		private String cacheStr ;

		public MarkovState(L current, List<L> history) {
			this.current = current;
			this.history = history;			
		}	

		public String toString() {
			if (cacheStr == null) {
				cacheStr = String.format("%s | %s",current,history);
			}
			return cacheStr;
		}
		public L getCurrentLabel() {
			return current;
		}
		public List<L> getHistory() {
			return Collections.unmodifiableList(history);
		}
		public int getIndex() {
			return index;
		}
		//		public boolean isStartState() {
		//			return isStart;
		//		}
		//		public boolean isStopState() {
		//			return isStop;
		//		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
			+ ((current == null) ? 0 : current.hashCode());
			result = prime * result
			+ ((history == null) ? 0 : history.hashCode());
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			MarkovState<String> other = (MarkovState<String>) obj;
			if (current == null) {
				if (other.current != null)
					return false;
			} else if (!current.equals(other.current))
				return false;
			if (history == null) {
				if (other.history != null)
					return false;
			} else if (!history.equals(other.history))
				return false;
			return true;
		}

	}

	public Indexer<MarkovState<L>> getStates() {
		return stateIndexer;
	}

	public int[][] getForwardTransitionMatrix() {
		return forwardTransitionMatrix;
	}

	public int[][] getBackwardTransitionMatrix() {
		return backwardTransitionMatrix;
	}
	
	private void orderOneCase(Indexer<L> labels0, L startLabel, L stopLabel) {
		this.order = 1;
		labels = new Indexer<L>(labels0);
		labels.add(startLabel); labels.add(stopLabel);
		this.startLabel = startLabel;
		this.stopLabel = stopLabel;
		stateIndexer = new Indexer<MarkovState<L>>();
		MarkovState<L> startState = null;
		MarkovState<L> stopState = null;
		int count = 0;
		for (L label: labels
				) {
			MarkovState<L> state = new MarkovState<L>(label, new ArrayList<L>());
			stateIndexer.add(state);
			if (label.equals(startLabel)) {
				startState = state;
			}
			if (label.equals(stopLabel)) {
				stopState = state;
			}
			state.index = count++;
		}
		List<MarkovState<L>> nontermStates = new ArrayList<MarkovState<L>>(stateIndexer);
		nontermStates.remove(startState); nontermStates.remove(stopState);
				
		Map<MarkovState<L>, List<MarkovState<L>>> forwardSparse = new HashMap<MarkovState<L>, List<MarkovState<L>>>();
		for (MarkovState<L> state: stateIndexer) {
			List<MarkovState<L>> successors = new ArrayList<MarkovState<L>>();
			if (state != stopState) { 
				successors.addAll(nontermStates);
				successors.add(stopState);
			}
			forwardSparse.put(state, successors);
		}
		Map<MarkovState<L>, List<MarkovState<L>>> backSparse = new HashMap<MarkovState<L>, List<MarkovState<L>>>();
		for (Map.Entry<MarkovState<L>, List<MarkovState<L>>> entry : forwardSparse.entrySet()) {
			MarkovState<L> state = entry.getKey();
			List<MarkovState<L>> nextStates = entry.getValue();
			for (MarkovState<L> nextState: nextStates) {
				CollectionUtils.addToValueList(backSparse, nextState, state);
			}
		}
		forwardTransitionMatrix = convertSparse(forwardSparse);
		backwardTransitionMatrix = convertSparse(backSparse);
		
	}
	 
	public MarkovStateEncoder(Indexer<L> labels0, L startLabel, L stopLabel, int order) {
		
		if (order == 1) { orderOneCase(labels0, startLabel, stopLabel); return; }
		
		this.labels = new ArrayList<L>(labels0);
		this.labels.remove(startLabel); this.labels.remove(stopLabel);
		
		this.order = order;
		this.startLabel = startLabel;
		this.stopLabel = stopLabel;

		stateIndexer = new Indexer<MarkovState<L>>();		
		buildTreeRec(new MarkovState<L>(null, new ArrayList<L>()), 0);
		buildTransition();
		//		this.stateToLabelIndex = new int[stateIndexer.size()];
		//		for (MarkovState<L> state: stateIndexer) {
		//			L label = state.getCurrentLabel();
		//			state.labelIndex = labels.indexOf(label);
		//			assert state.labelIndex >= 0 : "Label Not Found: " + label;
		//			stateToLabelIndex[state.index] = state.labelIndex ;
		//			System.out.printf("Mapping %s to %s\n", stateIndexer.getObject(state.index),labels.getObject(state.labelIndex));
		//		}

	}

	private void buildTransition() {

		Map<MarkovState<L>, MarkovState<L>> stateInterner = new HashMap<MarkovState<L>, MarkovState<L>>();
		for (MarkovState<L> state: stateIndexer) {
			stateInterner.put(state, state);
		}

		Collection<L> nextLabels = new ArrayList<L>(labels);
		nextLabels.add(stopLabel);
		Collection<L> prevLabels = new ArrayList<L>(labels);
		prevLabels.add(startLabel);		

		Map<MarkovState<L>, List<MarkovState<L>>> forwardSparse = new HashMap<MarkovState<L>, List<MarkovState<L>>>();		
		for (MarkovState<L> state: stateIndexer) {
			List<L> history = state.getHistory();			
			// Forward
			List<MarkovState<L>> nextStates = new ArrayList<MarkovState<L>>();
			for (L next: nextLabels) {
				List<L> newHistory = new ArrayList<L>(history.subList(Math.min(1,history.size()), history.size()));
				newHistory.add(state.getCurrentLabel());
				MarkovState<L> nextStateOld = new MarkovState<L>(next, newHistory);
				MarkovState<L> nextState = stateInterner.get(nextStateOld);
				if (nextState != null) nextStates.add(nextState);				
			}
			forwardSparse.put(state, nextStates);
		}

		forwardTransitionMatrix = convertSparse(forwardSparse);
		Map<MarkovState<L>, List<MarkovState<L>>> backSparse = new HashMap<MarkovState<L>, List<MarkovState<L>>>();
		for (Map.Entry<MarkovState<L>, List<MarkovState<L>>> entry : forwardSparse.entrySet()) {
			MarkovState<L> state = entry.getKey();
			List<MarkovState<L>> nextStates = entry.getValue();
			for (MarkovState<L> nextState: nextStates) {
				CollectionUtils.addToValueList(backSparse, nextState, state);
			}
		}
		backwardTransitionMatrix = convertSparse(backSparse);
	}

	private int[][] convertSparse(Map<MarkovState<L>, List<MarkovState<L>>> sparse) {
		int[][] transition = new int[stateIndexer.size()][];
		for (int s=0; s < stateIndexer.size(); ++s) {
			MarkovState<L> state = stateIndexer.getObject(s);
			List<MarkovState<L>> trans = CollectionUtils.getValueList(sparse,state);
			transition[s] = new int[trans.size()];
			for (int t=0; t < trans.size(); ++t) {
				transition[s][t] = trans.get(t).index;
			}
			Arrays.sort(transition[s]);
		}
		return transition;
	}

	//	public int getLabelIndex(int stateIndex) {
	//		return stateToLabelIndex[stateIndex];
	//	}

	private Tree<MarkovState<L>> buildTreeRec(MarkovState<L> state, int curDepth) {

		curDepth += 1;

		List<L> history = state.getHistory();
		if (history.size() == order-1 || startLabel.equals(state.current) || (history.size() > 0 && history.get(0).equals(startLabel))) {
			if ((!startLabel.equals(state.current) || state.history.isEmpty()) && !state.history.contains(stopLabel)) {
				if (!stopLabel.equals(state.current) || !state.history.isEmpty()) {
					stateIndexer.add(state);
					state.index = stateIndexer.indexOf(state);					
				}
			}
		}

		if (curDepth > order) {
			return new Tree<MarkovState<L>>(state, new ArrayList<Tree<MarkovState<L>>>());
		}

		Set<L> nextLabels = new HashSet<L>(labels);

		if (curDepth == order) {
			nextLabels.add(stopLabel);
		}	
		if (curDepth == 1) {
			nextLabels.add(startLabel);
		}

		List<Tree<MarkovState<L>>> childTrees = new ArrayList<Tree<MarkovState<L>>>();
		for (L nextLabel: nextLabels) {
			List<L> newHistory = new ArrayList<L>(state.getHistory());
			if (state.getCurrentLabel() != null) newHistory.add(state.getCurrentLabel());
			MarkovState<L> child = new MarkovState<L>(nextLabel,newHistory);
			Tree<MarkovState<L>> childTree = buildTreeRec(child, curDepth);
			childTrees.add(childTree);
		}
		return new Tree<MarkovState<L>>(state, childTrees);
	}

	/**
	 * Test for MarkovStateEncoder
	 * @param args
	 */
	public static void main(String[] args) {
		Indexer<String> labels = new Indexer<String>(CollectionUtils.makeList("A","B"));
		MarkovStateEncoder<String> encoder = new MarkovStateEncoder<String>(labels,"START","STOP",1);
		List<MarkovState<String>> states = encoder.getStates();
		for (MarkovState<String> state: states) {
			System.out.println(state);
		}
	}

}

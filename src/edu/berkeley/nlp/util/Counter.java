package edu.berkeley.nlp.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;


/**
 * A map from objects to doubles.  Includes convenience methods for getting,
 * setting, and incrementing element counts.  Objects not in the counter will
 * return a count of zero.  The counter is backed by a HashMap (unless specified
 * otherwise with the MapFactory constructor).
 *
 * @author Dan Klein
 */
public class Counter<E> implements Serializable {
	private static final long serialVersionUID = 1L;
	Map<E, Double> entries;
	boolean dirty = true;
	double cacheTotal = 0.0;
	MapFactory<E, Double> mf;

	/**
	 * The elements in the counter.
	 *
	 * @return set of keys
	 */
	public Set<E> keySet() {
		return entries.keySet();
	}
	
	public Set<Entry<E,Double>> entrySet() { 
		return entries.entrySet(); 
	}


	/**
	 * The number of entries in the counter (not the total count -- use totalCount() instead).
	 */
	public int size() {
		return entries.size();
	}

	/**
	 * True if there are no entries in the counter (false does not mean totalCount > 0)
	 */
	public boolean isEmpty() {
		return size() == 0;
	}

	/**
	 * Returns whether the counter contains the given key.  Note that this is the
	 * way to distinguish keys which are in the counter with count zero, and those
	 * which are not in the counter (and will therefore return count zero from
	 * getCount().
	 *
	 * @param key
	 * @return whether the counter contains the key
	 */
	public boolean containsKey(E key) {
		return entries.containsKey(key);
	}

	/**
	 * Get the count of the element, or zero if the element is not in the
	 * counter.
	 *
	 * @param key
	 * @return
	 */
	public double getCount(E key) {
		Double value = entries.get(key);
		if (value == null) return 0;
		return value;
	}
	
	/**
	 * I know, I know, this should be wrapped in a Distribution class, but
	 * it's such a common use...why not. Returns the MLE prob. Assumes all 
	 * the counts are >= 0.0 and totalCount > 0.0. If the latter is false,
	 * return 0.0 (i.e. 0/0 == 0)
	 * @author Aria 
	 * @param key
	 * @return MLE prob of the key
	 */
	public double getProbability(E key) {
		double count = getCount(key);
		double total = totalCount();
		if (total < 0.0) { 
			throw new RuntimeException("Can't call getProbability() with totalCount < 0.0"); 
		}		
		return total > 0.0 ? count / total : 0.0;
	}

	/**
	 * Destructively normalize this Counter in place.
	 */
	public void normalize() {
		double totalCount = totalCount();
		for (E key : keySet()) {
			setCount(key, getCount(key) / totalCount);
		}
		dirty = true;
	}

	/**
	 * Set the count for the given key, clobbering any previous count.
	 *
	 * @param key
	 * @param count
	 */
	public void setCount(E key, double count) {
		entries.put(key, count);
		dirty = true;
	}
	
	/**
	 * Set the count for the given key if it is larger than the previous one;
	 *
	 * @param key
	 * @param count
	 */
	public void put(E key, double count, boolean keepHigher) {
		if (keepHigher && entries.containsKey(key)){
			double oldCount = entries.get(key);
			if (count > oldCount){
				entries.put(key, count);
			}
		} else {
			entries.put(key, count);
		}
		dirty = true;
	}

	/**
	 * Will return a sample from the counter, will throw exception 
	 * if any of the counts are < 0.0 or if the totalCount() <= 0.0
	 * @return
	 * 
	 * @author aria42
	 */
	public E sample(Random rand) {
		double total = totalCount();
		if (total <= 0.0) {
			throw new RuntimeException(String.format("Attempting to sample() with totalCount() %.3f\n", total));
		}
		double sum = 0.0;
		double r = rand.nextDouble();
		for (Map.Entry<E, Double> entry: entries.entrySet()) {
			double count = entry.getValue();
			double frac = count /total;
			sum += frac;
			if (r < sum) {
				return entry.getKey();
			}
		}
		throw new IllegalStateException("Shoudl've have returned a sample by now....");
	}
	
	/**
	 * Will return a sample from the counter, will throw exception 
	 * if any of the counts are < 0.0 or if the totalCount() <= 0.0
	 * @return
	 * 
	 * @author aria42
	 */
	public E sample() {
		return sample(new Random());
	}
	
	public void removeKey(E key) {
		setCount(key, 0.0);
		dirty = true;
		removeKeyFromEntries(key);
	}

	/**
	 * @param key
	 */
	protected void removeKeyFromEntries(E key)
	{
		entries.remove(key);
	}
	

	/**
	 * Increment a key's count by the given amount.
	 *
	 * @param key
	 * @param increment
	 */
	public void incrementCount(E key, double increment) {
		setCount(key, getCount(key) + increment);
		dirty = true;
	}

	/**
	 * Increment each element in a given collection by a given amount.
	 */
	public void incrementAll(Collection<? extends E> collection, double count) {
		for (E key : collection) {
			incrementCount(key, count);
		}
		dirty = true;
	}

	public <T extends E> void incrementAll(Counter<T> counter) {
		for (T key : counter.keySet()) {
			double count = counter.getCount(key);
			incrementCount(key, count);
		}
		dirty = true;
	}

	/**
	 * Finds the total of all counts in the counter.  This implementation iterates
	 * through the entire counter every time this method is called.
	 *
	 * @return the counter's total
	 */
	public double totalCount() {
		if (!dirty) {
			return cacheTotal;
		}
		double total = 0.0;
		for (Map.Entry<E, Double> entry : entries.entrySet()) {
			total += entry.getValue();
		}
		cacheTotal = total;
		dirty = false;
		return total;
	}
	
	public List<E> getSortedKeys() {
		PriorityQueue<E> pq = this.asPriorityQueue();
		List<E> keys = new ArrayList<E>();
		while (pq.hasNext()) {
			keys.add(pq.next());
		}
		return keys;
	}
	
	

	/**
	 * Finds the key with maximum count.  This is a linear operation, and ties are broken arbitrarily.
	 *
	 * @return a key with minumum count
	 */
	public E argMax() {
		double maxCount = Double.NEGATIVE_INFINITY;
		E maxKey = null;
		for (Map.Entry<E, Double> entry : entries.entrySet()) {
			if (entry.getValue() > maxCount || maxKey == null) {
				maxKey = entry.getKey();
				maxCount = entry.getValue();
			}
		}
		return maxKey;
	}
	
	public double min()
	{
		return maxMinHelp(false);
	}
	
	public double max()
	{
		return maxMinHelp(true);
	}
	
	private double maxMinHelp(boolean max)
	{
		double maxCount = max ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
		
		for (Map.Entry<E, Double> entry : entries.entrySet()) {
			if ((max && entry.getValue() > maxCount) || (!max && entry.getValue()  < maxCount)) {
				
				maxCount = entry.getValue();
			}
		}
		return maxCount;
	}

	/**
	 * Returns a string representation with the keys ordered by decreasing
	 * counts.
	 *
	 * @return string representation
	 */
	@Override
	public String toString() {
		return toString(keySet().size());
	}

	/**
	 * Returns a string representation which includes no more than the
	 * maxKeysToPrint elements with largest counts.
	 *
	 * @param maxKeysToPrint
	 * @return partial string representation
	 */
	public String toString(int maxKeysToPrint) {
		return asPriorityQueue().toString(maxKeysToPrint);
	}

	/**
	 * Builds a priority queue whose elements are the counter's elements, and
	 * whose priorities are those elements' counts in the counter.
	 */
	public PriorityQueue<E> asPriorityQueue() {
		PriorityQueue<E> pq = new PriorityQueue<E>(entries.size());
		for (Map.Entry<E, Double> entry : entries.entrySet()) {
			pq.add(entry.getKey(), entry.getValue());
		}
		return pq;
	}
	
	/**
	 * Warning: all priorities are the negative of their counts in the counter here
	 * @return
	 */
	public PriorityQueue<E> asMinPriorityQueue() {
		PriorityQueue<E> pq = new PriorityQueue<E>(entries.size());
		for (Map.Entry<E, Double> entry : entries.entrySet()) {
			pq.add(entry.getKey(), -entry.getValue());
		}
		return pq;
	}

	public Counter() {
		this(false);
	}

	public Counter(boolean identityHashMap) {
		this(identityHashMap ? new MapFactory.IdentityHashMapFactory<E, Double>()
				: new MapFactory.HashMapFactory<E, Double>());
	}

	public Counter(MapFactory<E, Double> mf) {
		this.mf = mf;
		entries = mf.buildMap();
	}

	public Counter(Counter<? extends E> counter) {
		this();
		incrementAll(counter);
	}

	public Counter(Collection<? extends E> collection) {
		this();
		incrementAll(collection, 1.0);
	}

	public void pruneKeysBelowThreshold(double cutoff) {
		Iterator<E> it = entries.keySet().iterator();
		while (it.hasNext()) {
			E key = it.next();
			double val = entries.get(key);
			if (val < cutoff) {
				it.remove();
			}
		}
		dirty = true;
	}
	
	public Set<Map.Entry<E, Double>> getEntrySet() {
		return entries.entrySet();
	}
	
	public boolean isEqualTo(Counter<E> counter)
	{
		boolean tmp = true;
		Counter<E> bigger = counter.size() > size() ? counter : this;
		for (E e : bigger.keySet())
		{
			tmp &= counter.getCount(e) == getCount(e);
		}
		return tmp;
	}

	public static void main(String[] args) {
		Counter<String> counter = new Counter<String>();
		System.out.println(counter);
		counter.incrementCount("planets", 7);
		System.out.println(counter);
		counter.incrementCount("planets", 1);
		System.out.println(counter);
		counter.setCount("suns", 1);
		System.out.println(counter);
		counter.setCount("aliens", 0);
		System.out.println(counter);
		System.out.println(counter.toString(2));
		System.out.println("Total: " + counter.totalCount());
	}
	
	public void clear()
	{
		entries = mf.buildMap();
		dirty = true;
	}

	public void keepTopNKeys(int keepN)
	{
		keepKeysHelper(keepN,true);
	}
	
	public void keepBottomNKeys(int keepN)
	{
		keepKeysHelper(keepN,false);
	}
	
	private void keepKeysHelper(int keepN, boolean top)
	{
		Counter<E> tmp = new Counter<E>();

		int n = 0;
		for (E e : Iterators.able(top ? asPriorityQueue() : asMinPriorityQueue()))
		{
			

			if (n <= keepN) tmp.setCount(e, getCount(e));
			 n++;

		}
		clear();
		incrementAll(tmp);
		dirty = true;
		
		
	}

	
	/**
	 * Sets all counts to the given value, but does not remove any keys
	 */
	public void setAllCounts(double val)
	{
		for (E e : keySet())
		{
			setCount(e,val);
		}
		
	}

	public double dotProduct(Counter<E> other) {
		double sum = 0.0;
		for(Map.Entry<E, Double > entry: getEntrySet()) {
			final double otherCount = other.getCount(entry.getKey());
			if (otherCount == 0.0) continue;
			final double value = entry.getValue();
			if (value == 0.0) continue;
			sum += value * otherCount;
			
		}
		return sum;
	}

	public void scale(double c)
	{
		
		for (Map.Entry<E, Double> entry : getEntrySet())
		{
			entry.setValue(entry.getValue() * c);
		}

	}

}

package edu.berkeley.nlp.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fig.basic.Pair;

/**
 * @author Dan Klein
 */
public class CollectionUtils {
	public static <E extends Comparable<E>> List<E> sort(Collection<E> c) {
		List<E> list = new ArrayList<E>(c);
		Collections.sort(list);
		return list;
	}
	
	public static <E> boolean isSublistOf(List<E> bigger, List<E> smaller) {
	    if (smaller.size() > bigger.size()) return false;
	    for (int start=0; start + smaller.size() <= bigger.size(); ++start) {
	        List<E> sublist = bigger.subList(start, start+smaller.size());
	        if (sublist.equals(bigger)) {
	            return true;
	        }
	    }
	    return false;
	}

	public static <E> List<E> sort(Collection<E> c, Comparator<E> r) {
		List<E> list = new ArrayList<E>(c);
		Collections.sort(list, r);
		return list;
	}
	
	public static <K,V> void addToValueSet(Map<K, Set<V>> map, K key, V value) {
		Set<V> values = map.get(key);
		if (values == null) {
			values = new HashSet<V>();
			map.put(key, values);
		}
		values.add(value);
	}

	public static <K, V> void addToValueList(Map<K, List<V>> map, K key, V value) {
		List<V> valueList = map.get(key);
		if (valueList == null) {
			valueList = new ArrayList<V>();
			map.put(key, valueList);
		}
		valueList.add(value);
	}

	public static <K, V> List<V> getValueList(Map<K, List<V>> map, K key) {
		List<V> valueList = map.get(key);
		if (valueList == null)
			return Collections.emptyList();
		return valueList;
	}
	
	public static <K, V> Set<V> getValueSet(Map<K, Set<V>> map, K key) {
		Set<V> valueSet = map.get(key);
		if (valueSet == null)
			return Collections.emptySet();
		return valueSet;
	}

	public static <T> List<T> makeList(T... args) {
		return new ArrayList<T>(Arrays.asList(args));
	}
	
	public static <T> void quicksort(T[] array, Comparator<? super T> c)
	{

		quicksort(array, 0, array.length - 1, c);

	}

	public static <T> void quicksort(T[] array, int left0, int right0, Comparator<? super T> c)
	{

		int left, right;
		T pivot, temp;
		left = left0;
		right = right0 + 1;

		final int pivotIndex = (left0 + right0) / 2;
		pivot = array[pivotIndex];
		temp = array[left0];
		array[left0] = pivot;
		array[pivotIndex] = temp;

		do
		{

			do
				left++;
			while (left <= right0 && c.compare(array[left], pivot) < 0);

			do
				right--;
			while (c.compare(array[right], pivot) > 0);

			if (left < right)
			{
				temp = array[left];
				array[left] = array[right];
				array[right] = temp;
			}

		} while (left <= right);

		temp = array[left0];
		array[left0] = array[right];
		array[right] = temp;

		if (left0 < right) quicksort(array, left0, right, c);
		if (left < right0) quicksort(array, left, right0, c);

	}
	
	public static <S,T> Iterable<Pair<S,T>> getPairIterable(final Iterable<S> sIterable, final Iterable<T> tIterable) {
		return new Iterable<Pair<S,T>>() {
			public Iterator<Pair<S, T>> iterator() {
				class PairIterator implements Iterator<Pair<S,T>> {
					
					private Iterator<S> sIterator ;
					private Iterator<T> tIterator ;
					
					private PairIterator() {
						sIterator = sIterable.iterator();
						tIterator = tIterable.iterator();
					}
					
					public boolean hasNext() {
						// TODO Auto-generated method stub
						return sIterator.hasNext() && tIterator.hasNext();
					}

					public Pair<S, T> next() {
						// TODO Auto-generated method stub
						return Pair.newPair(sIterator.next(), tIterator.next());
					}

					public void remove() {
						// TODO Auto-generated method stub
						sIterator.remove();
						tIterator.remove();
					}					
				};
				return new PairIterator();
			}			
		};
	}
	
}

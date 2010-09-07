package edu.berkeley.nlp.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import edu.berkeley.nlp.util.ConcatenationIterator;

public class ConcatenationIterable<T> implements Iterable<T> {

	private Collection<Iterable<T>> iterableColl;
	
	public ConcatenationIterable(Collection<Iterable<T>> iterableColl) {
		this.iterableColl = iterableColl;
	}
	
	public ConcatenationIterable(Iterable<T>...iterables) {
		this.iterableColl = Arrays.asList(iterables);
	}
	
	public Iterator<T> iterator() {
		Collection<Iterator<T>> itColl = new ArrayList<Iterator<T>>();
		for (Iterable<T> iterable: iterableColl) {
			itColl.add(iterable.iterator());
		}
		return new ConcatenationIterator(itColl);
	}

}

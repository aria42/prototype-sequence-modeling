package edu.berkeley.nlp.util;

import java.util.Iterator;
import java.util.Queue;

public class BufferedIterator<T> implements Iterator<T> {
	
	private int bufSize ;
	private Iterator<T> it;
	private Queue<T> buffer ;
	
	public BufferedIterator(Iterator<T> it, int bufSize) {
		this.it = it;
		this.bufSize = bufSize;
		padBuffer();
	}
	
	public BufferedIterator(Iterator<T> it) {
		this(it, 16);
	}

	public boolean hasNext() {
		// TODO Auto-generated method stub
		return padBuffer();
	}
	
	private boolean padBuffer() {
		if (buffer.isEmpty()) {
			for (int i=0; i < bufSize && it.hasNext(); ++i) {
				buffer.add(it.next());
			}
		}
		return buffer.isEmpty();
	}

	public T next() {
		// TODO Auto-generated method stub
		padBuffer();
		return buffer.element();
	}

	public void remove() {
		// TODO Auto-generated method stub
		it.remove();
	}
}

package fr.emersion.gradualspan.po2;

import java.util.Iterator;
import java.util.NoSuchElementException;

class NotNullIterator<T> implements Iterator<T> {
	private Iterator<T> iter;
	private T next;

	public NotNullIterator(Iterator<T> iter) {
		this.iter = iter;
	}

	public boolean hasNext() {
		if (this.next != null) {
			return true;
		}
		if (!this.iter.hasNext()) {
			return false;
		}

		try {
			this.next = this.iter.next();
		} catch (NoSuchElementException e) {
			return false;
		}

		if (this.next == null) {
			return this.hasNext();
		}

		return true;
	}

	public T next() throws NoSuchElementException {
		if (this.next == null) {
			throw new NoSuchElementException();
		}

		T next = this.next;
		this.next = null;
		return next;
	}
}

package fr.emersion.gradualspan;

public interface ValuedItemset extends Iterable<ValuedItem> {
	public Iterable<ValuedItemset> children();
}

package fr.emersion.gradualspan;

// TODO: rename to ValuedNode
public interface ValuedItemset extends Iterable<ValuedItem> {
	public Iterable<ValuedItemset> children();
}

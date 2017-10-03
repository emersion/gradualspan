package fr.emersion.gradualspan;

public interface ValuedNode extends Iterable<ValuedItem> {
	public Iterable<ValuedNode> children();
	public String name();
}

package fr.emersion.gradualspan;

public interface ValuedNode extends Iterable<ValuedItem> {
	public Iterable<ValuedNode> children();

	default public String name() {
		return null;
	}
}

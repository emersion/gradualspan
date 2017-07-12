package fr.emersion.gradualspan;

public interface ValuedItem {
	public Object item();
	public GradualItem toGradual(ValuedItem other);
}

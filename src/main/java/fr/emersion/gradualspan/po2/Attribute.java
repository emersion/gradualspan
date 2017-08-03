package fr.emersion.gradualspan.po2;

import fr.emersion.gradualspan.GradualItem;
import fr.emersion.gradualspan.GradualOrder;
import fr.emersion.gradualspan.ValuedItem;

public class Attribute implements ValuedItem {
	private String k;
	private float v;

	protected Attribute(String k, float v) {
		this.k = k;
		this.v = v;
	}

	public Object item() {
		return this.k;
	}

	public GradualItem toGradual(ValuedItem other) {
		Attribute attr = (Attribute) other;

		float delta = attr.v - this.v;
		GradualOrder go = GradualOrder.EQUAL;
		if (delta != 0) {
			if (delta > 0) {
				go = GradualOrder.GREATER;
			} else {
				go = GradualOrder.LOWER;
			}
		}
		return new GradualItem(this.k, go);
	}
}

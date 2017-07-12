package fr.emersion.gradualspan;

public class GradualItem {
	public final Object item;
	public final GradualOrder order;

	public GradualItem(Object item, GradualOrder order) {
		this.item = item;
		this.order = order;
	}

	public int hashCode() {
		return this.item.hashCode() + this.order.hashCode();
	}

	public boolean equals(Object other) {
		if (!(other instanceof GradualItem)) {
			return false;
		}
		GradualItem gi = (GradualItem) other;
		return this.item.equals(gi.item) && this.order.equals(gi.order);
	}

	public String toString() {
		return this.item.toString() + this.order.toString();
	}
}

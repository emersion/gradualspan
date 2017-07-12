package fr.emersion.gradualspan;

class Pair<F, S> {
	public final F first;
	public final S second;

	public Pair(F first, S second) {
		this.first = first;
		this.second = second;
	}

	public String toString() {
		return "("+this.first.toString()+", "+this.second.toString()+")";
	}

	public boolean equals(Object other) {
		if (!(other instanceof Pair)) {
			return false;
		}

		Pair<?, ?> pair = (Pair<?, ?>) other;
		if (this.first != null && pair.first != null) {
			if (!this.first.equals(pair.first)) {
				return false;
			}
		} else if (this.first == null || pair.first == null) {
			return false;
		}
		if (this.second != null && pair.second != null) {
			if (!this.second.equals(pair.second)) {
				return false;
			}
		} else if (this.second == null || pair.second == null) {
			return false;
		}
		return true;
	}
}

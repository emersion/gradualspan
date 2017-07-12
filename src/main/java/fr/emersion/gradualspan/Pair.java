package fr.emersion.gradualspan;

import java.util.Objects;

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

	public int hashCode() {
		return Objects.hash(this.first, this.second);
	}

	public boolean equals(Object other) {
		if (!(other instanceof Pair)) {
			return false;
		}

		Pair<?, ?> pair = (Pair<?, ?>) other;
		return Objects.equals(this.first, pair.first) && Objects.equals(this.second, pair.second);
	}
}

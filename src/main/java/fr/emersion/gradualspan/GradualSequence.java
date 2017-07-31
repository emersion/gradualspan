package fr.emersion.gradualspan;

import java.util.Objects;

public class GradualSequence {
	public final GradualNode begin;
	public final GradualNode end;

	public GradualSequence() {
		this.begin = new GradualNode();
		this.end = new GradualNode();
	}

	public GradualSequence(GradualNode begin) {
		this.begin = begin;
		this.end = new GradualNode();

		this.close();
	}

	public GradualSequence(GradualNode begin, GradualNode end) {
		this.begin = begin;
		this.end = end;
	}

	public void close() {
		// TODO
	}

	public boolean equals(Object other) {
		if (!(other instanceof GradualSequence)) {
			return false;
		}
		GradualSequence gs = (GradualSequence) other;
		return Objects.equals(this.begin, gs.begin) && Objects.equals(this.end, gs.end);
	}

	public int hashCode() {
		return Objects.hash(this.begin, this.end);
	}
}

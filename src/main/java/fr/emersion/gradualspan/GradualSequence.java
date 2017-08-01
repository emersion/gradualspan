package fr.emersion.gradualspan;

import java.util.Objects;
import java.util.Set;

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

	/**
	 * Attaches trailing nodes to this.end.
	 */
	public void close() {
		this.closeNode(this.begin);
	}

	private void closeNode(GradualNode n) {
		if (n.children.size() == 0) {
			n.putChild(null, this.end);
			return;
		}

		Set<GradualNode> children = n.children.keySet();
		if (children.contains(this.end)) {
			return;
		}

		for (GradualNode child : children) {
			this.closeNode(child);
		}
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

	public String toString() {
		return this.begin.toString();
	}
}

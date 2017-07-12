package fr.emersion.gradualspan;

import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class GradualNode {
	private static class Arrow {
		// key = gi.item + target
		private final GradualItem gi;
		private final GradualNode target;

		private Arrow(GradualItem gi, GradualNode target) {
			this.gi = gi;
			this.target = target;
		}

		@Override
		public int hashCode() {
			if (this.gi == null) {
				return this.target.hashCode();
			}
			return this.target.hashCode() + this.gi.item.hashCode();
		}

		@Override
		public boolean equals(Object other) {
			if (!(other instanceof Arrow)) {
				return false;
			}
			Arrow otherArrow = (Arrow) other;
			if (!this.target.equals(otherArrow.target)) {
				return false;
			}
			if (this.gi == null || otherArrow.gi == null) {
				return this.gi == null && otherArrow.gi == null;
			}
			return this.gi.item.equals(otherArrow.gi.item);
		}
	}

	private Set<Arrow> children = new HashSet<>();

	public GradualNode() {}

	public void putChild(GradualItem gi, GradualNode n) {
		this.children.add(new Arrow(gi, n));
	}

	public boolean equals(Object other) {
		if (!(other instanceof GradualNode)) {
			return false;
		}
		GradualNode gn  = (GradualNode) other;

		if (this.children.size() != gn.children.size()) {
			return false;
		}

		// TODO: this can be optimized
		for (Arrow a1 : this.children) {
			boolean found = false;
			for (Arrow a2 : gn.children) {
				if ((a1.gi == null && a2.gi == null) || (a1.gi != null && a1.gi.equals(a2.gi))) {
					if (a1.target.equals(a2.target)) {
						found = true;
						break;
					}
				}
			}
			if (!found) {
				return false;
			}
		}
		return true;
	}

	private String toStringWithIndent(String indent) {
		if (this.children.size() == 0) {
			return "{}";
		}

		String subIndent = indent + "  ";

		Map<GradualNode, List<GradualItem>> childenByNode = new HashMap<>();
		for (Arrow a : this.children) {
			GradualItem gi = a.gi;
			GradualNode child = a.target;

			if (!childenByNode.containsKey(child)) {
				childenByNode.put(child, new ArrayList<>());
			}
			childenByNode.get(child).add(gi);
		}

		String s = this.hashCode()+"{\n";
		for (Map.Entry<GradualNode, List<GradualItem>> e : childenByNode.entrySet()) {
			GradualNode gn = e.getKey();

			s += subIndent;
			for (GradualItem gi : e.getValue()) {
				if (gi != null) {
					s += gi.toString();
				} else {
					s += "null";
				}
				s += " ";
			}

			s += gn.toStringWithIndent(subIndent) + "\n";
		}
		s += indent+"}";
		return s;
	}

	public String toString() {
		return this.toStringWithIndent("");
	}
}

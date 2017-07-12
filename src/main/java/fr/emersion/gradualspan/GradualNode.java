package fr.emersion.gradualspan;

import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class GradualNode {
	private Map<GradualNode, Set<GradualItem>> children = new HashMap<>();

	public GradualNode() {}

	public void putChild(GradualItem gi, GradualNode n) {
		if (!this.children.containsKey(n)) {
			this.children.put(n, new HashSet<>());
		}
		this.children.get(n).add(gi);
	}

	public boolean equals(Object other) {
		if (!(other instanceof GradualNode)) {
			return false;
		}
		GradualNode n = (GradualNode) other;

		if (this.children.size() != n.children.size()) {
			return false;
		}

		// TODO: this can be optimized
		for (Map.Entry<GradualNode, Set<GradualItem>> e1 : this.children.entrySet()) {
			GradualNode n1 = e1.getKey();
			Set<GradualItem> is1 = e1.getValue();

			boolean found = false;
			for (Map.Entry<GradualNode, Set<GradualItem>> e2 : n.children.entrySet()) {
				GradualNode n2 = e2.getKey();
				Set<GradualItem> is2 = e2.getValue();

				if (is1.equals(is2) && n1.equals(n2)) {
					found = true;
					break;
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

		String s = this.hashCode()+"{\n";
		for (Map.Entry<GradualNode, Set<GradualItem>> e : this.children.entrySet()) {
			GradualNode n = e.getKey();

			s += subIndent;
			for (GradualItem gi : e.getValue()) {
				if (gi != null) {
					s += gi.toString();
				} else {
					s += "null";
				}
				s += " ";
			}

			s += n.toStringWithIndent(subIndent) + "\n";
		}
		s += indent+"}";
		return s;
	}

	public String toString() {
		return this.toStringWithIndent("");
	}
}

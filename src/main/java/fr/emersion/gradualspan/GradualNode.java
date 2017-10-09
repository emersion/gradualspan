package fr.emersion.gradualspan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// TODO: create a DoubleEndedGradualNode subclass, remove parents from this one
public class GradualNode {
	protected Set<GradualNode> parents = Collections.newSetFromMap(new IdentityHashMap<>());
	protected Map<GradualNode, Set<GradualItem>> children = new IdentityHashMap<>();
	private String name = null;

	public GradualNode() {}

	public GradualNode(String name) {
		this.name = name;
	}

	public Map<GradualNode, Set<GradualItem>> children() {
		return Collections.unmodifiableMap(this.children);
	}

	public void putChild(GradualItem gi, GradualNode child) {
		if (this == child) {
			throw new RuntimeException("I cannot be my own child");
		}

		if (!this.children.containsKey(child)) {
			this.children.put(child, new HashSet<>());
			child.parents.add(this);
		}

		this.children.get(child).add(gi);
	}

	protected void removeChild(GradualItem gi, GradualNode child) {
		if (!this.children.containsKey(child)) {
			throw new RuntimeException("Attempting to remove an non-existing child");
		}

		Set<GradualItem> items = this.children.get(child);
		items.remove(gi);

		if (items.size() == 0) {
			this.children.remove(child);
			child.parents.remove(this);
		}
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

	public String name() {
		return this.name;
	}

	private String toStringWithIndent(String indent) {
		if (this.children.size() == 0) {
			return this.hashCode()+"{}";
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

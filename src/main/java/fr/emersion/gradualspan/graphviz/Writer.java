package fr.emersion.gradualspan.graphviz;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import fr.emersion.gradualspan.GradualItem;
import fr.emersion.gradualspan.GradualNode;
import fr.emersion.gradualspan.GradualOrder;
import fr.emersion.gradualspan.GradualSequence;

public class Writer {
	private PrintWriter w;
	private int n = 0;

	public Writer(OutputStream out) {
		this.w = new PrintWriter(out);
	}

	public void write(GradualSequence s) {
		this.w.printf("digraph sequence%d {\n", this.n);
		this.n++;

		this.w.printf("graph [pad=\"0.5\", nodesep=\"2\", ranksep=\"2\"];\n");

		this.writeNode(s.begin, new IdentityHashMap<>());

		this.w.printf("}\n");
		this.w.flush();
	}

	private void writeNode(GradualNode n, Map<GradualNode, String> visited) {
		String nodeId = this.nodeId(n, visited);

		for (Map.Entry<GradualNode, Set<GradualItem>> e : n.children().entrySet()) {
			GradualNode child = e.getKey();
			Set<GradualItem> items = e.getValue();
			boolean childVisited = visited.containsKey(child);
			String childId = this.nodeId(child, visited);

			for (GradualItem item : items) {
				if (item == null) {
					this.w.printf("	%s -> %s;\n", nodeId, childId);
				} else {
					String color = "black";
					if (item.order == GradualOrder.LOWER) {
						color = "red";
					} else if (item.order == GradualOrder.GREATER) {
						color = "blue";
					}
					this.w.printf("	%s -> %s [ label=\"%s\" color=\"%s\" ];\n", nodeId, childId, item, color);
				}
			}

			if (!childVisited) {
				this.writeNode(child, visited);
			}
		}
	}

	private String nodeId(GradualNode n, Map<GradualNode, String> visited) {
		if (!visited.containsKey(n)) {
			visited.put(n, Integer.toString(visited.size()));
		}
		return visited.get(n);
	}
}

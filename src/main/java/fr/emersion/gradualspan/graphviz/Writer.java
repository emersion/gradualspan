package fr.emersion.gradualspan.graphviz;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import fr.emersion.gradualspan.ValuedItem;
import fr.emersion.gradualspan.ValuedNode;
import fr.emersion.gradualspan.ValuedSequence;
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

	public void write(ValuedSequence s) {
		this.w.printf("digraph valuedsequence%d {\n", this.n);
		this.n++;

		//this.w.printf("\tgraph [nodesep=\"0.5\", ranksep=\"2\", rankdir=\"LR\"];\n");

		Map<ValuedNode, String> visited = new HashMap<>();
		for (ValuedNode is : s.root()) {
			this.writeValuedNode(is, visited);
		}

		this.w.printf("}\n");
		this.w.flush();
	}

	public void writeValuedNode(ValuedNode is, Map<ValuedNode, String> visited) {
		String nodeId = this.nodeId(is, visited);

		//String label = "";
		//for (ValuedItem i : is) {
		//	label += i+"\\n";
		//}
		this.w.printf("\t%s [label=\"%s\"];\n", nodeId, is.name());

		for (ValuedNode child : is.children()) {
			boolean childVisited = visited.containsKey(child);
			String childId = this.nodeId(child, visited);
			this.w.printf("\t%s -> %s\n", nodeId, childId);

			if (!childVisited) {
				this.writeValuedNode(child, visited);
			}
		}
	}

	public void write(GradualSequence s) {
		this.w.printf("digraph gradualsequence%d {\n", this.n);
		this.n++;

		this.w.printf("\tgraph [nodesep=\"0.5\", ranksep=\"2\", rankdir=\"LR\"];\n");

		this.writeGradualNode(s.begin, new IdentityHashMap<>());

		this.w.printf("}\n");
		this.w.flush();
	}

	private void writeGradualNode(GradualNode n, Map<GradualNode, String> visited) {
		String nodeId = this.nodeId(n, visited);

		for (Map.Entry<GradualNode, Set<GradualItem>> e : n.children().entrySet()) {
			GradualNode child = e.getKey();
			Set<GradualItem> items = e.getValue();
			boolean childVisited = visited.containsKey(child);
			String childId = this.nodeId(child, visited);

			for (GradualItem item : items) {
				if (item == null) {
					this.w.printf("\t%s -> %s;\n", nodeId, childId);
				} else {
					String color = "black";
					if (item.order == GradualOrder.LOWER) {
						color = "red";
					} else if (item.order == GradualOrder.GREATER) {
						color = "blue";
					}
					this.w.printf("\t%s -> %s [label=\"%s\" color=\"%s\"];\n", nodeId, childId, item, color);
				}
			}

			if (!childVisited) {
				this.writeGradualNode(child, visited);
			}
		}
	}

	private <T> String nodeId(T n, Map<T, String> visited) {
		if (!visited.containsKey(n)) {
			visited.put(n, Integer.toString(visited.size()));
		}
		return visited.get(n);
	}
}

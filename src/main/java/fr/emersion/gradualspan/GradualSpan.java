package fr.emersion.gradualspan;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

public class GradualSpan {
	public static Collection<GradualPattern> gradualSpan(Iterable<ValuedSequence> dbv, int minSupport, GradualSupport support) {
		// Transform the po valued sequence database into a po gradual sequence database
		System.out.println("Running valuedToGradual...");
		List<GradualSequence> dbg = new ArrayList<>();
		for (ValuedSequence vs : dbv) {
			dbg.add(valuedToGradual(vs));
		}

		// Mine patterns
		System.out.println("Running forwardTreeMining...");
		Collection<GradualPattern> patterns = forwardTreeMining(dbg, minSupport, support);

		// Cleanup and prune patterns
		System.out.println("Running mergingSuffixTree...");
		for (GradualPattern pattern : patterns) {
			mergingSuffixTree(pattern);
		}

		return patterns;
	}

	public static GradualSequence valuedToGradual(ValuedSequence vs) {
		// Take each po valued sequence root, transform it and all its children into
		// gradual nodes
		GradualSequence gs = new GradualSequence();
		Map<ValuedNode, GradualNode> visited = new HashMap<>();
		for (ValuedNode vis : vs.root()) {
			GradualNode gnChild = valuedNodeToGradual(vis, gs.begin, new HashMap<>(), visited);
			gs.begin.putChild(null, gnChild);
		}
		gs.close();
		return gs;
	}

	private static GradualNode valuedNodeToGradual(ValuedNode vis, GradualNode prev, Map<Object, Pair<ValuedItem, GradualNode>> last, Map<ValuedNode, GradualNode> visited) {
		// Make sure to create exactly one gradual node per valued node
		GradualNode gn;
		if (visited.containsKey(vis)) {
			gn = visited.get(vis);
		} else {
			gn = new GradualNode();
			visited.put(vis, gn);
		}

		// For each item, check if we've seen it before. If so, draw an arrow from
		// the previous node to this one.
		for (ValuedItem vi : vis) {
			if (last.containsKey(vi.item())) {
				Pair<ValuedItem, GradualNode> pair = last.get(vi.item());
				ValuedItem lastItem = pair.first;
				GradualNode lastNode = pair.second;

				if (gn == lastNode) {
					continue; // Duplicate attribute
				}

				GradualItem gi = lastItem.toGradual(vi);
				lastNode.putChild(gi, gn);
			}
			last.put(vi.item(), new Pair<>(vi, gn));
		}

		// Create gradual nodes for children, draw empty arrows from this node to
		// its children.
		for (ValuedNode child : vis.children()) {
			GradualNode gnChild = valuedNodeToGradual(child, gn, copyMap(last), visited);
			gn.putChild(null, gnChild);
		}

		return gn;
	}

	private static<K, V> Map<K, V> copyMap(Map<K, V> m) {
		// TODO: use a more efficient data structure
		return new HashMap<>(m);
	}

	public static Collection<GradualPattern> forwardTreeMining(Collection<GradualSequence> db, int minSupport, GradualSupport support) {
		return forwardTreeMining(db, minSupport, support, new HashSet<>());
	}

	private static Collection<GradualPattern> forwardTreeMining(Collection<GradualSequence> db, int minSupport, GradualSupport support, Set<Set<GradualSequence>> mined) {
		Collection<GradualPattern> result = new ArrayList<>();
		Map<GradualNode, Collection<GradualSequence>> projected = new IdentityHashMap<>();

		// In this call, we will mine a pattern with a support of exactly
		// upperSupport.
		int upperSupport = support.size(db);

		GradualPattern pattern = new GradualPattern();
		pattern.support = upperSupport;
		result.add(pattern);
		projected.put(pattern.begin, db);

		Queue<GradualNode> nodeQueue = new LinkedList<>();
		nodeQueue.add(pattern.begin);

		while (true) {
			GradualNode node = nodeQueue.poll();
			if (node == null) {
				break;
			}

			// List item occurences in this node's projected DB
			Map<GradualItem, GradualSupport.Occurence> occList = support.listOccurences(projected.get(node), minSupport);
			projected.remove(node); // We won't need that anymore

			for (Map.Entry<GradualItem, GradualSupport.Occurence> e : occList.entrySet()) {
				GradualItem item = e.getKey();
				GradualSupport.Occurence occ = e.getValue();

				// If the occurence has a high enough support to be included in this
				// pattern without decreasing its support, do it. Otherwise, call
				// ourselves recursively to mine a pattern having a lower support.
				if (occ.support == upperSupport) {
					// Create a new node, add it to the pattern, and add the new node to
					// the queue
					GradualNode newNode = new GradualNode();
					projected.put(newNode, occ.projected);
					node.putChild(item, newNode);
					nodeQueue.add(newNode);
				} else {
					// Get the subset of the database that supports the item
					Collection<GradualSequence> subDB = support.cover(db, item);
					Set<GradualSequence> subSet = new HashSet<>(subDB);
					if (mined.contains(subSet)) {
						continue; // Already mined
					}
					mined.add(subSet);

					// Mine the pattern
					Collection<GradualPattern> subResult = forwardTreeMining(subDB, minSupport, support, mined);
					result.addAll(subResult);
				}
			}
		}

		pattern.close();

		return result;
	}

	public static void mergingSuffixTree(GradualSequence seq) {
		mergingSuffixNode(seq.end);
	}

	private static void mergingSuffixNode(GradualNode node) {
		// STEP 1: remove redundant non-empty arrows pointing to node
		// That replaces them with empty arrows, that are cleaned in the next step

		// TODO: group multiple GradualItems and multiple GradualNodes?

		// Group parents by arrow label
		Map<GradualItem, Set<GradualNode>> parentsByItem = new HashMap<>();
		for (GradualNode parent : node.parents) {
			// Get all arrows from parent to node
			Set<GradualItem> items = parent.children.get(node);
			for (GradualItem gi : items) {
				if (!parentsByItem.containsKey(gi)) {
					parentsByItem.put(gi, Collections.newSetFromMap(new IdentityHashMap<>()));
				}
				parentsByItem.get(gi).add(parent);
			}
		}

		// Replace redundant arrows by empty arrows
		for (Map.Entry<GradualItem, Set<GradualNode>> e : parentsByItem.entrySet()) {
			GradualItem gi = e.getKey();
			Set<GradualNode> parents = e.getValue();
			if (gi == null) {
				continue;
			}
			if (parents.size() <= 1) {
				continue;
			}

			GradualNode newNode = new GradualNode();
			for (GradualNode parent : parents) {
				parent.removeChild(gi, node);
				parent.putChild(null, newNode);
			}

			newNode.putChild(gi, node);
		}

		// STEP 2: remove useless empty arrows

		// Check if this node can be merged with a parent (that deletes this node)
		// This is possible if this node has no other parent
		boolean canMergeNode = node.parents.size() <= 1;
		GradualNode mergeNodeWith = null;

		// List parents that can be merged with this node (that deletes parents)
		// This is possible if:
		// 1. There is only an empty arrow from the parent to this node
		// 2. The parent only has this node as child
		Set<GradualNode> parentsToMerge = Collections.newSetFromMap(new IdentityHashMap<>());
		for (GradualNode parent : new ArrayList<>(node.parents)) {
			Set<GradualItem> items = parent.children.get(node);

			// Check if there is an empty arrow from parent to this node that can
			// potentially be removed
			if (!items.contains(null)) {
				continue;
			}

			if (items.size() == 1) {
				// There is only an empty arrow from parent to this node
				if (parent.children.size() == 1) {
					// The parent only has this node as child
					parentsToMerge.add(parent);
				} else if (canMergeNode) {
					// This node can be merged with its only parent
					mergeNodeWith = parent;
				} else {
					// Unfortunately, the empty arrow cannot be removed
					// Do nothing
				}
			} else {
				// There is also a non-empty arrow from parent to node
				// The empty arrow is useless
				parent.removeChild(null, node);
			}
		}

		// Merge this node with a parent
		if (mergeNodeWith != null) {
			for (Map.Entry<GradualNode, Set<GradualItem>> e : new ArrayList<>(node.children.entrySet())) {
				GradualNode child = e.getKey();
				Set<GradualItem> items = e.getValue();

				for (GradualItem gi : new ArrayList<>(items)) {
					node.removeChild(gi, child);
					mergeNodeWith.putChild(gi, child);
				}
			}

			// Remove the useless empty arrow
			mergeNodeWith.removeChild(null, node);
		}

		// Merge parents with this node
		for (GradualNode parent : parentsToMerge) {
			// We want to delete parent
			// We know that there is only a useless empty arrow from parent to this
			// node
			// We can discard parent.children and copy parent.parents to this node
			for (GradualNode grandParent : new ArrayList<>(parent.parents)) {
				Set<GradualItem> items = grandParent.children.get(parent);
				for (GradualItem gi : new ArrayList<>(items)) {
					grandParent.removeChild(gi, parent);
					grandParent.putChild(gi, node);
				}
			}

			// Remove the useless empty arrow
			parent.removeChild(null, node);
		}

		// STEP 3: recursive call

		// If some parents were merged, this node has now new parents
		// If this node was merged, this node doesn't exist anymore
		if (parentsToMerge.size() > 0) {
			// Apply mergingSuffixNode one more time
			mergingSuffixNode(node);
		} else if (mergeNodeWith != null) {
			// Apply mergingSuffixTree on this node's only former parent
			mergingSuffixNode(mergeNodeWith);
		} else {
			// Apply mergingSuffixNode on parents
			for (GradualNode parent : new ArrayList<>(node.parents)) {
				mergingSuffixNode(parent);
			}
		}
	}
}

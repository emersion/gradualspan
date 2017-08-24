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
	public static Collection<GradualSequence> gradualSpan(Iterable<ValuedSequence> dbv, int minSupport, GradualSupport support) {
		List<GradualSequence> dbg = new ArrayList<>();
		for (ValuedSequence vs : dbv) {
			dbg.add(valuedToGradual(vs));
		}

		Collection<GradualSequence> patterns = forwardTreeMining(dbg, minSupport, support);

		//for (GradualSequence pattern : patterns) {
		//	mergingSuffixTree(pattern);
		//}

		return patterns;
	}

	public static GradualSequence valuedToGradual(ValuedSequence vs) {
		GradualSequence gs = new GradualSequence();
		for (ValuedItemset vis : vs.root()) {
			GradualNode gnChild = valuedNodeToGradual(vis, gs.begin, new HashMap<>());
			gs.begin.putChild(null, gnChild);
		}
		gs.close();
		return gs;
	}

	private static GradualNode valuedNodeToGradual(ValuedItemset vis, GradualNode prev, Map<Object, Pair<ValuedItem, GradualNode>> last) {
		GradualNode gn = new GradualNode();

		for (ValuedItem vi : vis) {
			if (last.containsKey(vi.item())) {
				Pair<ValuedItem, GradualNode> pair = last.get(vi.item());
				if (gn == pair.second) {
					continue; // Duplicate attribute
				}
				GradualItem gi = pair.first.toGradual(vi);
				pair.second.putChild(gi, gn);
			}
			last.put(vi.item(), new Pair<>(vi, gn));
		}

		for (ValuedItemset child : vis.children()) {
			GradualNode gnChild = valuedNodeToGradual(child, gn, copyMap(last));
			gn.putChild(null, gnChild);
		}

		return gn;
	}

	private static<K, V> Map<K, V> copyMap(Map<K, V> m) {
		// TODO: use a more efficient data structure
		return new HashMap<>(m);
	}

	public static Collection<GradualSequence> forwardTreeMining(Collection<GradualSequence> db, int minSupport, GradualSupport support) {
		return forwardTreeMining(db, minSupport, support, new HashSet<>());
	}

	private static Collection<GradualSequence> forwardTreeMining(Collection<GradualSequence> db, int minSupport, GradualSupport support, Set<Set<GradualSequence>> mined) {
		Collection<GradualSequence> result = new ArrayList<>();
		Map<GradualNode, Collection<GradualSequence>> projected = new IdentityHashMap<>();

		GradualSequence pattern = new GradualSequence();
		result.add(pattern);
		projected.put(pattern.begin, db);

		int upperSupport = support.size(db);

		Queue<GradualNode> nodeQueue = new LinkedList<>();
		nodeQueue.add(pattern.begin);

		while (true) {
			GradualNode node = nodeQueue.poll();
			if (node == null) {
				break;
			}

			Map<GradualItem, GradualSupport.Occurence> occList = support.listOccurences(projected.get(node), minSupport);
			projected.remove(node);

			for (Map.Entry<GradualItem, GradualSupport.Occurence> e : occList.entrySet()) {
				if (e.getValue().support != upperSupport) {
					continue;
				}

				GradualNode newNode = new GradualNode();
				projected.put(newNode, e.getValue().projected);
				node.putChild(e.getKey(), newNode);
				nodeQueue.add(newNode);
			}

			for (Map.Entry<GradualItem, GradualSupport.Occurence> e : occList.entrySet()) {
				if (e.getValue().support == upperSupport) {
					continue;
				}

				Collection<GradualSequence> subDB = support.cover(db, e.getKey());
				Set<GradualSequence> subSet = new HashSet<>(subDB);
				if (mined.contains(subSet)) {
					continue; // Already mined
				}
				mined.add(subSet);

				Collection<GradualSequence> subResult = forwardTreeMining(subDB, minSupport, support, mined);
				result.addAll(subResult);
			}
		}

		pattern.close();

		return result;
	}

	public static void mergingSuffixTree(GradualSequence seq) {
		mergingSuffixNode(seq.end);
	}

	private static void mergingSuffixNode(GradualNode node) {
		// STEP 1: remove redundant arrows pointing to node

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
				} else {
					// TODO: try to merge this node with parent
				}
			} else {
				// There is also a non-empty arrow from parent to node
				// The empty arrow is useless
				parent.removeChild(null, node);
			}
		}

		// Merge this node with a parent
		// TODO

		// Merge parents with this node
		for (GradualNode parent : parentsToMerge) {
			// We want to delete parent
			// We know that there is only a useless empty arrow from parent to this
			// node
			// We can discard parent.children and copy parent.parents to this node
			for (GradualNode grandParent : new ArrayList<>(parent.parents)) {
				Set<GradualItem> items = grandParent.children.get(parent);
				for (GradualItem gi : items) {
					grandParent.removeChild(gi, parent);
					grandParent.putChild(gi, node);
				}
			}

			// Remove the useless empty arrow
			parent.removeChild(null, node);
		}

		// STEP 3: recursive call

		// If some parents were merged, this node has now new parents
		if (parentsToMerge.size() > 0) {
			// Apply mergingSuffixNode one more time
			mergingSuffixNode(node);
		} else {
			// Apply mergingSuffixNode on parents
			for (GradualNode parent : new ArrayList<>(node.parents)) {
				mergingSuffixNode(parent);
			}
		}
	}
}

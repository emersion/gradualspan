package fr.emersion.gradualspan;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

public class GradualSpan {
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
		//System.out.println(mapToString(last));

		for (ValuedItem vi : vis) {
			if (last.containsKey(vi.item())) {
				Pair<ValuedItem, GradualNode> pair = last.get(vi.item());
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

	private static<K, V> String mapToString(Map<K, V> m) {
		String s = "{";
		for (K k : m.keySet()) {
			s += k.toString()+": "+m.get(k).toString()+", ";
		}
		s += "}";
		return s;
	}

	public static Collection<GradualSequence> forwardTreeMining(Collection<GradualSequence> db, int minSupport, GradualSupport support) {
		return forwardTreeMining(db, minSupport, support, new HashSet<>());
	}

	private static Collection<GradualSequence> forwardTreeMining(Collection<GradualSequence> db, int minSupport, GradualSupport support, Set<Set<GradualSequence>> mined) {
		Collection<GradualSequence> result = new ArrayList<>();
		Map<GradualNode, Collection<GradualSequence>> projected = new HashMap<>();

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
		// Group predecessors by arrow label
		Map<GradualItem, Set<GradualNode>> predecessors = new HashMap<>();
		for (GradualNode parent : node.parents) {
			Set<GradualItem> items = parent.children.get(node);
			for (GradualItem gi : items) {
				if (!predecessors.containsKey(gi)) {
					predecessors.put(gi, new HashSet<>());
				}
				predecessors.get(gi).add(parent);
			}
		}

		// Split each group in subgroups of unordered nodes
		for (Map.Entry<GradualItem, Set<GradualNode>> e : predecessors.entrySet()) {
			GradualItem item = e.getKey();
			Set<GradualNode> nodes = e.getValue();

			List<Set<GradualNode>> subGroups = new ArrayList<>();
			for (GradualNode n : nodes) {
				boolean added = false;
				for (Set<GradualNode> subGroup : subGroups) {
					if (isUnorderedNode(subGroup, n)) {
						subGroup.add(n);
						added = true;
						break;
					}
				}

				if (!added) {
					Set<GradualNode> subGroup = new HashSet<>();
					subGroup.add(n);
					subGroups.add(subGroup);
				}
			}

			// Merge nodes in sub-groups
			for (Set<GradualNode> subGroup : subGroups) {
				GradualNode merged = mergeNodes(subGroup);
				merged.putChild(item, node);

				// Add children to merged node
				for (GradualNode n : subGroup) {
					for (Map.Entry<GradualNode, Set<GradualItem>> childEntry : n.children.entrySet()) {
						GradualNode child = childEntry.getKey();
						Set<GradualItem> childItems = childEntry.getValue();

						for (GradualItem gi : childItems) {
							if (Objects.equals(gi, item)) {
								continue;
							}

							merged.putChild(gi, child);
						}
					}
				}
			}
		}

		// Recursive call on parents
		for (GradualNode parent : new ArrayList<>(node.parents)) {
			mergingSuffixNode(parent);
		}
	}

	private static boolean isUnorderedNode(Set<GradualNode> set, GradualNode node) {
		return !hasChild(set, node) && !hasParent(set, node);
	}

	private static boolean hasChild(Set<GradualNode> set, GradualNode node) {
		for (GradualNode child : node.children.keySet()) {
			if (set.contains(child) || hasChild(set, child)) {
				return true;
			}
		}
		return false;
	}

	private static boolean hasParent(Set<GradualNode> set, GradualNode node) {
		for (GradualNode parent : node.parents) {
			if (set.contains(parent) || hasParent(set, parent)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Creates a new merged node from nodes such that all parents of each node
	 * points to the new merged node.
	 */
	private static GradualNode mergeNodes(Set<GradualNode> nodes) {
		GradualNode merged = new GradualNode();

		for (GradualNode n : nodes) {
			for (GradualNode p : n.parents) {
				Set<GradualItem> items = p.children.get(n);
				p.children.remove(n);

				for (GradualItem item : items) {
					p.putChild(item, merged);
				}
			}
			n.parents.clear();
		}

		return merged;
	}
}

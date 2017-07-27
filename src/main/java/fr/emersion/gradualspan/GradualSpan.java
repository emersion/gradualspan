package fr.emersion.gradualspan;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.Queue;
import java.util.HashSet;
import java.util.LinkedList;

public class GradualSpan {
	public static GradualNode valuedToGradual(ValuedSequence vs) {
		GradualNode gn = new GradualNode();
		for (ValuedItemset vis : vs.root()) {
			GradualNode gnChild = valuedNodeToGradual(vis, gn, new HashMap<>());
			gn.putChild(null, gnChild);
		}
		return gn;
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

	public static List<GradualNode> forwardTreeMining(List<GradualNode> db, int minSupport) {
		return forwardTreeMining(db, minSupport, new HashSet<>());
	}

	private static List<GradualNode> forwardTreeMining(List<GradualNode> db, int minSupport, Set<Set<GradualNode>> mined) {
		List<GradualNode> result = new ArrayList<>();
		Map<GradualNode, List<GradualNode>> projected = new HashMap<>();

		GradualNode pattern = new GradualNode();
		result.add(pattern);
		projected.put(pattern, db);

		int upperSupport = db.size(); // TODO: support mode

		Queue<GradualNode> nodeQueue = new LinkedList<>();
		nodeQueue.add(pattern);

		while (true) {
			GradualNode node = nodeQueue.poll();
			if (node == null) {
				break;
			}

			Map<GradualItem, Occurence> occList = listOccurences(projected.get(node), minSupport); // TODO: support mode

			for (Map.Entry<GradualItem, Occurence> e : occList.entrySet()) {
				if (e.getValue().support != upperSupport) {
					continue;
				}

				GradualNode newNode = new GradualNode();
				projected.put(newNode, e.getValue().projected);
				node.putChild(e.getKey(), newNode);
				nodeQueue.add(newNode);
			}

			for (Map.Entry<GradualItem, Occurence> e : occList.entrySet()) {
				if (e.getValue().support == upperSupport) {
					continue;
				}

				List<GradualNode> subDB = cover(db, e.getKey()); // TODO: support mode
				Set<GradualNode> subSet = new HashSet<>(subDB);
				if (mined.contains(subSet)) {
					continue; // Already mined
				}
				mined.add(subSet);

				List<GradualNode> subResult = forwardTreeMining(subDB, minSupport, mined);
				result.addAll(subResult);
			}
		}

		return result;
	}

	public static class Occurence {
		public int support = 0;
		public final List<GradualNode> projected = new ArrayList<>();

		public boolean equals(Object other) {
			if (!(other instanceof Occurence)) {
				return false;
			}
			Occurence occ = (Occurence) other;
			return this.support == occ.support && this.projected.equals(occ.projected);
		}
	}

	// Support mode: sequence

	public static Map<GradualItem, Occurence> listOccurences(List<GradualNode> db, int minSupport) {
		Map<GradualItem, Occurence> occurences = new HashMap<>();
		for (GradualNode seq : db) {
			listSequenceOccurences(seq, occurences, new HashSet<>());
		}

		// Two passes are needed to prevent java.util.ConcurrentModificationException
		List<GradualItem> removed = new ArrayList<>();
		for (Map.Entry<GradualItem, Occurence> e : occurences.entrySet()) {
			if (e.getValue().support < minSupport) {
				removed.add(e.getKey());
			}
		}
		for (GradualItem k : removed) {
			occurences.remove(k);
		}

		return occurences;
	}

	private static void listSequenceOccurences(GradualNode n, Map<GradualItem, Occurence> occurences, Set<GradualItem> visited) {
		for (Map.Entry<GradualNode, Set<GradualItem>> e : n.children.entrySet()) {
			GradualNode child = e.getKey();
			Set<GradualItem> is = e.getValue();

			for (GradualItem i : is) {
				if (i != null) {
					// Only count one occurence per sequence
					if (!visited.contains(i)) {
						Occurence occ = occurences.get(i);
						if (occ == null) {
							occ = new Occurence();
							occurences.put(i, occ);
						}
						occ.support++;
						occ.projected.add(child);
						visited.add(i);
					}
				}
			}
			listSequenceOccurences(child, occurences, visited);
		}
	}

	public static List<GradualNode> cover(List<GradualNode> db, GradualItem item) {
		List<GradualNode> subDB = new ArrayList<>();
		for (GradualNode n : db) {
			if (itemInSequence(n, item)) {
				subDB.add(n);
			}
		}
		return subDB;
	}

	private static boolean itemInSequence(GradualNode n, GradualItem item) {
		for (Map.Entry<GradualNode, Set<GradualItem>> e : n.children.entrySet()) {
			GradualNode child = e.getKey();
			Set<GradualItem> is = e.getValue();

			if (is.contains(item)) {
				return true;
			}
			if (itemInSequence(child, item)) {
				return true;
			}
		}
		return false;
	}

	// Support mode: path

	/*public static Map<GradualItem, Integer> listOccurences(List<GradualNode> db, int minSupport) {
		Map<GradualItem, Integer> occurences = new HashMap<>();
		for (GradualNode seq : db) {
			listSequenceOccurences(seq, occurences, new ArrayList<>());
		}

		// Two passes are needed to prevent java.util.ConcurrentModificationException
		List<GradualItem> removed = new ArrayList<>();
		for (Map.Entry<GradualItem, Integer> occ : occurences.entrySet()) {
			if (occ.getValue() < minSupport) {
				removed.add(occ.getKey());
			}
		}
		for (GradualItem k : removed) {
			occurences.remove(k);
		}

		return occurences;
	}

	private static void listSequenceOccurences(GradualNode n, Map<GradualItem, Integer> occurences, List<GradualItem> path) {
		// TODO: stop at first match!

		if (n.children.size() == 0) {
			for (GradualItem step : path) {
				if (!occurences.containsKey(step)) {
					occurences.put(step, 0);
				}
				occurences.put(step, occurences.get(step) + 1);
			}
			return;
		}

		for (Map.Entry<GradualNode, Set<GradualItem>> e : n.children.entrySet()) {
			GradualNode child = e.getKey();
			Set<GradualItem> is = e.getValue();

			List<GradualItem> subPath = copyList(path);
			for (GradualItem i : is) {
				if (i != null) {
					subPath.add(i);
				}
			}
			listSequenceOccurences(child, occurences, subPath);
		}
	}*/

	private static<E> List<E> copyList(List<E> list) {
		// TODO: use a more efficient data structure
		return new ArrayList<>(list);
	}
}

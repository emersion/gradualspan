package fr.emersion.gradualspan;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

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
		return null; // TODO
	}

	public static Map<GradualItem, Integer> listOccurences(List<GradualNode> db, int minSupport) {
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
	}

	private static<E> List<E> copyList(List<E> list) {
		// TODO: use a more efficient data structure
		return new ArrayList<>(list);
	}
}

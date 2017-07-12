package fr.emersion.gradualspan;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

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

	/*private static Map<GradualItem, Integer> listOccurences(List<GradualNode> db) {
		Map<GradualItem, Integer> occurences = new HashMap<>();
		for (GradualNode seq : db) {
			listSequenceOccurences(seq, occurences, new ArrayList<>());
		}
		return occurences;
	}

	private static void listSequenceOccurences(GradualNode n, Map<GradualItem, Integer> occurences, List<GradualItem> path) {
		if (n.children.size() == 0) {
			for (GradualNode step : path) {
				if (!occurences.containsKey(step)) {
					occurences.put(step, 0);
				}
				occurences.put(step, occurences.get(step) + 1);
			}
			return;
		}

		for (GradualNode.Arrow a : n.children) {
			List<GradualItem> subPath = path;
			if (a.gi != null) {
				subPath = copyList(path);
				subPath.add(a.gi);
			}
			listSequenceOccurences(a.target, );
		}
	}

	private static<E> List<E> copyList(List<E> list) {
		// TODO: use a more efficient data structure
		return new ArrayList<>(list);
	}*/
}

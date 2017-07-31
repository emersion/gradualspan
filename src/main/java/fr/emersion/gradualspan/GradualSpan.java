package fr.emersion.gradualspan;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
		// TODO
	}
}

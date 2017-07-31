package fr.emersion.gradualspan;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface GradualSupport {
	public static class Occurence {
		public int support = 0;
		public final Collection<GradualSequence> projected = new ArrayList<>();

		public boolean equals(Object other) {
			if (!(other instanceof Occurence)) {
				return false;
			}
			Occurence occ = (Occurence) other;
			return this.support == occ.support && this.projected.equals(occ.projected);
		}
	}

	public int size(Collection<GradualSequence> db);
	public Map<GradualItem, Occurence> listOccurences(Collection<GradualSequence> db, int minSupport);
	public Collection<GradualSequence> cover(Collection<GradualSequence> db, GradualItem item);

	public static class BySequence implements GradualSupport {
		public int size(Collection<GradualSequence> db) {
			return db.size();
		}

		public Map<GradualItem, Occurence> listOccurences(Collection<GradualSequence> db, int minSupport) {
			Map<GradualItem, Occurence> occurences = new HashMap<>();
			for (GradualSequence seq : db) {
				listSequenceOccurences(seq.begin, occurences, new HashSet<>());
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

		private void listSequenceOccurences(GradualNode n, Map<GradualItem, Occurence> occurences, Set<GradualItem> visited) {
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
							occ.projected.add(new GradualSequence(child));
							visited.add(i);
						}
					}
				}
				listSequenceOccurences(child, occurences, visited);
			}
		}

		public Collection<GradualSequence> cover(Collection<GradualSequence> db, GradualItem item) {
			Collection<GradualSequence> subDB = new ArrayList<>();
			for (GradualSequence seq : db) {
				if (itemInSequence(seq.begin, item)) {
					subDB.add(seq);
				}
			}
			return subDB;
		}

		private boolean itemInSequence(GradualNode n, GradualItem item) {
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
	}

	/*public static class ByPath implements GradualSupport {
		public static Map<GradualItem, Integer> listOccurences(Collection<GradualNode> db, int minSupport) {
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
		}

		private <E> List<E> copyList(List<E> list) {
			// TODO: use a more efficient data structure
			return new ArrayList<>(list);
		}
	}*/
}

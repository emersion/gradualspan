package fr.emersion.jenaplayground;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import fr.emersion.gradualspan.*;

public class GradualSpanTest extends TestCase {
	private Object a = "a";
	private Object b = "b";
	private Object c = "c";

	public GradualSpanTest(String testName) {
		super(testName);
	}

	public static Test suite() {
		return new TestSuite(GradualSpanTest.class);
	}

	private ValuedSequence valuedSequence() {
		ValuedSequence vs = new ValuedSequence();

		ValuedItemset vis1_1 = new ValuedItemset();
		vis1_1.add(new ValuedItem(a, 10));
		vs.add(vis1_1);

		ValuedItemset vis2_1 = new ValuedItemset();
		vis2_1.add(new ValuedItem(a, 15));
		vis1_1.children.add(vis2_1);

		ValuedItemset vis1_2 = new ValuedItemset();
		vis1_2.add(new ValuedItem(b, 15));
		vis1_2.add(new ValuedItem(c, 10));
		vs.add(vis1_2);

		ValuedItemset vis2_2 = new ValuedItemset();
		vis2_2.add(new ValuedItem(b, 10));
		vis1_2.children.add(vis2_2);

		ValuedItemset vis2_3 = new ValuedItemset();
		vis2_3.add(new ValuedItem(b, 15));
		vis2_3.add(new ValuedItem(c, 15));
		vis1_2.children.add(vis2_3);

		return vs;
	}

	private GradualNode gradualSequence() {
		GradualNode gn = new GradualNode();

		GradualNode gn1_1 = new GradualNode();
		gn.putChild(null, gn1_1);

		GradualNode gn1_2 = new GradualNode();
		gn.putChild(null, gn1_2);

		GradualNode gn2_1 = new GradualNode();
		gn1_1.putChild(null, gn2_1);
		gn1_1.putChild(new GradualItem(a, GradualOrder.GREATER), gn2_1);

		GradualNode gn2_2 = new GradualNode();
		gn1_2.putChild(null, gn2_2);
		gn1_2.putChild(new GradualItem(b, GradualOrder.LOWER), gn2_2);

		GradualNode gn2_3 = new GradualNode();
		gn1_2.putChild(null, gn2_3);
		gn1_2.putChild(new GradualItem(b, GradualOrder.EQUAL), gn2_3);
		gn1_2.putChild(new GradualItem(c, GradualOrder.GREATER), gn2_3);

		return gn;
	}

	private List<GradualNode> gradualDB() {
		List<GradualNode> db = new ArrayList<>();

		db.add(gradualSequence());

		GradualNode gn = new GradualNode();
		db.add(gn);

		GradualNode gn1_1 = new GradualNode();
		gn.putChild(null, gn1_1);

		GradualNode gn2_1 = new GradualNode();
		gn1_1.putChild(null, gn2_1);
		gn1_1.putChild(new GradualItem(a, GradualOrder.GREATER), gn2_1);

		return db;
	}

	private static class ValuedSequence extends ArrayList<fr.emersion.gradualspan.ValuedItemset> implements fr.emersion.gradualspan.ValuedSequence {
		public Iterable<fr.emersion.gradualspan.ValuedItemset> root() {
			return this;
		}

		public String toString() {
			String s = "";
			for (fr.emersion.gradualspan.ValuedItemset vis : this.root()) {
				s += vis.toString() + "\n";
			}
			return s;
		}
	}

	private static class ValuedItemset extends HashSet<fr.emersion.gradualspan.ValuedItem> implements fr.emersion.gradualspan.ValuedItemset {
		private ArrayList<fr.emersion.gradualspan.ValuedItemset> children = new ArrayList<>();

		public Iterable<fr.emersion.gradualspan.ValuedItemset> children() {
			return this.children;
		}

		public String toString() {
			String s = "";
			for (fr.emersion.gradualspan.ValuedItem vi : this) {
				s += vi.toString() + " ";
			}

			s += this.hashCode();

			if (this.children.size() == 0) {
				s += "{}";
				return s;
			}

			s += "{\n";
			for (fr.emersion.gradualspan.ValuedItemset vis : this.children) {
				s += vis.toString() + "\n";
			}
			s += "}";
			return s;
		}
	}

	private static class ValuedItem implements fr.emersion.gradualspan.ValuedItem {
		private final Object item;
		private final float value;

		public ValuedItem(Object item, float value) {
			this.item = item;
			this.value = value;
		}

		public Object item() {
			return this.item;
		}

		public GradualItem toGradual(fr.emersion.gradualspan.ValuedItem other) {
			float delta = ((ValuedItem) other).value - this.value;

			GradualOrder go = GradualOrder.EQUAL;
			if (delta != 0) {
				if (delta > 0) {
					go = GradualOrder.GREATER;
				} else {
					go = GradualOrder.LOWER;
				}
			}
			return new GradualItem(this.item, go);
		}

		public String toString() {
			return this.item.toString() + "=" + new Float(this.value).toString();
		}
	}

	public void testValuedToGradual() {
		ValuedSequence vs = valuedSequence();
		GradualNode expected = gradualSequence();
		//System.out.println(vs);
		GradualNode gs = GradualSpan.valuedToGradual(vs);
		//System.out.println(gs);
		//System.out.println(expected);
		assertEquals(expected, gs);
	}

	// Only checks support in equals
	// TODO: check projected too
	private static class Occurence extends GradualSpan.Occurence {
		public boolean equals(Object other) {
			if (!(other instanceof GradualSpan.Occurence)) {
				return false;
			}
			GradualSpan.Occurence occ = (GradualSpan.Occurence) other;
			return this.support == occ.support;
		}
	}

	public void testListOccurences() {
		List<GradualNode> db = gradualDB();
		int minSupport = 2;

		Map<GradualItem, GradualSpan.Occurence> occurences = GradualSpan.listOccurences(db, minSupport);
		//System.out.println(occurences);

		Map<GradualItem, GradualSpan.Occurence> expected = new HashMap<>();
		GradualSpan.Occurence occ = new Occurence();
		occ.support = 2;
		// TODO: occ.projected
		expected.put(new GradualItem(a, GradualOrder.GREATER), occ);

		assertEquals(expected, occurences);
	}
}

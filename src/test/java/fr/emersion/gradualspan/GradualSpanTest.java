package fr.emersion.jenaplayground;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

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

		ValuedNode vis1_1 = new ValuedNode();
		vis1_1.add(new ValuedItem(a, 10));
		vs.add(vis1_1);

		ValuedNode vis2_1 = new ValuedNode();
		vis2_1.add(new ValuedItem(a, 15));
		vis1_1.children.add(vis2_1);

		ValuedNode vis1_2 = new ValuedNode();
		vis1_2.add(new ValuedItem(b, 15));
		vis1_2.add(new ValuedItem(c, 10));
		vs.add(vis1_2);

		ValuedNode vis2_2 = new ValuedNode();
		vis2_2.add(new ValuedItem(b, 10));
		vis1_2.children.add(vis2_2);

		ValuedNode vis2_3 = new ValuedNode();
		vis2_3.add(new ValuedItem(b, 15));
		vis2_3.add(new ValuedItem(c, 15));
		vis1_2.children.add(vis2_3);

		return vs;
	}

	private GradualSequence gradualSequence() {
		GradualSequence gs = new GradualSequence();

		GradualNode gn1_1 = new GradualNode();
		gs.begin.putChild(null, gn1_1);

		GradualNode gn1_2 = new GradualNode();
		gs.begin.putChild(null, gn1_2);

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

		gs.close();

		return gs;
	}

	private Collection<GradualSequence> gradualDBSmall() {
		List<GradualSequence> db = new ArrayList<>();

		db.add(gradualSequence());

		GradualSequence gs = new GradualSequence();
		db.add(gs);

		GradualNode gn1_1 = new GradualNode();
		gs.begin.putChild(null, gn1_1);

		GradualNode gn2_1 = new GradualNode();
		gn1_1.putChild(null, gn2_1);
		gn1_1.putChild(new GradualItem(a, GradualOrder.GREATER), gn2_1);

		gs.close();

		return db;
	}

	private Collection<GradualSequence> gradualDB() {
		List<GradualSequence> db = new ArrayList<>();

		{
			GradualSequence gs = new GradualSequence();
			db.add(gs);

			GradualNode gn1_1 = new GradualNode();
			gs.begin.putChild(null, gn1_1);

			GradualNode gn2_1 = new GradualNode();
			gn1_1.putChild(null, gn2_1);
			gn1_1.putChild(new GradualItem(a, GradualOrder.GREATER), gn2_1);

			GradualNode gn3_1 = new GradualNode();
			gn2_1.putChild(null, gn3_1);
			gn2_1.putChild(new GradualItem(b, GradualOrder.GREATER), gn3_1);

			GradualNode gn3_2 = new GradualNode();
			gn2_1.putChild(null, gn3_2);
			gn2_1.putChild(new GradualItem(c, GradualOrder.LOWER), gn3_2);

			gs.close();
		}

		{
			GradualSequence gs = new GradualSequence();
			db.add(gs);

			GradualNode gn1_1 = new GradualNode();
			gs.begin.putChild(null, gn1_1);

			GradualNode gn2_1 = new GradualNode();
			gn1_1.putChild(null, gn2_1);
			gn1_1.putChild(new GradualItem(a, GradualOrder.GREATER), gn2_1);

			GradualNode gn3_1 = new GradualNode();
			gn2_1.putChild(null, gn3_1);
			gn2_1.putChild(new GradualItem(b, GradualOrder.GREATER), gn3_1);

			GradualNode gn3_2 = new GradualNode();
			gn2_1.putChild(null, gn3_2);
			gn2_1.putChild(new GradualItem(c, GradualOrder.EQUAL), gn3_2);

			gs.close();
		}

		{
			GradualSequence gs = new GradualSequence();
			db.add(gs);

			GradualNode gn1_1 = new GradualNode();
			gs.begin.putChild(null, gn1_1);

			GradualNode gn2_1 = new GradualNode();
			gn1_1.putChild(null, gn2_1);
			gn1_1.putChild(new GradualItem(a, GradualOrder.GREATER), gn2_1);

			gs.close();
		}

		return db;
	}

	private static class ValuedSequence extends ArrayList<fr.emersion.gradualspan.ValuedNode> implements fr.emersion.gradualspan.ValuedSequence {
		public Iterable<fr.emersion.gradualspan.ValuedNode> root() {
			return this;
		}

		public String toString() {
			String s = "";
			for (fr.emersion.gradualspan.ValuedNode vis : this.root()) {
				s += vis.toString() + "\n";
			}
			return s;
		}
	}

	private static class ValuedNode extends HashSet<fr.emersion.gradualspan.ValuedItem> implements fr.emersion.gradualspan.ValuedNode {
		private ArrayList<fr.emersion.gradualspan.ValuedNode> children = new ArrayList<>();

		public Iterable<fr.emersion.gradualspan.ValuedNode> children() {
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
			for (fr.emersion.gradualspan.ValuedNode vis : this.children) {
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
		GradualSequence expected = gradualSequence();
		//System.out.println(vs);
		GradualSequence gs = GradualSpan.valuedToGradual(vs);
		//System.out.println(gs);
		//System.out.println(expected);
		assertEquals(expected, gs);
	}

	// Only checks support in equals
	// TODO: check projected too
	private static class Occurence extends GradualSupport.Occurence {
		public boolean equals(Object other) {
			if (!(other instanceof GradualSupport.Occurence)) {
				return false;
			}
			GradualSupport.Occurence occ = (GradualSupport.Occurence) other;
			return this.support == occ.support;
		}
	}

	public void testListOccurencesBySequence() {
		GradualSupport support = new GradualSupport.BySequence();

		Collection<GradualSequence> db = gradualDBSmall();
		int minSupport = 2;

		Map<GradualItem, GradualSupport.Occurence> occurences = support.listOccurences(db, minSupport);
		//System.out.println(occurences);

		Map<GradualItem, GradualSupport.Occurence> expected = new HashMap<>();
		GradualSupport.Occurence occ = new Occurence();
		occ.support = 2;
		// TODO: occ.projected
		expected.put(new GradualItem(a, GradualOrder.GREATER), occ);

		assertEquals(expected, occurences);
	}

	public void testForwardTreeMiningSmall() {
		Collection<GradualSequence> db = gradualDBSmall();
		int minSupport = 2;

		Collection<GradualPattern> patterns = GradualSpan.forwardTreeMining(db, minSupport, new GradualSupport.BySequence());
		//System.out.println(patterns);

		List<GradualSequence> expected = new ArrayList<>();

		GradualSequence gs = new GradualSequence();
		expected.add(gs);

		GradualNode gn1_1 = new GradualNode();
		gs.begin.putChild(new GradualItem(a, GradualOrder.GREATER), gn1_1);

		gs.close();

		assertEquals(expected, patterns);
	}

	public void testForwardTreeMining() {
		Collection<GradualSequence> db = gradualDB();
		int minSupport = 2;

		Collection<GradualPattern> patterns = GradualSpan.forwardTreeMining(db, minSupport, new GradualSupport.BySequence());
		//System.out.println(patterns);

		List<GradualSequence> expected = new ArrayList<>();

		{
			GradualSequence gs = new GradualSequence();
			expected.add(gs);

			GradualNode gn1_1 = new GradualNode();
			gs.begin.putChild(new GradualItem(a, GradualOrder.GREATER), gn1_1);

			gs.close();
		}

		{
			GradualSequence gs = new GradualSequence();
			expected.add(gs);

			GradualNode gn1_1 = new GradualNode();
			gs.begin.putChild(new GradualItem(a, GradualOrder.GREATER), gn1_1);

			GradualNode gn1_2 = new GradualNode();
			gs.begin.putChild(new GradualItem(b, GradualOrder.GREATER), gn1_2);

			GradualNode gn2_1 = new GradualNode();
			gn1_1.putChild(new GradualItem(b, GradualOrder.GREATER), gn2_1);

			gs.close();
		}

		assertEquals(expected, patterns);
	}

	public void testMergingSuffixTreeSimple() {
		GradualSequence gs = new GradualSequence();
		{
			GradualNode gn1_1 = new GradualNode();
			gs.begin.putChild(new GradualItem(a, GradualOrder.GREATER), gn1_1);

			GradualNode gn1_2 = new GradualNode();
			gs.begin.putChild(new GradualItem(c, GradualOrder.GREATER), gn1_2);

			GradualNode gn2_1 = new GradualNode();
			gn1_1.putChild(new GradualItem(b, GradualOrder.GREATER), gn2_1);
			gn1_2.putChild(new GradualItem(b, GradualOrder.GREATER), gn2_1);
		}
		gs.close();

		//System.out.println(gs);
		GradualSpan.mergingSuffixTree(gs);
		//System.out.println(gs);

		GradualSequence expected = new GradualSequence();
		{
			GradualNode gn1_1 = new GradualNode();
			expected.begin.putChild(new GradualItem(a, GradualOrder.GREATER), gn1_1);
			expected.begin.putChild(new GradualItem(c, GradualOrder.GREATER), gn1_1);

			gn1_1.putChild(new GradualItem(b, GradualOrder.GREATER), expected.end);
		}
		expected.close();

		assertEquals(expected, gs);
	}

	public void testMergingSuffixTreeWithOtherBranch() {
		GradualSequence gs = new GradualSequence();
		{
			GradualNode gn1_1 = new GradualNode();
			gs.begin.putChild(new GradualItem(a, GradualOrder.GREATER), gn1_1);

			GradualNode gn1_2 = new GradualNode();
			gs.begin.putChild(new GradualItem(c, GradualOrder.GREATER), gn1_2);

			GradualNode gn2_1 = new GradualNode();
			gn1_1.putChild(new GradualItem(b, GradualOrder.GREATER), gn2_1);
			gn1_2.putChild(new GradualItem(b, GradualOrder.GREATER), gn2_1);

			GradualNode gn2_2 = new GradualNode();
			gn1_2.putChild(new GradualItem(a, GradualOrder.LOWER), gn2_2);

			GradualNode gn3_2 = new GradualNode();
			gn2_2.putChild(new GradualItem(c, GradualOrder.GREATER), gn3_2);
		}
		gs.close();

		//System.out.println(gs);
		GradualSpan.mergingSuffixTree(gs);
		//System.out.println(gs);

		GradualSequence expected = new GradualSequence();
		{
			GradualNode gn1_1 = new GradualNode();
			expected.begin.putChild(new GradualItem(c, GradualOrder.GREATER), gn1_1);

			GradualNode gn1_2 = new GradualNode();
			expected.begin.putChild(new GradualItem(a, GradualOrder.GREATER), gn1_2);
			gn1_1.putChild(null, gn1_2);

			GradualNode gn2_1 = new GradualNode();
			gn1_1.putChild(new GradualItem(a, GradualOrder.LOWER), gn2_1);

			gn1_2.putChild(new GradualItem(b, GradualOrder.GREATER), expected.end);
			gn2_1.putChild(new GradualItem(c, GradualOrder.GREATER), expected.end);
		}
		expected.close();

		assertEquals(expected, gs);
	}

	public void testMergingSuffixTreeWithOtherChild() {
		GradualSequence gs = new GradualSequence();
		{
			GradualNode gn1_1 = new GradualNode();
			gs.begin.putChild(new GradualItem(a, GradualOrder.GREATER), gn1_1);

			GradualNode gn1_2 = new GradualNode();
			gs.begin.putChild(new GradualItem(c, GradualOrder.GREATER), gn1_2);

			GradualNode gn2_1 = new GradualNode();
			gn1_1.putChild(new GradualItem(b, GradualOrder.GREATER), gn2_1);
			gn1_2.putChild(new GradualItem(b, GradualOrder.GREATER), gn2_1);

			GradualNode gn2_2 = new GradualNode();
			gn1_2.putChild(new GradualItem(a, GradualOrder.LOWER), gn2_2);
		}
		gs.close();

		//System.out.println(gs);
		GradualSpan.mergingSuffixTree(gs);
		//System.out.println(gs);

		GradualSequence expected = new GradualSequence();
		{
			GradualNode gn1_1 = new GradualNode();
			expected.begin.putChild(new GradualItem(a, GradualOrder.GREATER), gn1_1);

			GradualNode gn1_2 = new GradualNode();
			expected.begin.putChild(new GradualItem(c, GradualOrder.GREATER), gn1_2);

			gn1_2.putChild(null, gn1_1);

			gn1_1.putChild(new GradualItem(b, GradualOrder.GREATER), expected.end);
			gn1_2.putChild(new GradualItem(a, GradualOrder.LOWER), expected.end);
		}
		expected.close();

		assertEquals(expected, gs);
	}

	public void testMergingSuffixTreeIntoParent() {
		GradualSequence gs = new GradualSequence();
		{
			GradualNode gn1_1 = new GradualNode();
			gs.begin.putChild(new GradualItem(a, GradualOrder.GREATER), gn1_1);

			GradualNode gn2_1 = new GradualNode();
			gn1_1.putChild(new GradualItem(c, GradualOrder.GREATER), gn2_1);

			GradualNode gn2_2 = new GradualNode();
			gn1_1.putChild(null, gn2_2);

			GradualNode gn3_1 = new GradualNode();
			gn2_1.putChild(new GradualItem(a, GradualOrder.LOWER), gn3_1);

			GradualNode gn3_2 = new GradualNode();
			gn2_2.putChild(new GradualItem(b, GradualOrder.GREATER), gn3_2);
		}
		gs.close();

		//System.out.println(gs);
		GradualSpan.mergingSuffixTree(gs);
		//System.out.println(gs);

		GradualSequence expected = new GradualSequence();
		{
			GradualNode gn1_1 = new GradualNode();
			expected.begin.putChild(new GradualItem(a, GradualOrder.GREATER), gn1_1);

			GradualNode gn2_1 = new GradualNode();
			gn1_1.putChild(new GradualItem(c, GradualOrder.GREATER), gn2_1);

			gn1_1.putChild(new GradualItem(b, GradualOrder.GREATER), expected.end);
			gn2_1.putChild(new GradualItem(a, GradualOrder.LOWER), expected.end);
		}
		expected.close();

		assertEquals(expected, gs);
	}

	public void testMergingSuffixTreeUsefulEmptyArrow() {
		Supplier<GradualSequence> supplier = () -> {
			GradualSequence gs = new GradualSequence();

			GradualNode gn1_1 = new GradualNode();
			gs.begin.putChild(new GradualItem(a, GradualOrder.GREATER), gn1_1);

			GradualNode gn1_2 = new GradualNode();
			gs.begin.putChild(new GradualItem(c, GradualOrder.GREATER), gn1_2);

			GradualNode gn2_1 = new GradualNode();
			gn1_1.putChild(new GradualItem(b, GradualOrder.GREATER), gn2_1);
			gn1_2.putChild(null, gn2_1);

			GradualNode gn2_2 = new GradualNode();
			gn1_2.putChild(new GradualItem(a, GradualOrder.LOWER), gn2_2);

			gn2_1.putChild(new GradualItem(a, GradualOrder.GREATER), gs.end);
			gn2_2.putChild(new GradualItem(c, GradualOrder.GREATER), gs.end);

			gs.close();

			return gs;
		};

		GradualSequence gs = supplier.get();

		//System.out.println(gs);
		GradualSpan.mergingSuffixTree(gs);
		//System.out.println(gs);

		GradualSequence expected = supplier.get();

		assertEquals(expected, gs);
	}
}

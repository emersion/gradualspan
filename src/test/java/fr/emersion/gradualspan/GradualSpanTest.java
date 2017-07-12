package fr.emersion.jenaplayground;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.ArrayList;
import java.util.HashSet;

import fr.emersion.gradualspan.*;

public class GradualSpanTest extends TestCase {
	public GradualSpanTest(String testName) {
		super(testName);
	}

	public static Test suite() {
		return new TestSuite(GradualSpanTest.class);
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

	public void testGradualSpan() {
		Object a = "a";
		Object b = "b";
		Object c = "c";

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

		//System.out.println(vs);
		GradualNode gn = GradualSpan.valuedToGradual(vs);
		//System.out.println(gn);

		GradualNode expected = new GradualNode();

		GradualNode gn1_1 = new GradualNode();
		expected.putChild(null, gn1_1);

		GradualNode gn1_2 = new GradualNode();
		expected.putChild(null, gn1_2);

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

		//System.out.println(expected);
		assertEquals(expected, gn);
	}
}

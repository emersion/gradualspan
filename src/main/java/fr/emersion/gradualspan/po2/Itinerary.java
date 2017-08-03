package fr.emersion.gradualspan.po2;

import java.lang.Iterable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.jena.rdf.model.Resource;

import fr.emersion.gradualspan.ValuedItemset;
import fr.emersion.gradualspan.ValuedSequence;

public class Itinerary implements ValuedSequence {
	private Iterable<Resource> root;
	protected Map<Resource, Resource> intervals;

	public Itinerary(Iterable<Resource> root, Map<Resource, Resource> intervals) {
		this.root = root;
		this.intervals = intervals;
	}

	public Iterable<ValuedItemset> root() {
		List<ValuedItemset> root = new ArrayList<>();
		for (Resource r : this.root) {
			root.add(new Step(this, r));
		}
		return root;
	}
}

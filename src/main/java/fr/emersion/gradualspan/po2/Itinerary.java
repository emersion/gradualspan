package fr.emersion.gradualspan.po2;

import java.lang.Iterable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;

import fr.emersion.gradualspan.ValuedItemset;
import fr.emersion.gradualspan.ValuedSequence;

public class Itinerary implements ValuedSequence {
	private Resource resource;
	protected Map<Resource, Resource> intervals; // interval -> step
	protected Set<Resource> steps;

	public Itinerary(Resource resource, Map<Resource, Resource> intervals) {
		this.resource = resource;
		this.intervals = intervals;

		StmtIterator stmtIter = resource.listProperties(PO2.hasForStep);
		this.steps = new HashSet<>();
		while (stmtIter.hasNext()) {
			this.steps.add(stmtIter.next().getResource());
		}
	}

	public Iterable<ValuedItemset> root() {
		Set<Resource> rootSteps = new HashSet<>(this.steps);
		for (Resource step : this.steps) {
			Resource interval = step.getProperty(PO2.existsAt).getResource();
			StmtIterator stmtIter = interval.listProperties(Time.intervalBefore);
			while (stmtIter.hasNext()) {
				Resource nextInterval = stmtIter.next().getResource();
				Resource nextStep = this.intervals.get(nextInterval);
				rootSteps.remove(nextStep);
			}
		}

		List<ValuedItemset> root = new ArrayList<>();
		for (Resource step : rootSteps) {
			root.add(new Step(this, step));
		}

		return root;
	}

	public String toString() {
		String s = ":itinerary "+this.resource.getURI()+" {\n";
		for (ValuedItemset is : this.root()) {
			s += is.toString() + "\n";
		}
		s += "}";
		return s;
	}
}

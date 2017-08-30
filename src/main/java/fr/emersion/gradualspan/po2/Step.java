package fr.emersion.gradualspan.po2;

import java.lang.Iterable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;

import fr.emersion.gradualspan.ValuedItem;
import fr.emersion.gradualspan.ValuedItemset;

public class Step implements ValuedItemset {
	private static String compactURI(String uri) {
		return uri.replace(PO2.NS, ":").replace("http://aims.fao.org/aos/agrovoc/", "agrovoc:");
	}

	private Itinerary itinerary;
	private Resource resource;

	public Step(Itinerary itinerary, Resource resource) {
		this.itinerary = itinerary;
		this.resource = resource;
	}

	public Iterator<ValuedItem> iterator() {
		String queryString = Main.prefixes +
			"SELECT ?attribute ?observation ?mixture ?product WHERE {" +
			"	{" +
			"		?observation :observedDuring ?step ." +
			"		?observation :computedResult ?attribute ." +
			"		FILTER NOT EXISTS { ?attribute :isSingularMeasureOf [] } ." + // Comment this to get functions too
			// Its seems this doesn't bring more data
			/*"	} UNION {" +
			"		?step :hasForMixture ?mixture ." +
			"		?observation :observes ?mixture ." +
			"		?observation :computedResult ?attribute ." +
			"		FILTER NOT EXISTS { ?observation :observedDuring ?step } ." +
			"		FILTER NOT EXISTS { ?attribute :isSingularMeasureOf [] } ." +
			*/"	} UNION {" +
			"		?step :hasForMixture ?mixture ." +
			"		?mixture :hasForAttribute ?attribute ." +
			"	} UNION {" +
			"		?step :hasForMixture ?mixture ." +
			"		?mixture :isComposedOf ?product ." +
			"		?product :hasForAttribute ?attribute ." +
			"	} ." +
			"}";

		QuerySolutionMap bindings = new QuerySolutionMap();
		bindings.add("step", this.resource);

		Query query = QueryFactory.create(queryString);
		QueryExecution qexec = QueryExecutionFactory.create(query, this.resource.getModel(), bindings);
		ResultSet results = qexec.execSelect();

		return new Iterator<ValuedItem>() {
			private QueryExecution exec = qexec;
			private ResultSet iter = results;

			public boolean hasNext() {
				boolean next = this.iter.hasNext();
				if (!next) {
					try {
						this.close();
					} catch (Exception e) {
						// TODO
						throw new RuntimeException(e.toString());
					}
				}
				return next;
			}

			// TODO: this is dangerous: return this.next()
			public ValuedItem next() throws NoSuchElementException {
				QuerySolution sol = this.iter.nextSolution();

				Resource attribute = sol.getResource("attribute");
				Resource observation = sol.getResource("observation");
				Resource mixture = sol.getResource("mixture");
				Resource product = sol.getResource("product");

				Statement hasForValue = attribute.getProperty(PO2.hasForValue);
				Statement minKernel = attribute.getProperty(PO2.minKernel);
				Statement hasForUnitOfMeasure = attribute.getProperty(PO2.hasForUnitOfMeasure);
				Statement isSingularMeasureOf = attribute.getProperty(PO2.isSingularMeasureOf);

				Resource qualityType = attribute.getProperty(RDF.type).getResource();

				String k = compactURI(qualityType.getURI());
				if (observation != null && mixture != null) {
					k = ":mixture_observation " + k;
				} else if (observation != null) {
					k = ":observation " + k;
				} else if (product != null) {
					Resource productType = product.getProperty(RDF.type).getResource();
					k = ":product " + compactURI(productType.getURI()) + " " + k;
				} else {
					k = ":mixture " + k;
				}

				// if (hasForValue != null) {
				// 	System.out.printf("Skipping attribute %s with non-numeric value %s\n", attribute, hasForValue.getString());
				// 	continue;
				// }

				String s;
				if (minKernel != null) {
					s = minKernel.getString();
				} else if (hasForValue != null) {
					s = hasForValue.getString();
				} else {
					System.out.printf("Attribute %s has no value\n", attribute);
					return this.next();
				}
				s = s.replace(",", "."); // Screw French people
				if (s.length() > 0 && s.charAt(0) == '[') {
					// e.g. [384;384]
					s = s.substring(1, s.indexOf(';'));
				}
				float v;
				try {
					v = new Float(s);
				} catch (NumberFormatException e) {
					System.out.printf("Attribute %s has invalid value: %s\n", attribute, s);
					return this.next();
				}

				// TODO: prevent duplicate values for the same key
				/*if (row.containsKey(k)) {
					if (isSingularMeasureOf != null) {
						// TODO: handle this case
						return this.next();
					}

					//printProperties(attribute);
					System.out.printf("Step %s has duplicate values for %s (theirs: %f, ours: %f)\n", step, k, row.get(k), v);
					v = Float.NaN;
				}*/

				return new Attribute(k, v);
			}

			public void close() throws Exception {
				this.exec.close();
			}
		};
	}

	public Iterable<ValuedItemset> children() {
		Itinerary itinerary = this.itinerary;
		Resource interval = this.resource.getProperty(PO2.existsAt).getResource();

		return new Iterable<ValuedItemset>() {
			public Iterator<ValuedItemset> iterator() {
				StmtIterator stmtIter = interval.listProperties(Time.intervalBefore);

				return new Iterator<ValuedItemset>() {
					private StmtIterator iter = stmtIter;

					public boolean hasNext() {
						return this.iter.hasNext();
					}

					public ValuedItemset next() throws NoSuchElementException {
						Resource nextInterval = stmtIter.next().getResource();
						Resource nextStep = itinerary.intervals.get(nextInterval);
						return new Step(itinerary, nextStep);
					}
				};
			}
		};
	}

	public boolean equals(Object other) {
		if (!(other instanceof Step)) return false;
		Step step = (Step) other;
		return this.resource.equals(step.resource);
	}

	public int hashCode() {
		return this.resource.hashCode();
	}

	public String toString() {
		String s = ":step "+this.compactURI(this.resource.getURI())+" {\n";
		for (ValuedItem i : this) {
			s += i.toString() + "\n";
		}
		for (ValuedItemset is : this.children()) {
			s += is.toString() + "\n";
		}
		s += "}";
		return s;
	}
}

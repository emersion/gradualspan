package fr.emersion.jenaplayground;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.Iterable;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.util.FileManager;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

public class App {
	public static final String PO2_NS = "http://opendata.inra.fr/PO2/";
	public static final String TIME_NS = "http://www.w3.org/2006/time#";

	public static void printProperties(Resource object) {
		StmtIterator stmtIter = object.listProperties();
		while (stmtIter.hasNext()) {
			Statement stmt = stmtIter.next();
			System.out.println(stmt);
		}
	}

	public static void printChildren(OntProperty prop, Resource interval) {
		System.out.printf("%s", interval);
		while (true) {
			Statement stmt = interval.getProperty(prop);
			if (stmt == null) {
				break;
			}
			interval = stmt.getResource();
			System.out.printf(" -> %s\n", interval);
		}
		System.out.printf("\n");
	}

	private static void step(Map<Resource, Integer> depths, OntProperty nextProp, Map<Resource, Collection<Resource>> ancestors, Resource object, int i) {
		if (depths.containsKey(object)) {
			if (depths.get(object) != i) {
				System.out.printf("Invalid depth for %s (theirs: %d, ours: %d)\n", object, depths.get(object), i);
				return;
			} else if (i != 0) {
				return;
			}
		} else {
			depths.put(object, i);
		}

		StmtIterator stmtIter = object.listProperties(nextProp);
		while (stmtIter.hasNext()) {
			Resource next = stmtIter.next().getResource();
			if (next.equals(object)) {
				System.out.println("Object after "+object.toString()+" is itself");
				continue;
			}
			step(depths, nextProp, ancestors, next, i + 1);
		}

		if (ancestors.containsKey(object)) {
			for (Resource prev : ancestors.get(object)) {
				if (prev.equals(object)) {
					System.out.println("Object before "+object.toString()+" is itself");
					continue;
				}
				step(depths, nextProp, ancestors, prev, i - 1);
			}
		}
	}

	public static List<List<Resource>> sortObjects(OntProperty nextProp, Collection<Resource> objects) {
		// Build a map of ancestors
		Map<Resource, Collection<Resource>> ancestors = new HashMap<>();
		for (Resource object : objects) {
			StmtIterator stmtIter = object.listProperties(nextProp);
			while (stmtIter.hasNext()) {
				Resource next = stmtIter.next().getResource();

				if (!ancestors.containsKey(next)) {
					ancestors.put(next, new ArrayList<>());
				}
				ancestors.get(next).add(object);
			}
		}

		// Walk in graph
		Map<Resource, Integer> depths = new HashMap<>();
		for (Resource object : objects) {
			if (depths.containsKey(object)) {
				continue;
			}

			depths.put(object, 0);

			step(depths, nextProp, ancestors, object, 0);
		}

		// Sort objects
		List<Resource> list = new ArrayList<>(objects);
		Collections.sort(list, (a, b) -> depths.get(a) - depths.get(b));

		// Build 2D list
		int curDepth = 0;
		List<List<Resource>> result = new ArrayList<>();
		List<Resource> cur = null;
		for (Resource object : list) {
			int depth = depths.get(object);
			if (cur == null || curDepth < depth) {
				curDepth = depth;
				cur = new ArrayList<>();
				result.add(cur);
				curDepth = depth;
			}

			cur.add(object);
		}

		return result;
	}

	public static String compactURI(String uri) {
		return uri.replace(PO2_NS, ":");
	}

	public static void main(String[] args) throws Exception {
		//String ontFile = "/home/simon/Downloads/PO2_core_v1.4.rdf";
		//String objectsFile = "/home/simon/Downloads/Echantillon_PO2_CellExtraDry.rdf";
		//String objectsFile = "/home/simon/Downloads/PO2_CellExtraDryData.rdf";
		String objectsFile = "/home/simon/Downloads/PO2_CarredasData.rdf";

		String processURI = "http://opendata.inra.fr/PO2/cellextradry_process_13";
		String itineraryURI = "http://opendata.inra.fr/PO2/Itinerary1";
		String stepURI = "http://opendata.inra.fr/PO2/cellextradry_process_13_step_40";

		String isQualityMeasurementOfURI = "http://purl.obolibrary.org/obo/IAO_0000221";

		System.out.println("Loading ontology...");
		OntModel ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, null);
		ontModel.read(FileManager.get().open(objectsFile), null);

		Model model = ontModel;
		//Model model = ontModel.getBaseModel();
		//Model model = ModelFactory.createDefaultModel();
		//model.read(FileManager.get().open(objectsFile), null);
		//System.out.println("Loaded objects");

		String prefixes = "PREFIX rdf: <"+RDF.uri+">\n" +
			"PREFIX rdfs: <"+RDFS.uri+">\n" +
			"PREFIX : <"+PO2_NS+">\n" +
			"PREFIX time: <"+TIME_NS+">\n";

		OntClass itineraryClass = ontModel.getOntClass(PO2_NS+"itinerary");
		OntProperty hasForStepProp = ontModel.getOntProperty(PO2_NS+"hasForStep");
		OntProperty existsAtProp = ontModel.getOntProperty(PO2_NS+"existsAt");
		OntProperty intervalBeforeProp = ontModel.getOntProperty(TIME_NS+"intervalBefore");
		OntProperty isQualityMeasurementOfProp = ontModel.getOntProperty(isQualityMeasurementOfURI);
		OntProperty hasForValueProp = ontModel.getOntProperty(PO2_NS+"hasForValue");
		OntProperty minKernelProp = ontModel.getOntProperty(PO2_NS+"minKernel");
		OntProperty hasForUnitOfMeasureProp = ontModel.getOntProperty(PO2_NS+"hasForUnitOfMeasure");
		OntProperty observesProp = ontModel.getOntProperty(PO2_NS+"observes");

		/*String queryString = prefixes +
			"SELECT ?x WHERE {\n" +
			//"	?x rdf:type/rdfs:subClassOf* :step .\n" +
			//"	<"+itineraryURI+"> :hasForStep ?y .\n" +
			//"	?y :existsAt/!time:intervalBefore [] .\n" +
			"	?x :existsAt[time:intervalBefore*[^:existsAt <"+stepURI+">]] .\n" +
			"}";

		Query query = QueryFactory.create(queryString);
		try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
			ResultSet results = qexec.execSelect();
			while (results.hasNext()) {
				QuerySolution soln = results.nextSolution();
				Resource r = soln.getResource("x");
				System.out.println(r);
			}
		}

		System.out.println("---");*/

		/*String queryString = prefixes +
			"SELECT (COUNT(DISTINCT ?q) AS ?n) WHERE {\n" +
			"	?i rdf:type/rdfs:subClassOf* :itinerary .\n" +
			"	?i :hasForStep ?s .\n" +
			"	OPTIONAL {\n" +
			"		?o :observedDuring ?s ." +
			"		?o :computedResult/:hasForMeasure/<"+isQualityMeasurementOfURI+">/rdf:type ?q ." +
			"	} .\n" +
			"} GROUP BY ?i";*/

		/*String queryString = prefixes +
			"SELECT (COUNT(DISTINCT ?q) AS ?n) WHERE {\n" +
			"	?i rdf:type/rdfs:subClassOf* :itinerary .\n" +
			"	?i :hasForStep ?s .\n" +
			"	OPTIONAL {\n" +
			"		?s :hasForMixture/:isComposedOf ?p ." +
			"		?p :hasForAttribute/:hasForMeasure/<"+isQualityMeasurementOfURI+">/rdf:type ?q ." +
			"	} .\n" +
			"} GROUP BY ?i";*/

		/*String queryString = prefixes +
			"SELECT (COUNT(DISTINCT ?mt) AS ?n) WHERE {\n" +
			"	?i rdf:type/rdfs:subClassOf* :itinerary .\n" +
			"	?i :hasForStep ?s .\n" +
			"	OPTIONAL {\n" +
			"		?s :hasForMaterial ?m ." +
			"		?m rdf:type ?mt" +
			//"		?m :hasForAttribute/:hasForMeasure/<"+isQualityMeasurementOfURI+">/rdf:type ?q ." +
			"	} .\n" +
			"} GROUP BY ?i";

		Query query = QueryFactory.create(queryString);
		try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
			ResultSet results = qexec.execSelect();
			int i = 0;
			while (results.hasNext()) {
				QuerySolution soln = results.nextSolution();
				System.out.printf("%s\n", soln.getLiteral("n").getString());
				//System.out.printf("Observations: %s\n", soln.get("no"));
				//Resource r = soln.getResource("s");
				//System.out.println(r);
				i++;
			}
			System.out.printf("Total: %d\n", i);
		}

		System.out.println("---");*/

		System.out.println("Loading time concept...");

		Map<Resource, Resource> steps = new HashMap<>();
		{
			String queryString = prefixes +
				"SELECT ?s ?i WHERE {\n" +
				"	?s rdf:type/rdfs:subClassOf* :step .\n" +
				"	?s :existsAt ?i .\n" +
				"}";

			Query query = QueryFactory.create(queryString);
			try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
				ResultSet results = qexec.execSelect();
				while (results.hasNext()) {
					QuerySolution soln = results.nextSolution();
					steps.put(soln.getResource("i"), soln.getResource("s"));
				}
			}
		}

		List<List<Resource>> intervals = sortObjects(intervalBeforeProp, steps.keySet());
		// for (List<Resource> list : result) {
		// 	for (Resource interval : list) {
		// 		System.out.println(interval);
		//
		// 		StmtIterator intervalBeforeIter = interval.listProperties(intervalBeforeProp);
		// 		while (intervalBeforeIter.hasNext()) {
		// 			System.out.printf("	next: %s\n", intervalBeforeIter.next().getResource());
		// 		}
		// 	}
		// 	System.out.printf("\n");
		// }

		ResIterator iter = model.listSubjectsWithProperty(RDF.type, itineraryClass);
		while (iter.hasNext()) {
			Resource itinerary = iter.next();
			//printProperties(itinerary);
			System.out.printf("Processing itinerary %s\n", itinerary);

			StmtIterator stmtIter = itinerary.listProperties(hasForStepProp);
			Set<Resource> itinerarySteps = new HashSet<>();
			while (stmtIter.hasNext()) {
				Resource step = stmtIter.next().getResource();
				itinerarySteps.add(step);
			}

			Set<String> attributes = new HashSet<>();
			List<Map<String, Float>> table = new ArrayList<>();
			for (List<Resource> list : intervals) {
				Map<String, Float> row = new HashMap<>();
				table.add(row);

				for (Resource interval : list) {
					Resource step = steps.get(interval);
					if (step == null) {
						System.out.printf("Interval %s has no step\n", interval);
						continue;
					}
					if (!itinerarySteps.contains(step)) {
						continue;
					}

					String queryString = prefixes +
						"SELECT ?measure ?observation ?mixture ?product WHERE {" +
						"	{" +
						"		?observation :observedDuring ?step ." +
						"		?observation :computedResult/:hasForMeasure ?measure ." +
						"	} UNION {" +
						"		?step :hasForMixture ?mixture ." +
						"		?mixture :hasForAttribute/:hasForMeasure ?measure ." +
						"	} UNION {" +
						"		?step :hasForMixture ?mixture ." +
						"		?mixture :isComposedOf ?product ." +
						"		?product :hasForAttribute/:hasForMeasure ?measure ." +
						"	} ." +
						"	?measure rdf:type/rdfs:subClassOf* :simple_measure ." +
						"}";

					QuerySolutionMap bindings = new QuerySolutionMap();
					bindings.add("step", step);

					Query query = QueryFactory.create(queryString);
					try (QueryExecution qexec = QueryExecutionFactory.create(query, model, bindings)) {
						ResultSet results = qexec.execSelect();
						while (results.hasNext()) {
							QuerySolution sol = results.nextSolution();
							Resource measure = sol.getResource("measure");
							Resource observation = sol.getResource("observation");
							Resource mixture = sol.getResource("mixture");
							Resource product = sol.getResource("product");

							Statement hasForValue = measure.getProperty(hasForValueProp);
							Statement minKernel = measure.getProperty(minKernelProp);
							Statement hasForUnitOfMeasure = measure.getProperty(hasForUnitOfMeasureProp);
							Statement isQualityMeasurementOf = measure.getProperty(isQualityMeasurementOfProp);

							if (isQualityMeasurementOf == null) {
								System.out.printf("Measure %s has no quality\n", measure);
								continue;
							}

							Resource qualityType = isQualityMeasurementOf.getResource().getProperty(RDF.type).getResource();

							String k = compactURI(qualityType.getURI());
							if (observation != null) {
								k = ":observation " + k;
							} else if (product != null) {
								Resource productType = product.getProperty(RDF.type).getResource();
								k = ":product " + compactURI(productType.getURI()) + " " + k;
							} else {
								k = ":mixture " + k;
							}

							String s;
							if (hasForValue != null) {
								s = hasForValue.getString();
							} else if (minKernel != null) {
								s = minKernel.getString();
							} else {
								System.out.printf("Measure %s has no value\n", measure);
								continue;
							}
							s = s.replace(",", "."); // Screw French people
							float v;
							try {
								if (s.length() > 0 && s.charAt(0) == '[') {
									// e.g. [384;384]
									s = s.substring(1, s.indexOf(';'));
								}
								v = new Float(s);
							} catch (NumberFormatException e) {
								System.out.printf("Measure %s has invalid value: %s\n", measure, s);
								continue;
							}

							if (row.containsKey(k)) {
								System.out.printf("Step %s has duplicate values for %s (theirs: %f, ours: %f)\n", step, k, row.get(k), v);
								continue;
							}

							attributes.add(k);
							row.put(k, v);
						}
					}
				}
			}

			int maxValues = attributes.size() * table.size();
			int nbrValues = 0;

			String name = compactURI(itinerary.getURI()).replace(":", "");
			try (OutputStream os = new FileOutputStream("output-"+name+".tsv")) {
				PrintStream ps = new PrintStream(os);

				ps.println(String.join("\t", attributes));
				for (Map<String, Float> row : table) {
					List<String> values = new ArrayList<>();
					for (String k : attributes) {
						Float v = row.get(k);
						if (v == null) {
							values.add("");
						} else {
							values.add(v.toString());
							nbrValues++;
						}
					}
					ps.println(String.join("\t", values));
				}
			}

			System.out.printf("Values rate: %f\n", (float) nbrValues / (float) maxValues);
		}
	}
}

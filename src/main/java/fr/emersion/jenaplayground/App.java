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

	public static void printChildren(OntProperty prop, Resource object) {
		System.out.println(object);
		StmtIterator stmtIter = object.listProperties(prop);
		while (stmtIter.hasNext()) {
			Resource next = stmtIter.next().getResource();
			System.out.printf("\t%s\n", next);
		}
	}

	// public static void printChildren(OntProperty prop, Resource object) {
	// 	System.out.println(object);
	// 	while (true) {
	// 		Statement stmt = object.getProperty(prop);
	// 		if (stmt == null) {
	// 			break;
	// 		}
	// 		object = stmt.getResource();
	// 		System.out.printf("	%s\n", object);
	// 	}
	// 	System.out.printf("\n");
	// }

	public static Collection<Resource> getHeads(OntProperty nextProp, Collection<Resource> objects) {
		Set<Resource> heads = new HashSet<>(objects);
		for (Resource object : objects) {
			StmtIterator stmtIter = object.listProperties(nextProp);
			while (stmtIter.hasNext()) {
				Resource next = stmtIter.next().getResource();
				heads.remove(next);
			}
		}
		return heads;
	}

	private static void step(Map<Resource, Integer> depths, OntProperty nextProp, Map<Resource, Collection<Resource>> ancestors, Resource object, int lastDepth, int depth) {
		if (depths.containsKey(object)) {
			int existingDepth = depths.get(object);
			if (existingDepth != depth) {
				// Depth conflict
				if (lastDepth < depth && lastDepth < existingDepth) {
					// Going forward
					return;
				}
				if (lastDepth > depth && lastDepth > existingDepth) {
					// Going backward
					return;
				}
			} else {
				// Already explored
				return;
			}
		}
		depths.put(object, depth);

		StmtIterator stmtIter = object.listProperties(nextProp);
		while (stmtIter.hasNext()) {
			Resource next = stmtIter.next().getResource();
			if (next.equals(object)) {
				System.out.println("Object after "+object.toString()+" is itself");
				continue;
			}
			step(depths, nextProp, ancestors, next, depth, depth + 1);
		}

		if (ancestors.containsKey(object)) {
			for (Resource prev : ancestors.get(object)) {
				if (prev.equals(object)) {
					System.out.println("Object before "+object.toString()+" is itself");
					continue;
				}
				step(depths, nextProp, ancestors, prev, depth, depth - 1);
			}
		}
	}

	private static void allPathsStep(OntProperty nextProp, Resource object, List<Resource> path, List<List<Resource>> paths) {
		boolean first = true;
		List<Resource> truncatedPath = path.subList(0, path.size());
		StmtIterator stmtIter = object.listProperties(nextProp);
		while (stmtIter.hasNext()) {
			Resource next = stmtIter.next().getResource();
			if (next.equals(object)) {
				continue;
			}

			if (first) {
				allPathsStep(nextProp, next, path, paths);
				first = false;
			} else {
				List<Resource> subPath = new ArrayList<>(truncatedPath);
				paths.add(subPath);
				allPathsStep(nextProp, next, subPath, paths);
			}
		}
	}

	// TODO: doesn't work for merges
	public static List<List<Resource>> allPaths(OntProperty nextProp, Collection<Resource> objects) {
		// Find roots
		Set<Resource> roots = new HashSet<>(objects);
		for (Resource object : objects) {
			StmtIterator stmtIter = object.listProperties(nextProp);
			while (stmtIter.hasNext()) {
				Resource next = stmtIter.next().getResource();
				roots.remove(next);
			}
		}

		// DFS
		List<List<Resource>> paths = new ArrayList<>();
		for (Resource root : roots) {
			List<Resource> path = new ArrayList<>();
			paths.add(path);
			allPathsStep(nextProp, root, path, paths);
		}
		return paths;
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

			step(depths, nextProp, ancestors, object, 0, 0);
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
		//String objectsFile = "/home/simon/Downloads/PO2_CarredasData.rdf";

		// v1.5
		String objectsFile = "/home/simon/Downloads/Ontologie/PO2_output_CellExtraDry.rdf";
		//String objectsFile = "/home/simon/Downloads/Ontologie/PO2_output_CAREDAS_THESE_BOISARD_SIM_CAREDAS_Gierczynski_CAREDAS_LAWRENCE_CAREDAS_MOSCA_CAREDAS_PHAN_CARREDAS_BIGASKI_CARREDAS_LAWRENCE.rdf";

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
		OntProperty isSingularMeasureOfProp = ontModel.getOntProperty(PO2_NS+"isSingularMeasureOf");

		/*{
			String queryString = prefixes +
				"SELECT DISTINCT ?interval WHERE {\n" +
				"	?step rdf:type/rdfs:subClassOf* :step .\n" +
				"	?step :existsAt ?interval .\n" +
				"	FILTER NOT EXISTS { [] time:intervalBefore ?interval } .\n" +
				"}";

			Query query = QueryFactory.create(queryString);
			try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
				ResultSet results = qexec.execSelect();
				while (results.hasNext()) {
					QuerySolution sol = results.nextSolution();
					printChildren(intervalBeforeProp, sol.getResource("interval"));
				}
			}
		}

		System.out.println("---");*/

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
				QuerySolution sol = results.nextSolution();
				Resource r = sol.getResource("x");
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
			"		?s :hasForMixture/:isComposedOf?p ." +
			"		?p :hasForAttriute/:hasForMeasure/<"+isQualityMeasurementOfURI+">/rdf:type ?q ." +
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
				QuerySolution sol = results.nextSolution();
				System.out.printf("%s\n", sol.getLiteral("n").getString());
				//System.out.printf("Observations: %s\n", sol.get("no"));
				//Resource r = sol.getResource("s");
				//System.out.println(r);
				i++;
			}
			System.out.printf("Total: %d\n", i);
		}

		System.out.println("---");*/

		/*String queryString = prefixes +
			"SELECT ?r WHERE {\n" +
			"	?o rdf:type/rdfs:subClassOf* :observation ." +
			"	?o :computedResult ?r ." +
			"}";

		Query query = QueryFactory.create(queryString);
		try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
			ResultSet results = qexec.execSelect();
			int i = 0;
			while (results.hasNext()) {
				QuerySolution sol = results.nextSolution();
				printProperties(sol.getResource("r"));
				i++;
			}
			System.out.printf("Total: %d\n", i);
		}

		System.out.println("---");*/

		/*{
			String queryString = prefixes +
				"SELECT ?step ?type ?interval ?mixture WHERE {" +
				"	<http://opendata.inra.fr/PO2/cellextradry_process_2_itinerary_4> :hasForStep ?step ." +
				"	?step a ?type ." +
				"	?step :existsAt ?interval ." +
				"	OPTIONAL { ?step :hasForMixture ?mixture } ." +
				"}";

			Query query = QueryFactory.create(queryString);
			try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
				ResultSet results = qexec.execSelect();
				int i = 0;
				while (results.hasNext()) {
					QuerySolution sol = results.nextSolution();
					printProperties(sol.getResource("step"));
					printProperties(sol.getResource("type"));
					Resource mixture = sol.getResource("mixture");
					if (mixture != null) {
						printProperties(mixture);
					}
					//printChildren(intervalBeforeProp, sol.getResource("interval"));
					System.out.println("");
					i++;
				}
				System.out.printf("Total: %d\n", i);
			}
		}

		System.out.println("---");*/

		System.out.println("Loading time concept...");

		Map<Resource, Resource> steps = new HashMap<>();
		{
			String queryString = prefixes +
				"SELECT ?s ?i WHERE {" +
				"	?s rdf:type/rdfs:subClassOf* :step ." +
				"	?s :existsAt ?i ." +
				"}";

			Query query = QueryFactory.create(queryString);
			try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
				ResultSet results = qexec.execSelect();
				while (results.hasNext()) {
					QuerySolution sol = results.nextSolution();
					steps.put(sol.getResource("i"), sol.getResource("s"));
				}
			}
		}

		List<List<Resource>> intervals = sortObjects(intervalBeforeProp, steps.keySet());
		for (List<Resource> list : intervals) {
			for (Resource interval : list) {
				printChildren(intervalBeforeProp, interval);
			}
			System.out.printf("\n");
		}

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

			Set<String> keys = new HashSet<>();
			List<Map<String, Float>> table = new ArrayList<>();
			int i = 0;
			for (List<Resource> list : intervals) {
				int j = 0;
				for (Resource interval : list) {
					Resource step = steps.get(interval);
					if (step == null) {
						System.out.printf("Interval %s has no step\n", interval);
						continue;
					}
					if (!itinerarySteps.contains(step)) {
						continue;
					}

					j++;

					Map<String, Float> row = new HashMap<>();
					table.add(row);
					row.put("t", (float) i);

					//System.out.printf("	Processing step %s...\n", step);
					//printProperties(step);

					String queryString = prefixes +
						"SELECT ?attribute ?observation ?mixture ?product WHERE {" +
						"	{" +
						"		?observation :observedDuring ?step ." +
						"		?observation :computedResult ?attribute ." +
						//"		FILTER NOT EXISTS { ?attribute :isSingularMeasureOf [] } ." +
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
					bindings.add("step", step);

					Query query = QueryFactory.create(queryString);
					try (QueryExecution qexec = QueryExecutionFactory.create(query, model, bindings)) {
						ResultSet results = qexec.execSelect();
						while (results.hasNext()) {
							QuerySolution sol = results.nextSolution();
							Resource attribute = sol.getResource("attribute");
							Resource observation = sol.getResource("observation");
							Resource mixture = sol.getResource("mixture");
							Resource product = sol.getResource("product");

							Statement hasForValue = attribute.getProperty(hasForValueProp);
							Statement minKernel = attribute.getProperty(minKernelProp);
							Statement hasForUnitOfMeasure = attribute.getProperty(hasForUnitOfMeasureProp);
							Statement isSingularMeasureOf = attribute.getProperty(isSingularMeasureOfProp);

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
								continue;
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
								continue;
							}

							if (row.containsKey(k)) {
								if (isSingularMeasureOf != null) {
									// TODO: handle this case
									continue;
								}

								//printProperties(attribute);
								System.out.printf("Step %s has duplicate values for %s (theirs: %f, ours: %f)\n", step, k, row.get(k), v);
								v = Float.NaN;
							}

							keys.add(k);
							row.put(k, v);
						}
					}
				}

				if (j > 0) {
					i++;
				}
			}

			int maxValues = keys.size() * table.size();
			int nbrValues = 0;

			String name = compactURI(itinerary.getURI()).replace(":", "");
			try (OutputStream os = new FileOutputStream("output-"+name+".tsv")) {
				PrintStream ps = new PrintStream(os);

				ps.println("t\t" + String.join("\t", keys));
				for (Map<String, Float> row : table) {
					List<String> values = new ArrayList<>();

					values.add(row.get("t").toString());
					//values.add("");

					for (String k : keys) {
						Float v = row.get(k);
						if (v == null) {
							values.add("");
						} else {
							values.add(v.toString());
							if (!Float.isNaN(v)) {
								nbrValues++;
							}
						}
					}
					ps.println(String.join("\t", values));
				}
			}

			System.out.printf("Values rate: %f\n", (float) nbrValues / (float) maxValues);
			break;
		}
		//*/
	}
}

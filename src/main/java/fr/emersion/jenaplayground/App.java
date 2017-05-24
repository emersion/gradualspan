package fr.emersion.jenaplayground;

import java.lang.Iterable;
import java.util.ArrayList;
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
			System.out.printf(" -> %s", interval);
		}
		System.out.printf("\n");
	}

	// TODO: assumes objects is a tree
	public static List<List<Resource>> sortObjects(OntProperty prop, Iterable<Resource> objects) {
		// Find heads: find object that don't have an anscestor
		Set<Resource> heads = new HashSet();
		for (Resource object : objects) {
			heads.add(object);
		}

		for (Resource object : objects) {
			StmtIterator stmtIter = object.listProperties(prop);
			while (stmtIter.hasNext()) {
				Statement stmt = stmtIter.next();
				heads.remove(stmt.getResource());
			}
		}

		// BFS from heads to leaves
		Queue<Resource> queue = new LinkedList();
		for (Resource head : heads) {
			queue.add(head);
		}

		int queueLen = queue.size();
		int nextQueueLen = 0;
		List<Resource> list = new ArrayList();
		List<List<Resource>> result = new ArrayList();
		while (true) {
			Resource object = queue.poll();
			if (object == null) {
				break;
			}
			queueLen--;

			list.add(object);

			Statement s = object.getProperty(prop);
			if (s != null) {
				queue.add(s.getResource());
				nextQueueLen++;
			}

			if (queueLen == 0) {
				queueLen = nextQueueLen;
				nextQueueLen = 0;

				result.add(list);
				list = new ArrayList();
			}
		}

		return result;
	}

	public static void main(String[] args) {
		String ontFile = "/home/simon/Downloads/PO2_core_v1.4.rdf";
		String objectsFile = "/home/simon/Downloads/Echantillon_PO2_CellExtraDry.rdf";

		String processURI = "http://opendata.inra.fr/PO2/cellextradry_process_13"; // count: 1
		String itineraryURI = "http://opendata.inra.fr/PO2/cellextradry_process_13_itinerary_14"; // count: 3
		String stepURI = "http://opendata.inra.fr/PO2/cellextradry_process_13_step_40";

		OntModel ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, null);
		ontModel.read(FileManager.get().open(ontFile), null);
		System.out.println("Loaded ontology");

		Model model = ontModel.getBaseModel();
		//Model model = ModelFactory.createDefaultModel();
		model.read(FileManager.get().open(objectsFile), null);
		System.out.println("Loaded objects");

		/*String queryString =
			"PREFIX rdf: <"+RDF.uri+">\n" +
			"PREFIX rdfs: <"+RDFS.uri+">\n" +
			"PREFIX : <"+PO2_NS+">\n" +
			"PREFIX time: <"+TIME_NS+">\n" +
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

		OntClass itineraryClass = ontModel.getOntClass(PO2_NS+"itinerary");
		OntProperty hasForStepProp = ontModel.getOntProperty(PO2_NS+"hasForStep");
		OntProperty existsAtProp = ontModel.getOntProperty(PO2_NS+"existsAt");
		OntProperty intervalBeforeProp = ontModel.getOntProperty(TIME_NS+"intervalBefore");

		ResIterator iter = model.listSubjectsWithProperty(RDF.type, itineraryClass);
		iter.next();
		while (iter.hasNext()) {
			Resource itinerary = iter.next();
			//printProperties(itinerary);

			StmtIterator stmtIter = itinerary.listProperties(hasForStepProp);
			Map<Resource, Resource> steps = new HashMap();
			while (stmtIter.hasNext()) {
				Resource step = stmtIter.next().getResource();
				steps.put(step.getProperty(existsAtProp).getResource(), step);
			}

			List<List<Resource>> result = sortObjects(intervalBeforeProp, steps.keySet());
			for (List<Resource> list : result) {
				for (Resource interval : list) {
					System.out.println(interval);

					StmtIterator intervalBeforeIter = interval.listProperties(intervalBeforeProp);
					while (intervalBeforeIter.hasNext()) {
						System.out.printf("	next: %s\n", intervalBeforeIter.next().getResource());
					}
				}
				System.out.printf("\n");
			}

			// while (stmtIter.hasNext()) {
			// 	Statement stmt = stmtIter.next();
			// 	Resource step = stmt.getResource();
			// 	//printProperties(step);
			// 	Resource interval = step.getProperty(existsAtProp).getResource();
			// 	//printChildren(intervalBeforeProp, interval);
			// }

			break;
		}
	}
}

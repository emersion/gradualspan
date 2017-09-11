package fr.emersion.gradualspan.po2;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.FileManager;

import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

import fr.emersion.gradualspan.GradualPattern;
import fr.emersion.gradualspan.GradualSequence;
import fr.emersion.gradualspan.GradualSpan;
import fr.emersion.gradualspan.GradualSupport;
import fr.emersion.gradualspan.ValuedSequence;
import fr.emersion.gradualspan.graphviz.Writer;

public class Main {
	protected static final String prefixes =
		"PREFIX rdf: <"+RDF.uri+">\n" +
		"PREFIX rdfs: <"+RDFS.uri+">\n" +
		"PREFIX : <"+PO2.NS+">\n" +
		"PREFIX time: <"+Time.NS+">\n";

	public static void main(String[] args) throws Exception {
		if (args.length == 0) {
			System.out.println("usage: gradualspan-po2 <database>");
			System.exit(1);
		}

		String objectsFile = args[0];

		System.out.println("Loading database in memory...");
		OntModel ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, null);
		ontModel.read(FileManager.get().open(objectsFile), null);
		Model model = ontModel;

		System.out.println("Loading time concept...");
		Map<Resource, Resource> intervals = new HashMap<>();
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
					intervals.put(sol.getResource("i"), sol.getResource("s"));
				}
			}
		}

		System.out.println("Loading itineraries...");
		List<ValuedSequence> dbv = new ArrayList<>();
		ResIterator iter = model.listSubjectsWithProperty(RDF.type, PO2.itinerary);
		while (iter.hasNext()) {
			Resource itinerary = iter.next();
			dbv.add(new Itinerary(itinerary, intervals));
		}
		//System.out.println(dbv);
		System.out.println("Loaded "+dbv.size()+" itineraries.");

		String valuedSeqGraphPath = "valued-sequences.dot";
		System.out.println("Writing valued sequences to "+valuedSeqGraphPath+"...");
		try (FileOutputStream out = new FileOutputStream(valuedSeqGraphPath)) {
			Writer w = new Writer(out);
			for (ValuedSequence s : dbv) {
				w.write(s);
			}
		}

		System.out.println("Running valuedToGradual...");
		List<GradualSequence> dbg = new ArrayList<>();
		for (ValuedSequence vs : dbv) {
			dbg.add(GradualSpan.valuedToGradual(vs));
		}

		String gradualSeqGraphPath = "gradual-sequences.dot";
		System.out.println("Writing gradual sequences to "+gradualSeqGraphPath+"...");
		try (FileOutputStream out = new FileOutputStream(gradualSeqGraphPath)) {
			Writer w = new Writer(out);
			for (GradualSequence s : dbg) {
				w.write(s);
			}
		}

		System.out.println("Running forwardTreeMining...");
		Collection<GradualPattern> patterns = GradualSpan.forwardTreeMining(dbg, 15, new GradualSupport.BySequence());

		System.out.println("Running mergingSuffixTree...");
		for (GradualPattern pattern : patterns) {
			GradualSpan.mergingSuffixTree(pattern);
		}

		System.out.println("Extracted "+patterns.size()+" patterns.");
		int i = 0;
		for (GradualPattern pattern : patterns) {
			System.out.println(i+": support="+pattern.support());
			i++;
		}

		String patternsGraphPath = "patterns.dot";
		System.out.println("Writing patterns to "+patternsGraphPath+"...");
		try (FileOutputStream out = new FileOutputStream(patternsGraphPath)) {
			Writer w = new Writer(out);
			for (GradualSequence p : patterns) {
				w.write(p);
			}
		}
	}
}

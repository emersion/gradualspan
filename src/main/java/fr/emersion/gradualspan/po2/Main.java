package fr.emersion.gradualspan.po2;

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

import fr.emersion.gradualspan.GradualSequence;
import fr.emersion.gradualspan.GradualSpan;
import fr.emersion.gradualspan.GradualSupport;
import fr.emersion.gradualspan.ValuedSequence;

public class Main {
	protected static final String prefixes =
		"PREFIX rdf: <"+RDF.uri+">\n" +
		"PREFIX rdfs: <"+RDFS.uri+">\n" +
		"PREFIX : <"+PO2.NS+">\n" +
		"PREFIX time: <"+Time.NS+">\n";

	public static void main(String[] args) throws Exception {
		String objectsFile = "/home/simon/Downloads/Ontologie/PO2_output_CellExtraDry.rdf";
		//String objectsFile = "/home/simon/Downloads/Ontologie/PO2_output_CAREDAS_THESE_BOISARD_SIM_CAREDAS_Gierczynski_CAREDAS_LAWRENCE_CAREDAS_MOSCA_CAREDAS_PHAN_CARREDAS_BIGASKI_CARREDAS_LAWRENCE.rdf";

		System.out.println("Loading ontology...");

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

		List<ValuedSequence> db = new ArrayList<>();
		ResIterator iter = model.listSubjectsWithProperty(RDF.type, PO2.itinerary);
		while (iter.hasNext()) {
			Resource itinerary = iter.next();
			db.add(new Itinerary(itinerary, intervals));
		}

		System.out.println(db);

		System.out.println("Applying gradualspan...");

		Collection<GradualSequence> patterns = GradualSpan.gradualSpan(db, 2, new GradualSupport.BySequence());

		System.out.println(patterns);
	}
}

package fr.emersion.gradualspan.po2;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

class Time {
	private static final Model m = ModelFactory.createDefaultModel();

	public static final String NS = "http://www.w3.org/2006/time#";
	public static final Resource NAMESPACE = m.createResource(NS);

	public static String getURI() {
		return NS;
	}

	public static final Property intervalBefore = m.createProperty(NS + "intervalBefore");
}

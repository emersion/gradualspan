package fr.emersion.gradualspan.po2;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

class PO2 {
	private static final Model m = ModelFactory.createDefaultModel();

	public static final String NS = "http://opendata.inra.fr/PO2/";
	public static final Resource NAMESPACE = m.createResource(NS);

	public static String getURI() {
		return NS;
	}

	public static final Resource itinerary = m.createResource(NS + "itinerary");

	public static final Property hasForStep = m.createProperty(NS + "hasForStep");
	public static final Property existsAt = m.createProperty(NS + "existsAt");
	public static final Property isQualityMeasurementOf = m.createProperty("http://purl.obolibrary.org/obo/IAO_0000221");
	public static final Property hasForValue = m.createProperty(NS + "hasForValue");
	public static final Property minKernel = m.createProperty(NS + "minKernel");
	public static final Property hasForUnitOfMeasure = m.createProperty(NS + "hasForUnitOfMeasure");
	public static final Property observes = m.createProperty(NS + "observes");
	public static final Property isSingularMeasureOf = m.createProperty(NS + "isSingularMeasureOf");
}

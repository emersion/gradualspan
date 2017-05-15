package fr.emersion.jenaplayground;

import java.io.InputStream;
import org.apache.jena.util.FileManager;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

/**
 * Hello world!
 *
 */
public class App
{
    public static void main( String[] args )
    {
        Model model = ModelFactory.createDefaultModel();
        InputStream in = FileManager.get().open("vc-db-1.rdf");
        model.read(in, null);
        model.write(System.out);
    }
}

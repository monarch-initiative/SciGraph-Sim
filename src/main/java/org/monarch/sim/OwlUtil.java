package org.monarch.sim;

import java.util.Collection;

import edu.sdsc.scigraph.neo4j.OntologyConfiguration;
import edu.sdsc.scigraph.owlapi.OwlLoadConfiguration;
import edu.sdsc.scigraph.owlapi.OwlLoader;

public class OwlUtil {

	/***
	 * Load an OWL import chain.
	 * 
	 * @param url The root URL of the import chain ("http://www.w3.org/TR/owl-guide/wine.rdf")
	 * @param graphLocation The location on disk to store the graph ("target/wine" might be good for
	 *        testing)
	 */
	public static void loadOntology(String url, String graphLocation) {
		OntologyConfiguration ontologyConfiguration = new OntologyConfiguration();
		ontologyConfiguration.setGraphLocation(graphLocation);
		OwlLoadConfiguration config = new OwlLoadConfiguration();
		config.setOntologyConfiguration(ontologyConfiguration);
		config.getOntologyUrls().add(url);
		OwlLoader.load(config);
	}
	
	/***
	 * Load an OWL import chain.
	 * 
	 * @param urls The root URLs of the import chain ("http://www.w3.org/TR/owl-guide/wine.rdf")
	 * @param graphLocation The location on disk to store the graph ("target/wine" might be good for
	 *        testing)
	 */
	public static void loadOntology(Collection<String> urls, String graphLocation) {
		OntologyConfiguration ontologyConfiguration = new OntologyConfiguration();
		ontologyConfiguration.setGraphLocation(graphLocation);
		OwlLoadConfiguration config = new OwlLoadConfiguration();
		config.setOntologyConfiguration(ontologyConfiguration);
		for (String url : urls)
		{
			config.getOntologyUrls().add(url);
		}
		OwlLoader.load(config);
	}

}

package org.monarch.sim;

import java.util.HashMap;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.tooling.GlobalGraphOperations;

public class MappedDB {
	
	protected GraphDatabaseService db;
	protected HashMap<String, Node> fragmentMap;
	
	public MappedDB(String url, String graphLocation, boolean forceRebuild) {
		GraphFactory factory = new GraphFactory();
		db = factory.buildOntologyDB(url, graphLocation, forceRebuild);
		getFragmentMap();
	}
	
	public MappedDB(GraphDatabaseService db) {
		this.db = db;
		getFragmentMap();
	}
	
	private void getFragmentMap() {
		fragmentMap = new HashMap<>();
		
		for (Node n : GlobalGraphOperations.at(db).getAllNodes())
		{
			if (n.hasProperty("fragment"))
			{
				fragmentMap.put(makeCanonicalFragment((String) n.getProperty("fragment")), n);
			}
		}
	}
	
	protected String makeCanonicalFragment(String data) {
		return data.replaceAll("_", ":");
	}
	
	public String nodeToString(Node n) {
		if (n == null)
		{
			return "null";
		}
		
		// String together the appropriate properties.
		String str = "";
		if (n.hasProperty("fragment"))
		{
			str += makeCanonicalFragment((String) n.getProperty("fragment")) + " ";
		}
		if (n.hasProperty("http://www.w3.org/2000/01/rdf-schema#label"))
		{
			str += n.getProperty("http://www.w3.org/2000/01/rdf-schema#label") + " ";
		}
		
		// If we didn't find the desired properties, just use Neo4j's default representation.
		if (str.equals(""))
		{
			str = n.toString();
		}
		
		return str;
	}
	
	public Node getNodeByFragment(String fragment) {
		return fragmentMap.get(makeCanonicalFragment(fragment));
	}
	
	public void close() {
		db.shutdown();
	}

}

package org.monarch.sim;

import java.util.HashMap;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.tooling.GlobalGraphOperations;

public abstract class MappedDB {
	
	static GraphDatabaseService db;
	static HashMap<String, Node> fragmentMap;
	
	public MappedDB(String url, String graphLocation, boolean forceBuild) {
		GraphFactory factory = new GraphFactory();
		// If we were told to rebuild or the target location is empty, build from
		// the given url.
		if (forceBuild /* FIXME: Check if location is already in use. */)
		{
			db = factory.buildOntologyDB(url, graphLocation);
		}
		// Otherwise, we've already got the graph stored, and we can use that.
		else
		{
			db = factory.loadOntologyDB(graphLocation);
		}
		getPropertyMap("fragment");
	}
	
	public MappedDB(GraphDatabaseService oldDB) {
		db = oldDB;
		getPropertyMap("fragment");
	}
	
	private void getPropertyMap(String property) {
		fragmentMap = new HashMap<>();
		
		for (Node n : GlobalGraphOperations.at(db).getAllNodes())
		{
			if (n.hasProperty(property))
			{
				fragmentMap.put(makeCanonical(property, (String)n.getProperty(property)), n);
			}
		}
	}
	
	protected String makeCanonical(String property, String data) {
		if (property.equals("fragment"))
		{
			return data.replaceAll("_", ":");
		}
		// TODO: If we want to handle other data, this is the place to do it.
		return data;
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
			str += makeCanonical("fragment", (String)n.getProperty("fragment")) + " ";
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
		return fragmentMap.get(makeCanonical("fragment", fragment));
	}
	
	public void close() {
		db.shutdown();
	}

}

package org.monarch.sim;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.tooling.GlobalGraphOperations;

public class PathFinder {
	
	static GraphDatabaseService db;
	static HashMap<String, Node> map;
	
	public PathFinder(String url, String graphLocation, boolean forceLoad) {
		GraphFactory factory = new GraphFactory();
		if (forceLoad /* FIXME: Check if location is already in use. */)
		{
			db = factory.buildOntologyDB(url, graphLocation);
		}
		else
		{
			db = factory.loadOntologyDB(graphLocation);
		}
		getPropertyMap("fragment");
	}
	
	public Iterable<String []> getPairs(String filename) {
		ArrayList<String []> pairs = new ArrayList<>();
		
		try
		{
			// Get the lines from the appropriate file.
			BufferedReader reader = new BufferedReader(new FileReader(filename));
			String line;
			while ((line = reader.readLine()) != null)
			{
				// Pull out the two columns we care about.
				String [] pieces = line.split("\t");
				String [] pair = new String [2];
				pair[0] = pieces[0];
				pair[1] = pieces[3];
				pairs.add(pair);
			}
			reader.close();
		}
		catch (Exception e)
		{
			System.out.println(e.getMessage());
		}
		
		return pairs;
	}
	
	private void getPropertyMap(String property) {
		map = new HashMap<>();
		
		for (Node n : GlobalGraphOperations.at(db).getAllNodes())
		{
			if (n.hasProperty(property))
			{
				map.put(makeCanonical(property, (String)n.getProperty(property)), n);
			}
		}
	}
	
	public Node getLCS(String [] pair) {
		Node first = map.get(makeCanonical("fragment", pair[0]));
		Node second = map.get(makeCanonical("fragment", pair[1]));
		
		// Check that both nodes exist.
		boolean failed = false;
		if (first == null)
		{
			System.out.println("Node " + pair[0] + " does not exist.");
			failed = true;
		}
		if (second == null)
		{
			System.out.println("Node " + pair[1] + " does not exist.");
			failed = true;
		}
		if (failed)
		{
			return null;
		}
		
		System.out.println(nodeToString(first) + " ancestors:");
		for (Node n : Neo4jTraversals.getAncestors(first))
		{
			System.out.println(nodeToString(n));
		}
		System.out.println();
		System.out.println(nodeToString(second) + " ancestors:");
		for (Node n : Neo4jTraversals.getAncestors(second))
		{
			System.out.println(nodeToString(n));
		}
		System.out.println();
		
		return Neo4jTraversals.getLCS(first, second);
	}
	
	private String makeCanonical(String property, String data) {
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
		
		String str = "";
		if (n.hasProperty("fragment"))
		{
			str += makeCanonical("fragment", (String)n.getProperty("fragment")) + " ";
		}
		if (n.hasProperty("http://www.w3.org/2000/01/rdf-schema#label"))
		{
			str += n.getProperty("http://www.w3.org/2000/01/rdf-schema#label") + " ";
		}
		
		if (str.equals(""))
		{
			str = n.toString();
		}
		
		return str;
	}
	
	public Node getNodeByFragment(String fragment) {
		return map.get(makeCanonical("fragment", fragment));
	}
	
	public void close() {
		db.shutdown();
	}

}

package org.monarch.sim;

import java.io.BufferedReader;
import java.io.Console;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.tooling.GlobalGraphOperations;

public class Paths {
	
	static GraphDatabaseService db;
	static HashMap<String, Node> map;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		TestGraphFactory factory = new TestGraphFactory();
//		db = factory.buildOntologyDB("http://purl.obolibrary.org/obo/upheno/monarch.owl", "target/monarch");
		// FIXME: Find some way to do this more programatically.
		db = factory.loadOntologyDB("target/monarch");
		getPropertyMap("fragment");
		
		System.out.println("Finished Setup");
		System.out.println();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		db.shutdown();
	}
	
	private static Iterable<String []> getPairs(String filename) throws Exception {
		ArrayList<String []> pairs = new ArrayList<>();
		
		BufferedReader reader = new BufferedReader(new FileReader(filename));
		String line;
		while ((line = reader.readLine()) != null)
		{
			// Pull out the two pieces we care about.
			String [] pieces = line.split("\t");
			String [] pair = new String [2];
			pair[0] = pieces[0];
			pair[1] = pieces[3];
			pairs.add(pair);
		}
		reader.close();
		
		return pairs;
	}
	
	private static void getPropertyMap(String property) {
		map = new HashMap<>();
		
		for (Node n : GlobalGraphOperations.at(db).getAllNodes())
		{
			if (n.hasProperty(property))
			{
				map.put(makeCanonical(property, (String)n.getProperty(property)), n);
			}
		}
	}
	
	private static Node getLCS(String [] pair) {
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
		
		System.out.println(makeString(first) + " ancestors:");
		for (Node n : Neo4jTraversals.getAncestors(first))
		{
			System.out.println(makeString(n));
		}
		System.out.println();
		System.out.println(makeString(second) + " ancestors:");
		for (Node n : Neo4jTraversals.getAncestors(second))
		{
			System.out.println(makeString(n));
		}
		System.out.println();
		
		return Neo4jTraversals.getLCS(first, second);
	}
	
	private static String makeCanonical(String property, String data) {
		if (property.equals("fragment"))
		{
			return data.replaceAll("_", ":");
		}
		// TODO: If we want to handle other data, this is the place to do it.
		return data;
	}
	
	private static String makeString(Node n) {
		String str = "";
		if (n.hasProperty("fragment"))
		{
			str += n.getProperty("fragment") + " ";
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
	
	@Test
	public void test() {
		try
		{
			for (String [] pair : getPairs("HPGO.tsv"))
			{
				System.out.println(getLCS(pair));
				System.out.println();
			}
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}

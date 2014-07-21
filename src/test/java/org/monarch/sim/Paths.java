package org.monarch.sim;

import java.io.BufferedReader;
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

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		TestGraphFactory factory = new TestGraphFactory();
		db = factory.buildOntologyDB("http://purl.obolibrary.org/obo/upheno/monarch.owl", "target/monarch");
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		db.shutdown();
	}
	
	private Iterable<String []> getPairs(String filename) throws Exception {
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
	
	private HashMap<String, Node> getPropertyMap(String property, GraphDatabaseService db) {
		HashMap<String, Node> map = new HashMap<>();
		
		for (Node n : GlobalGraphOperations.at(db).getAllNodes())
		{
			if (n.hasProperty(property))
			{
				map.put((String)n.getProperty(property), n);
			}
		}
		
		return map;
	}
	

	@Test
	public void test() {
		System.out.println(getPropertyMap("fragment", db));
	}

}

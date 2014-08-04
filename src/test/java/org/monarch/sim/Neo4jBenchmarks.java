package org.monarch.sim;

import java.util.Collection;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.tooling.GlobalGraphOperations;

import com.carrotsearch.junitbenchmarks.BenchmarkRule;

public class Neo4jBenchmarks {
	
	@Rule
	public TestRule benchmarkRun = new BenchmarkRule();
	
	static GraphDatabaseService testDB;
	static Node root;
	static SciGraphTraverser traverser;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		TestGraphFactory factory = new TestGraphFactory();
		testDB = factory.buildOntologyDB("http://purl.obolibrary.org/obo/upheno/monarch.owl", "target/monarch", false);
//		testDB = factory.buildCompleteDB(1200);
		traverser = new SciGraphTraverser(testDB);
//		traverser.relationships("SUBCLASS_OF");
		root = testDB.getNodeById(71805);
//		root = getRoot(testDB);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		testDB.shutdown();
	}
	
	public static Node getRoot(GraphDatabaseService db) {
		Node first = null;
		for (Node n : GlobalGraphOperations.at(db).getAllNodes())
		{
			if (n.getId() != 0)
			{
				first = n;
				break;
			}
		}
		
		Collection<Node> parents = Neo4jTraversals.getParents(first);
		while (!parents.isEmpty())
		{
			first = parents.iterator().next();
			parents = Neo4jTraversals.getParents(first);
		}
		
		return first;
	}
	
	public void getAllByAll(GraphDatabaseService db) {
		// FIXME: This should actually return something.
		Iterable<Node> nodes = GlobalGraphOperations.at(db).getAllNodes();
		for (Node first : nodes)
		{
			if (first.getId() == 0)
			{
				continue;
			}
			for (Node second : nodes)
			{
				if (second.getId() <= first.getId())
				{
					continue;
				}
				Neo4jTraversals.getLCS(first, second);
			}
		}
	}

	@Test
	public void naiveTest() {
		System.out.println("Starting naive test");
		int count = 0;
		for (Node n : Neo4jTraversals.getDescendants(root))
		{
			count++;
		}
		System.out.println(count);
		System.out.println("Found all descendants");
	}
	
	@Test
	public void traverserTest() {
		System.out.println("Starting traverser test");
		int count = 0;
		for (Node n : traverser.getDescendants(root))
		{
			count++;
		}
		System.out.println(count);
		System.out.println("Found all descendants");
	}
	
//	@Test
//	public void setICTest() {
//		Neo4jTraversals.setAllIC(testDB);
//	}

}

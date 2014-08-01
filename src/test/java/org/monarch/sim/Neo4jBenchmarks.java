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

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		TestGraphFactory factory = new TestGraphFactory();
		testDB = factory.buildOntologyDB("http://purl.obolibrary.org/obo/upheno/monarch.owl", "target/monarch", false);
//		testDB = factory.buildCompleteDB(300);
		root = getRoot(testDB);
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
	public void test() {
//		getAllByAll(testDB);
		Neo4jTraversals.getDescendants(root);
	}
	
	@Test
	public void setICTest() {
		Neo4jTraversals.setAllIC(testDB);
	}

}

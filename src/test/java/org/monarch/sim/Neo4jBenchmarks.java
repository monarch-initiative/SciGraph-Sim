package org.monarch.sim;

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

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		GraphFactory factory = new GraphFactory();
		testDB = factory.buildCompleteDB(256);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		testDB.shutdown();
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
		getAllByAll(testDB);
	}

}

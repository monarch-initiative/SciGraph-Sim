package org.monarch.sim;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.tooling.GlobalGraphOperations;

public class TraverserTest {

	static GraphDatabaseService db;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		db = new TestGraphFactory().buildTreeDB(15);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		db.shutdown();
	}

	@Test
	public void test() {
		SciGraphTraverser traverser = new SciGraphTraverser(db);
		traverser.relationships("SUBCLASS_OF", Direction.INCOMING);
		for (Node baseNode : GlobalGraphOperations.at(db).getAllNodes())
		{
			System.out.println(baseNode + ":");
			for (Node node : traverser.getParents(baseNode))
			{
				System.out.println(node);
			}
			System.out.println();
		}
	}

}

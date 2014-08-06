package org.monarch.sim;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;

public class TraverserTest {

	static GraphDatabaseService db;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		db = new TestGraphFactory().buildCompleteDB(255);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		db.shutdown();
	}

	@Test
	public void test() {
		SciGraphTraverser traverser = new SciGraphTraverser(db, "test");
		traverser.relationships("SUBCLASS_OF", Direction.INCOMING);
		Node first = db.getNodeById(42);
		Node second = db.getNodeById(234);
		System.out.println(traverser.getIC(first));
		System.out.println(traverser.getIC(second));
		System.out.println(traverser.getLCS(first, second));
	}

}

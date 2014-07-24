package org.monarch.sim;

import java.util.ArrayList;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;

public class PhevorTest {
	
	static Phevor phevor;
	static GraphDatabaseService db;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		db = new GraphFactory().buildCompleteDB(16);
		phevor = new Phevor(db);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		phevor.close();
	}

	@Test
	public void test() {
		ArrayList<Node> baseNodes = new ArrayList<>();
		baseNodes.add(db.getNodeById(8));
		phevor.setBaseNodes(baseNodes);
		for (int i = 1; i <= 16; i++)
		{
			ArrayList<Node> other = new ArrayList<Node>();
			other.add(db.getNodeById(i));
			System.out.println("Node " + i + ":");
			System.out.println("Score: " + phevor.compareOtherNodes(other));
			System.out.println();
		}
	}

}

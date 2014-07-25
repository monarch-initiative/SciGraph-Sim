package org.monarch.sim;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;

public class PhevorTest {
	
	static Phevor phevor;
	static Collection<GraphDatabaseService> dbs;
	static GraphDatabaseService completeDB;
	static GraphDatabaseService treeDB;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		GraphFactory factory = new GraphFactory();
		dbs = new ArrayList<>();
		completeDB = factory.buildCompleteDB(15);
		dbs.add(completeDB);
		treeDB = factory.buildTreeDB(15);
		dbs.add(treeDB);
		ArrayList<String []> links = new ArrayList<>();
		phevor = new Phevor(dbs, links);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		phevor.close();
	}

	@Test
	public void test() {
		ArrayList<String> baseNodes = new ArrayList<>();
		baseNodes.add((String) completeDB.getNodeById(8).getProperty("fragment"));
		baseNodes.add((String) treeDB.getNodeById(8).getProperty("fragment"));
		phevor.setBaseNodes(baseNodes);
		for (int i = 1; i <= 15; i++)
		{
			ArrayList<String> other = new ArrayList<>();
			other.add((String) completeDB.getNodeById(i).getProperty("fragment"));
			System.out.println("Node COMPLETE:" + i);
			System.out.println("Score: " + phevor.compareOtherNodes(other));
			System.out.println();
		}
		for (int i = 1; i <= 15; i++)
		{
			ArrayList<String> other = new ArrayList<>();
			other.add((String) treeDB.getNodeById(i).getProperty("fragment"));
			System.out.println("Node TREE:" + i);
			System.out.println("Score: " + phevor.compareOtherNodes(other));
			System.out.println();
		}
	}

}

package org.monarch.sim;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;

public class NaiveTraverserIT {

	static GraphDatabaseService db;
	static MappedDB mapped;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		db = new TestGraphFactory().buildCompleteDB(10000);
		mapped = new MappedDB(db);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		mapped.close();
	}

	@Test
	public void test() {
		long start, end;
		Node base = mapped.getNodeByFragment("COMPLETE:4");
		NaiveTraverser traverser = new NaiveTraverser(db, "test");
		
		System.out.println("STARTING");
		start = System.nanoTime();
		traverser.pushAllNodes();
		end = System.nanoTime();
		System.out.println((end - start) / 1_000_000);
		System.out.println(traverser.getIC(base));
		System.out.println("FINISHED");
//		
//		System.out.println("CHILDREN:");
//		for (Node n : traverser.getChildren(base))
//		{
//			System.out.println(mapped.nodeToString(n));
//		}
//		System.out.println();
//		
//		System.out.println("PARENTS:");
//		for (Node n : traverser.getParents(base))
//		{
//			System.out.println(mapped.nodeToString(n));
//		}
//		System.out.println();
//		
//		System.out.println("DESCENDANTS:");
//		for (Node n : traverser.getDescendants(base))
//		{
//			System.out.println(mapped.nodeToString(n));
//		}
//		System.out.println();
//		
//		System.out.println("ANCESTORS:");
//		for (Node n : traverser.getAncestors(base))
//		{
//			System.out.println(mapped.nodeToString(n));
//		}
//		System.out.println();
//		
//		Node other = mapped.getNodeByFragment("COMPLETE:10");
//		System.out.println("LCS:");
//		System.out.println(mapped.nodeToString(traverser.getDummyLCS(base, other)));
	}

}

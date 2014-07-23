package org.monarch.sim;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.graphdb.Node;

public class PathTest {

	static PathFinder monarchPathFinder;
	static PathFinder retinaPathFinder;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
//		monarchPathFinder = new PathFinder("http://purl.obolibrary.org/obo/upheno/monarch.owl",
//				"target/monarch", false);
//		System.out.println("Finished Monarch Setup");
		retinaPathFinder = new PathFinder(new File("src/test/resources/ontologies/retina-test.owl").getAbsolutePath(),
				"target/retina", true);
		System.out.println("Finished Retina Setup");
		System.out.println();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		//monarchPathFinder.close();
		retinaPathFinder.close();
	}
	
	@Test
	public void test() {
//		try
//		{
//			for (String [] pair : monarchPathFinder.getPairs("HPGO.tsv"))
//			{
//				System.out.println(monarchPathFinder.nodeToString(monarchPathFinder.getLCS(pair)));
//				System.out.println();
//			}
//		}
//		catch (Exception e)
//		{
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		Node first = retinaPathFinder.getNodeByFragment("HP:0000479");
		Node second = retinaPathFinder.getNodeByFragment("GO:0060041");
		System.out.println(retinaPathFinder.nodeToString(first) + ", " + retinaPathFinder.nodeToString(second));
		String [] pair = {"HP:0000479", "GO:0060041"};
		System.out.println(retinaPathFinder.getLCS(pair));
	}
	
	@Test
	public void ancestorsTest() {
		Iterable<Node> ancs = Neo4jTraversals.getAncestors(retinaPathFinder.getNodeByFragment("GO:0060041"));
		boolean found = false;
		for (Node n : ancs) {
			if (n.equals(retinaNode()))
				found = true;
		}
		assertTrue(found);
	}
	
	private Node retinaNode() {
		return retinaPathFinder.getNodeByFragment("UBERON:0000966");
	}

}

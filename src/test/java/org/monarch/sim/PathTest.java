package org.monarch.sim;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.graphdb.Node;

import com.tinkerpop.pipes.util.structures.Pair;

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
//		monarchPathFinder.close();
		retinaPathFinder.close();
	}
	
	@Test
	public void test() {
//		try
//		{
//			for (String [] pair : monarchPathFinder.getPairs("HPGO.tsv"))
//			{
//				System.out.println(pair[0] + ", " + pair[1]);
//				System.out.println(monarchPathFinder.nodeToString(monarchPathFinder.getLCS(pair)));
//				System.out.println();
//			}
//		}
//		catch (Exception e)
//		{
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		String firstFragment = "HP:0000479";
		Node first = retinaPathFinder.getNodeByFragment(firstFragment);
		String secondFragment = "GO:0060041";
		Node second = retinaPathFinder.getNodeByFragment(secondFragment);
		System.out.println(retinaPathFinder.nodeToString(first) + ", " + retinaPathFinder.nodeToString(second));
		Pair<String, String> pair = new Pair<>(firstFragment, secondFragment);
		Node lcs = retinaPathFinder.getLCS(pair);
		String lcsFragment = (String) lcs.getProperty("fragment");
		System.out.println(retinaPathFinder.nodeToString(lcs));
		System.out.println(retinaPathFinder.getShortestPath(firstFragment, lcsFragment));
		System.out.println(retinaPathFinder.getShortestPath(secondFragment, lcsFragment));
	}
	
	@Test
	public void ancestorsTest() {
		Iterable<Node> ancs = Neo4jTraversals.getAncestors(retinaPathFinder.getNodeByFragment("GO:0060041"));
		assertThat("Retina should be an ancestor", ancs, hasItem(retinaNode()));
	}

	private Node retinaNode() {
		return retinaPathFinder.getNodeByFragment("UBERON:0000966");
	}

}

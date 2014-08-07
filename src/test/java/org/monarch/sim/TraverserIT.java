package org.monarch.sim;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.tooling.GlobalGraphOperations;

public class TraverserIT {

	static GraphDatabaseService db;
	static MappedDB mapped;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		db = new GraphFactory().buildOntologyDB("http://purl.obolibrary.org/obo/upheno/monarch.owl",
				"target/monarch", false);
		mapped = new MappedDB(db);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		mapped.close();
		db.shutdown();
	}
	
	private void printPath(List<Node> path) {
		LinkedList<Node> copy = new LinkedList<>(path);
		Node prev = copy.removeFirst();
		System.out.println("~~ " + mapped.nodeToString(prev) + "~~");
		
		// Find all edges between each pair of successive nodes.
		while (!copy.isEmpty())
		{
			Node next = copy.removeFirst();
			for (Relationship edge : prev.getRelationships())
			{
				if (edge.getOtherNode(prev).equals(next))
				{
					Node start = edge.getStartNode();
					Node end = edge.getEndNode();
					String toPrint = "";
					toPrint += mapped.nodeToString(start);
					toPrint += "--";
					toPrint += edge.getType();
					toPrint += "--> ";
					toPrint += mapped.nodeToString(end);
					System.out.println(toPrint);
				}
			}
			System.out.println("~~ " + mapped.nodeToString(next) + "~~");
			prev = next;
		}
	}
	
	@Test
	public void test() throws IOException {
		SciGraphTraverser traverser = new SciGraphTraverser(db, "test");
		for (RelationshipType rt : GlobalGraphOperations.at(db).getAllRelationshipTypes())
		{
			if (!rt.name().equals("SUPERCLASS_OF"))
			{
				traverser.relationships(rt.name(), Direction.INCOMING);
			}
		}
		
		BufferedReader reader = new BufferedReader(new FileReader("HPGO_non_obvious.tsv"));
		String line;
		while ((line = reader.readLine()) != null)
		{
			String [] pieces = line.split("\t");
			Node first = mapped.getNodeByFragment(pieces[0]);
			Node second = mapped.getNodeByFragment(pieces[3]);
			
			if (first == null)
			{
				System.out.println("Node " + pieces[0] + " does not exist.");
				continue;
			}
			if (second == null)
			{
				System.out.println("Node " + pieces[3] + " does not exist.");
				continue;
			}
			
			Node lcs = traverser.getDummyLCS(first, second);
			System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
			System.out.println(mapped.nodeToString(first));
			System.out.println(mapped.nodeToString(second));
			System.out.println("LCS: " + mapped.nodeToString(lcs));
			System.out.println();

			// FIXME: Add shortest path calculation to traverser.
			List<Node> firstPath = Neo4jTraversals.getShortestPath(first, lcs);
			List<Node> secondPath = Neo4jTraversals.getShortestPath(second, lcs);
			printPath(firstPath);
			System.out.println();
			printPath(secondPath);
			System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
		}
		reader.close();
	}

}

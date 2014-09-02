package org.monarch.sim;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.tooling.GlobalGraphOperations;

public class NaiveTraverserIT {

	static GraphDatabaseService db;
	static MappedDB mapped;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		db = new GraphFactory().buildOntologyDB("http://purl.obolibrary.org/obo/upheno/monarch.owl",
				"target/monarch", false);
//		db = new TestGraphFactory().buildEquivDB();
		mapped = new MappedDB(db);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		mapped.close();
	}
	
	public Collection<String []> getPairs(String filename) throws IOException {
		Collection<String []> pairs = new ArrayList<>();
		
		// Get the lines from the appropriate file.
		BufferedReader reader = new BufferedReader(new FileReader(filename));
		String line;
		while ((line = reader.readLine()) != null)
		{
			// Pull out the two columns we care about.
			String [] pieces = line.split("\t");
			String [] pair = {pieces[0], pieces[3]};
			pairs.add(pair);
		}
		reader.close();
		
		return pairs;
	}
	
	public void printPairInfo(String [] pair, NaiveTraverser traverser) {
		Node first = mapped.getNodeByFragment(pair[0]);
		Node second = mapped.getNodeByFragment(pair[1]);
		if (first == null)
		{
			System.out.println(pair[0] + " does not exist");
			return;
		}
		if (second == null)
		{
			System.out.println(pair[1] + " does not exist");
			return;
		}
		Node lcs = traverser.getLCS(first, second);
		
		
		System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
		System.out.println(mapped.nodeToString(first) + " --  IC = " + traverser.getIC(first));
		System.out.println(mapped.nodeToString(second) + " --  IC = " + traverser.getIC(second));
		System.out.println();
		System.out.println("LCS:");
		if (lcs == null)
		{
			System.out.println("No LCS found");
			return;
		}
		else
		{
			System.out.println(mapped.nodeToString(lcs) + " --  IC = " + traverser.getIC(lcs));
		}
		System.out.println();
		
		List<Node> firstPath = traverser.getShortestPath(first, lcs);
		List<Node> secondPath = traverser.getShortestPath(second, lcs);
		System.out.println(""
				+ mapped.nodeToString(first)
				+ "--[" + (firstPath.size() - 1) + " edges]--> "
				+ mapped.nodeToString(lcs)
				+ "<--[" + (secondPath.size() - 1) + " edges]-- "
				+ mapped.nodeToString(second)
				);
		System.out.println((firstPath.size() + secondPath.size() - 2) + " edges total");
		
		System.out.println();
		printPath(firstPath);
		System.out.println();
		printPath(secondPath);
		
//		Set<Node> commonAncestors = traverser.getAncestors(first);
//		commonAncestors.retainAll(traverser.getAncestors(second));
//		for (Node n : commonAncestors)
//		{
//			System.out.println(mapped.nodeToString(n) + " --  IC = " + traverser.getIC(n));
//		}
		System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
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
		Set<String> toDelete = new HashSet<>();
		toDelete.add("SUPERCLASS_OF");
		toDelete.add("PHEVOR_SUBCLASS_OF");
		GraphUtil.cleanEdges(db, toDelete);
		
//		GraphUtil.fixEquivalent(db);
		
//		for (Node n : GlobalGraphOperations.at(db).getAllNodes())
//		{
//			if (n.getId() == 0)
//			{
//				continue;
//			}
//			System.out.println(n.getProperty("fragment"));
//		}
//		System.out.println();
//		for (Relationship e : GlobalGraphOperations.at(db).getAllRelationships())
//		{
//			System.out.println(e.getStartNode().getProperty("fragment")
//					+ " --" + e.getType().name() + "--> "
//					+ e.getEndNode().getProperty("fragment"));
//		}
//		System.out.println();
		
		System.out.println("~~~ EDGE TYPES ~~~");
		for (RelationshipType type : GlobalGraphOperations.at(db).getAllRelationshipTypes())
		{
			System.out.println(type.name());
		}
		System.out.println("~~~~~~~~~~~~~~~~~~");
		
//		long start, end;
		NaiveTraverser traverser = new NaiveTraverser(db, "test");
		Set<String> excluded = new HashSet<>();
		excluded.add("SUPERCLASS_OF");
		excluded.add("DISJOINT_WITH");
		excluded.add("PHEVOR_SUBCLASS_OF");
		excluded.add("PROPERTY");
		excluded.add("inSubset");
		traverser.excludeEdgeTypes(excluded);
		
		Set<String> ignored = new HashSet<>();
		System.out.println("Pushing...");
		ignored.add("EQUIVALENT_TO");
		traverser.pushAllNodes(ignored);
		System.out.println("Finished pushing");
		
		traverser.excludeEdgeTypes(excluded);
		Collection<String []> pairs = getPairs("HPGO_non_obvious.tsv");
		for (String [] pair : pairs)
		{
			printPairInfo(pair, traverser);
		}
		
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

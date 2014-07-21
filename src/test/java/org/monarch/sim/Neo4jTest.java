package org.monarch.sim;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.monarch.sim.Neo4jTraversals;
import org.monarch.sim.TestGraphFactory;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.tooling.GlobalGraphOperations;

import com.google.common.collect.Lists;

public class Neo4jTest {

	// ^-shaped graph.
	static GraphDatabaseService waterDB;
	// Complete graph.
	static GraphDatabaseService completeDB;
	// Balanced binary tree.
	static GraphDatabaseService treeDB;
	// A directed cycle with one edge reversed.
	static GraphDatabaseService cycleDB;
	// Graph from monarchGraph folder.
	static GraphDatabaseService monarchDB;	
	// Graph of wines.
	static GraphDatabaseService wineDB;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		TestGraphFactory factory = new TestGraphFactory();
		waterDB = factory.buildWaterDB();
		completeDB = factory.buildCompleteDB(16);
		treeDB = factory.buildTreeDB(15);	
		cycleDB = factory.buildCycleDB();
		monarchDB = factory.buildMonarchDB();
//		wineDB = factory.buildWineDB();
	}
	
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		// Clean up the databases.
		waterDB.shutdown();
		completeDB.shutdown();
		treeDB.shutdown();
		cycleDB.shutdown();
		monarchDB.shutdown();
//		wineDB.shutdown();
	}

	private static Iterable<String> getProperties(GraphDatabaseService db) {
		LinkedList<String> properties = new LinkedList<>();

		// Find the first node with properties.
		for (Node node : GlobalGraphOperations.at(db).getAllNodes())
		{
			boolean found = false;

			for (String property : node.getPropertyKeys())
			{
				// Get all the properties.
				properties.add(property);
				found = true;
			}

			// Once we've found a nontrivial node, stop.
			if (found)
			{
				break;
			}
		}

		return properties;
	}

	private void validateDB(GraphDatabaseService db) {
		validateDBNodes(db);
		System.out.println();
		validateDBPairwise(db);
	}

	private void validateDBNodes(GraphDatabaseService db) {
		Iterable<Node> nodes = GlobalGraphOperations.at(db).getAllNodes();
		for (Node node : nodes)
		{
			if (node.hasProperty("name"))
			{
				System.out.println("NODE: " + node.getProperty("name"));
				System.out.println("IC: " + Neo4jTraversals.getIC(node));
				System.out.println("Parents:");
				for (Node parent : Neo4jTraversals.getParents(node))
				{
					System.out.println(parent.getProperty("name"));
				}
				System.out.println("Children:");
				for (Node child : Neo4jTraversals.getChildren(node))
				{
					System.out.println(child.getProperty("name"));
				}
				System.out.println("Ancestors:");
				for (Node ancestor : Neo4jTraversals.getAncestors(node))
				{
					System.out.println(ancestor.getProperty("name"));
				}
				System.out.println("Descendants:");
				for (Node descendant : Neo4jTraversals.getDescendants(node))
				{
					System.out.println(descendant.getProperty("name"));
				}
				System.out.println();
			}
		}
	}

	private void validateDBPairwise(GraphDatabaseService db) {
		Iterable<Node> nodes = GlobalGraphOperations.at(db).getAllNodes();
		for (Node first : nodes)
		{
			if (!first.hasProperty("name"))
			{
				continue;
			}
			String first_name = (String)first.getProperty("name");
			for (Node second : nodes)
			{
				if (!second.hasProperty("name"))
				{
					continue;
				}
				String second_name = (String)second.getProperty("name");
				if (first_name.compareTo(second_name) > 0)
				{
					continue;
				}
				System.out.println("NODES: " + first_name + " " + second_name);
				System.out.println("Common Ancestors:");
				for (Node ancestor : Neo4jTraversals.getCommonAncestors(first, second))
				{
					System.out.println(ancestor.getProperty("name"));
				}
				Node lcs = Neo4jTraversals.getLCS(first, second);
				if (lcs == null)
				{
					System.out.println("LCS: Null");
					System.out.println();
					continue;
				}
				System.out.println("LCS: " + lcs.getProperty("name"));
				System.out.println();
			}
		}
	}
	
	public void validateMonarchDB() {
		// Keep track of some statistics.
		int trials = 10000;
		int totalCommonAncestors = 0;
		HashMap<Node, Integer> subsumerCounts = new HashMap<>();
		int relevantSubsumerCount = 5;
		double totalIC = 0;
		double lcsIC = 0;
		
		// We should get a sense of which subsumer method is faster.
		long ancestorTime = 0;
		long lcsTime = 0;
		long start, end;
		
		// We need to know how many nodes there are to choose one uniformly.
		ArrayList<Node> nodes = Lists.newArrayList(GlobalGraphOperations.at(monarchDB).getAllNodes());
		int count = nodes.size();
		
		for (int i = 0; i < trials; i++)
		{
			// Pick any two nodes.
			Random rand = new Random();
			Node m, n;
			do
			{
				int j = rand.nextInt(count);
				int k = rand.nextInt(count);
				m = nodes.get(j);
				n = nodes.get(k);
			}
			while (m.getId() == 0 || n.getId() == 0);
			totalIC += Neo4jTraversals.getIC(m);
			totalIC += Neo4jTraversals.getIC(n);
			System.out.println("NODES: " + m + " " + n);
			
			// Find all common ancestors.
			System.out.println("Common Ancestors:");
			start = System.nanoTime();
			Iterable<Node> ancestors = Neo4jTraversals.getCommonAncestors(m, n);
			end = System.nanoTime();
			ancestorTime += (end - start);
			for (Node ancestor : ancestors)
			{
				System.out.println(ancestor);
				totalCommonAncestors++;
			}
			
			// Find the LCS.
			start = System.nanoTime();
			Node lcs = Neo4jTraversals.getLCS(m, n);
			end = System.nanoTime();
			lcsTime += (end - start);
			if (!subsumerCounts.containsKey(lcs))
			{
				subsumerCounts.put(lcs, 0);
			}
			subsumerCounts.put(lcs, subsumerCounts.get(lcs) + 1);
			lcsIC += Neo4jTraversals.getIC(lcs);
			System.out.println("LCS: " + lcs);
			System.out.println();			
		}
		
		// Process statistics.
		double averageCommonAncestors = totalCommonAncestors * 1.0 / trials;
		double averageIC = totalIC / (2 * trials);
		double averageLCSIC = lcsIC / trials;
		// This would be trivial in a sensible language.
		List<Entry<Node, Integer>> entries = new LinkedList<>(subsumerCounts.entrySet());
		Collections.sort(entries, new Comparator<Entry<Node, Integer>>()
			{
				public int compare(Entry<Node, Integer> first, Entry<Node, Integer> second) {
					return first.getValue().compareTo(second.getValue());
				}
			});
		Collections.reverse(entries);
		
		// Report to the user.
		System.out.println("Trials: " + trials);
		System.out.println("Milliseconds to compute ancestors: " + ancestorTime / 1000000);
		System.out.println("Milliseconds to compute LCS: " + lcsTime / 1000000);
		System.out.println("Average common ancestors: " + averageCommonAncestors);
		System.out.println("Average IC: " + averageIC);
		System.out.println("Average LCS IC: " + averageLCSIC);
		System.out.println();
		System.out.println("Common LCS nodes:");
		for (Entry<Node, Integer> entry : entries)
		{
			Node key = entry.getKey();
			Integer value = entry.getValue();
			if (value == 1)
			{
				break;
			}
			System.out.println(key + ": " + value + " times");
			if (key.hasProperty("label"))
			{
				System.out.println(key.getProperty("label"));
			}
			System.out.println("IC: " + Neo4jTraversals.getIC(key));
			relevantSubsumerCount--;
			if (relevantSubsumerCount == 0)
			{
				break;
			}
			System.out.println();
		}
	}
	
	@Test
	public void test() {
//		validateDBNodes(treeDB);
		validateMonarchDB();
//		validateDBPairwise(cycleDB);
//		for (Node n : GlobalGraphOperations.at(wineDB).getAllNodes())
//		{
//			if (n.hasProperty("uri"))
//			{
//				System.out.println(n.getProperty("uri"));
//			}
//		}

		GraphDatabaseService db = new TestGraphFactory().buildMPSubsetDB();
		System.out.println(Neo4jTest.getProperties(db));
	}
	
}

package org.monarch.sim;

import org.monarch.sim.Neo4jTraversals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Map.Entry;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.test.TestGraphDatabaseFactory;
import org.neo4j.tooling.GlobalGraphOperations;

public class Neo4jTest {
	
	// Define the relationships we want to use.
	static enum RelTypes implements RelationshipType {
		SUBCLASS,
	}

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
		buildWaterDB();		
		buildCompleteDB(16);
		buildTreeDB(15);	
		buildCycleDB();
		buildMonarchDB();
//		buildWineDB();
	}

	private static void buildWaterDB() {
		// Build a database with three nodes in a ^ configuration.
		waterDB = new TestGraphDatabaseFactory().newImpermanentDatabase();
		Node waterA = addNode(waterDB, "A");
		Node waterB = addNode(waterDB, "B");
		Node waterC = addNode(waterDB, "C");
		addEdge(waterDB, waterA, waterB);
		addEdge(waterDB, waterC, waterB);
		setAllIC(waterDB);
	}
	
	private static void buildCompleteDB(int numNodes) {
		// Build a complete graph with edges directed toward the lower indices.
		completeDB = new TestGraphDatabaseFactory().newImpermanentDatabase();
		ArrayList<Long> ids = new ArrayList<>();
		for (int i = 1; i <= numNodes; i++)
		{
			Node newNode = addNode(completeDB, "" + i);
			for (Long id : ids)
			{
				addEdge(completeDB, newNode, completeDB.getNodeById(id));
			}
			ids.add(newNode.getId());
		}
		setAllIC(completeDB);
	}

	private static void buildTreeDB(int numNodes) {
		// Build a balanced binary tree with edges directed toward the lower indices.
		treeDB = new TestGraphDatabaseFactory().newImpermanentDatabase();
		for (int i = 1; i <= numNodes; i++)
		{
			Node newNode = addNode(treeDB, "" + i);
			if (i != 1)
			{
				addEdge(treeDB, newNode, treeDB.getNodeById(i / 2));
			}
		}
		setAllIC(treeDB);
	}

	private static void buildCycleDB() {
		// Build a small pathological graph.
		cycleDB = new TestGraphDatabaseFactory().newImpermanentDatabase();
		Node a = addNode(cycleDB, "A");
		Node b = addNode(cycleDB, "B");
		Node c = addNode(cycleDB, "C");
		Node d = addNode(cycleDB, "D");
		Node e = addNode(cycleDB, "E");
		addEdge(cycleDB, b, a);
		addEdge(cycleDB, c, b);
		addEdge(cycleDB, d, c);
		addEdge(cycleDB, e, d);
		addEdge(cycleDB, e, a);
		setAllIC(cycleDB);
	}
	
	private static void buildMonarchDB() {
		// Build a database from the monarchGraph folder.
		monarchDB = new TestGraphDatabaseFactory().newImpermanentDatabase();

		// All the edges in the given graph are undirected, so we need to rebuild.
		Transaction tx = monarchDB.beginTx();
		GraphDatabaseService tempDB = new GraphDatabaseFactory().newEmbeddedDatabase("monarchGraph");
		HashMap<Node, Node> map = new HashMap<>();
		for (Node n : GlobalGraphOperations.at(tempDB).getAllNodes())
		{
			if (n.getId() == 0)
			{
				continue;
			}
			Node newNode = addNode(monarchDB, "" + n.getId());
			for (String property : n.getPropertyKeys())
			{
				newNode.setProperty(property, n.getProperty(property));
			}
			map.put(n, newNode);
		}
		tx.success();
		tx.finish();
		
		// FIXME: Remove this once the version on Jenkins works.
		// Node tempRoot = tempDB.getNodeById(1);
		Node tempRoot = null;
		for (Node n : GlobalGraphOperations.at(tempDB).getAllNodes())
		{
			if (tempRoot == null)
			{
				tempRoot = n;
				continue;
			}
			if (n.getId() < tempRoot.getId())
			{
				tempRoot = n;
			}
		}
		
		// Expand outward starting with node 1.
		HashSet<Node> visited = new HashSet<>();
		LinkedList<Node> toExpand = new LinkedList<>();
		toExpand.add(tempRoot);
		while (!toExpand.isEmpty())
		{
			Node next = toExpand.removeFirst();
			if (!visited.add(next))
			{
				continue;
			}
			
			for (Relationship edge : next.getRelationships(Direction.INCOMING))
			{
				Node child = edge.getStartNode();
				if (!visited.contains(child))
				{
					addEdge(monarchDB, map.get(child), map.get(next));
					toExpand.add(child);
				}
			}
		}
		
		// Point all nodes without edges to node 1.
		for (Node n : GlobalGraphOperations.at(monarchDB).getAllNodes())
		{
			boolean found = false;
			for (@SuppressWarnings("unused") Relationship edge : n.getRelationships())
			{
				found = true;
				break;
			}
			if (!found)
			{
				addEdge(monarchDB, n, map.get(tempRoot));
			}
		}
		
		setAllIC(monarchDB);
	}

	private static void buildWineDB() {
		OwlTestUtil.loadOntology("http://www.w3.org/TR/owl-guide/wine.rdf", "target/wine");
		wineDB = new GraphDatabaseFactory().newEmbeddedDatabase("target/wine");
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
	
	private static Node addNode(GraphDatabaseService db) {
		// Wrap a transaction around node creation.
		Transaction tx = db.beginTx();
		Node newNode = db.createNode();
		tx.success();
		tx.finish();
		return newNode;
	}
	
	private static Node addNode(GraphDatabaseService db, String name) {
		// Wrap a transaction around node creation.
		Transaction tx = db.beginTx();
		Node newNode = db.createNode();
		newNode.setProperty("name", name);
		tx.success();
		tx.finish();
		return newNode;
	}
	
	private static Relationship addEdge(GraphDatabaseService db, Node first, Node second)
	{
		// Wrap a transaction around edge creation.
		Transaction tx = db.beginTx();
		Relationship newRel = first.createRelationshipTo(second, RelTypes.SUBCLASS);
		tx.success();
		tx.finish();
		return newRel;		
	}
	
	private static void setAllIC(GraphDatabaseService db) {
		Transaction tx = db.beginTx();
		Iterable<Node> nodes = GlobalGraphOperations.at(db).getAllNodes();
		int totalNodes = IteratorUtil.count(nodes) - 1;
		for (Node n : nodes)
		{
			if (n.getId() == 0)
			{
				continue;
			}
			// FIXME: When we have real data, we should calculate this better.
			int nodesBelow = IteratorUtil.count(Neo4jTraversals.getDescendants(n));
			double ic = - Math.log((double)nodesBelow / totalNodes) / Math.log(2);
			n.setProperty("IC", ic);
		}
		tx.success();
		tx.finish();
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
				System.out.println("IC: " + node.getProperty("IC"));
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
		int count = IteratorUtil.count(GlobalGraphOperations.at(monarchDB).getAllNodes());
		
		for (int i = 0; i < trials; i++)
		{
			// Pick any two nodes.
			Random rand = new Random();
			int j = rand.nextInt(count - 1) + 1;
			int k = rand.nextInt(count - 1) + 1;
			Node m = monarchDB.getNodeById(j);
			Node n = monarchDB.getNodeById(k);
			totalIC += (double)m.getProperty("IC");
			totalIC += (double)n.getProperty("IC");
			System.out.println("NODES: " + m.getProperty("name") + " " + n.getProperty("name"));
			
			// Find all common ancestors.
			System.out.println("Common Ancestors:");
			start = System.nanoTime();
			Iterable<Node> ancestors = Neo4jTraversals.getCommonAncestors(m, n);
			end = System.nanoTime();
			ancestorTime += (end - start);
			for (Node ancestor : ancestors)
			{
				System.out.println(ancestor.getProperty("name"));
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
			lcsIC += (double)lcs.getProperty("IC");
			System.out.println("LCS: " + lcs.getProperty("name"));
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
			System.out.println("Node " + key.getProperty("name") + ": " + value + " times");
			System.out.println(key.getProperty("label"));
			System.out.println("IC: " + key.getProperty("IC"));
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
//		validateDBPairwise(treeDB);
//		for (Node n : GlobalGraphOperations.at(wineDB).getAllNodes())
//		{
//			if (n.hasProperty("uri"))
//			{
//				System.out.println(n.getProperty("uri"));
//			}
//		}
	}
	
}

package org.monarch.sim;

import java.util.Random;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.monarch.sim.Neo4jTraversals.RelTypes;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.test.TestGraphDatabaseFactory;
import org.neo4j.tooling.GlobalGraphOperations;

public class CypherTest {
	
	// Complete graph.
	static GraphDatabaseService completeDB;
	static ExecutionEngine completeEngine;
	// Balanced binary tree.
	static GraphDatabaseService treeDB;
	static ExecutionEngine treeEngine;
	// Augmented binary tree.
	static GraphDatabaseService augTreeDB;
	static ExecutionEngine augTreeEngine;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		buildCompleteDB(16);
		buildTreeDB(15);
	}
	
	public static Node addNode(GraphDatabaseService db) {
		// Wrap a transaction around node creation.
		Transaction tx = db.beginTx();
		Node newNode = db.createNode();
		tx.success();
		tx.finish();
		return newNode;
	}
	
	public static Node addNode(GraphDatabaseService db, String name) {
		// Wrap a transaction around node creation.
		Transaction tx = db.beginTx();
		Node newNode = db.createNode();
		newNode.setProperty("name", name);
		tx.success();
		tx.finish();
		return newNode;
	}
	
	public static Relationship addEdge(GraphDatabaseService db, Node first, Node second)
	{
		// Wrap a transaction around edge creation.
		Transaction tx = db.beginTx();
		Relationship newRel = first.createRelationshipTo(second, RelTypes.SUBCLASS);
		tx.success();
		tx.finish();
		return newRel;		
	}

	private static void buildCompleteDB(int numNodes) {
		completeDB = new TestGraphDatabaseFactory().newImpermanentDatabase();
		completeEngine = new ExecutionEngine(completeDB);
		// Query taken from neo4j.org.
		// Create a temporary central node.
		String query = "CREATE (center {count:0}) ";
		// Build the right number of nodes.
		query += "FOREACH (";
		query += "x in range(1, " + numNodes + ") ";
		query += "| ";
		query += "CREATE (leaf {count:x}), (center)-[:X]->(leaf)";
		query += ") ";
		query += "WITH center ";
		// Connect nodes in the correct direction.
		query += "MATCH (leaf1)<--(center)-->(leaf2) ";
		query += "WHERE id(leaf1) < id(leaf2) ";
		query += "CREATE (leaf1)-[:X]->(leaf2) ";
		query += "WITH center ";
		// Delete the central node and associated edges.
		query += "MATCH (center)-[r]->() ";
		query += "DELETE center, r ";
		
		completeEngine.execute(query);
	}

	private static void buildTreeDB(int numNodes) {
		treeDB = new TestGraphDatabaseFactory().newImpermanentDatabase();
		treeEngine = new ExecutionEngine(treeDB);
		buildAugmentedTree(numNodes, 0, treeDB, treeEngine);
	}
	
	private static void buildAugmentedTree(int numNodes, int extraEdges,
			GraphDatabaseService db, ExecutionEngine engine) {
		// Build a balanced binary tree.
		for (int i = 1; i <= numNodes; i++)
		{
			Node newNode = addNode(db);
			if (i != 1)
			{
				addEdge(db, newNode, db.getNodeById(i / 2));
			}
		}
		
		// Throw in extra edges.
		for (int i = 0; i < extraEdges; i++)
		{
			// Choose two random indexes.
			Random rand = new Random();
			int first = rand.nextInt(numNodes);
			int second = first;
			while (first == second)
			{
				second = rand.nextInt(numNodes);
			}
			
			// Order the indexes correctly.
			if (first < second)
			{
				int temp = first;
				first = second;
				second = temp;
			}
			
			// If the edge already exists, try again.
			Node firstNode = db.getNodeById(first);
			Node secondNode = db.getNodeById(second);
			boolean exists = false;
			for (Relationship edge : GlobalGraphOperations.at(db).getAllRelationships())
			{
				if (edge.getStartNode().equals(firstNode) && edge.getEndNode().equals(secondNode))
				{
					exists = true;
					break;
				}
			}
			if (exists)
			{
				i--;
				continue;
			}
			
			// Connect the associated nodes.
			addEdge(db, firstNode, secondNode);
		}
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		// Clean up all our graphs.
		completeDB.shutdown();
		treeDB.shutdown();
	}
	
	public void benchmarkAugTree() {
		int numNodes = 15;
		int possibleEdges = (numNodes - 1) * (numNodes - 2) / 2;
		int trials = 10;
		int pairsToTry = 100;
		int warmupPairs = 10;
		
		System.out.println(numNodes + " nodes");
		
		// Vary the number of edges to add.
		for (int percent = 0; percent <= 50; percent += 10)
		{
			int extraEdges = percent * possibleEdges / 100;
			long totalTime = 0;
			
			// Repeat to get an average.
			for (int trial = 0; trial < trials; trial++)
			{
				// Build the augmented tree.
				augTreeDB = new TestGraphDatabaseFactory().newImpermanentDatabase();
				augTreeEngine = new ExecutionEngine(augTreeDB);
				buildAugmentedTree(numNodes, extraEdges, augTreeDB, augTreeEngine);
				
				// Try pairs.
				long start, end;
				for (int pairsTried = 0; pairsTried < pairsToTry + warmupPairs; pairsTried++)
				{
					// Get two random nodes.
					Random rand = new Random();
					Node first = augTreeDB.getNodeById(rand.nextInt(numNodes));
					Node second = augTreeDB.getNodeById(rand.nextInt(numNodes));
					
					// Find common ancestors.
					start = System.nanoTime();
					CypherTraversals.getCommonAncestors(first, second, augTreeEngine);
					end = System.nanoTime();
					if (pairsTried >= warmupPairs)
					{
						totalTime += end - start;
					}
				}
				
				// Clean up.
				augTreeDB.shutdown();
			}
			
			// Show the results.
			long avgMilliTime = totalTime / (1000000 * trials);
			System.out.println(percent + "%: " + avgMilliTime + " milliseconds");
		}
	}

	@Test
	public void test() {
//		Iterable<Node> nodes = GlobalGraphOperations.at(completeDB).getAllNodes();
//		for (Node n : nodes)
//		{
//			System.out.println(CypherTraversals.getAncestors(n, completeEngine));
//		}
//		for (Node first : nodes)
//		{
//			long firstId = first.getId();
//			for (Node second : nodes)
//			{
//				long secondId = second.getId();
//				System.out.println(firstId + " " + secondId + ":");
//				System.out.println(CypherTraversals.getCommonAncestors(first, second, completeEngine));
//			}
//		}
		benchmarkAugTree();
//		for (Relationship edge : GlobalGraphOperations.at(completeDB).getAllRelationships())
//		{
//			System.out.println(edge.getStartNode() + "->" + edge.getEndNode());
//		}
	}

}

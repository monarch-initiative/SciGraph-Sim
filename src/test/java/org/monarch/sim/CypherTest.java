package org.monarch.sim;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.test.TestGraphDatabaseFactory;
import org.neo4j.tooling.GlobalGraphOperations;

public class CypherTest {
	
	// Complete graph.
	static GraphDatabaseService completeDB;
	static ExecutionEngine completeEngine;
	// Balanced binary tree.
	static GraphDatabaseService treeDB;
	static ExecutionEngine treeEngine;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		buildCompleteDB(16);
		buildTreeDB(15);
	}

	private static void buildCompleteDB(int i) {
		completeDB = new TestGraphDatabaseFactory().newImpermanentDatabase();
		completeEngine = new ExecutionEngine(completeDB);
		// Query taken from neo4j.org.
		// Create a temporary central node.
		String query = "CREATE (center {count:0}) ";
		// Build the right number of nodes.
		query += "FOREACH (";
		query += "x in range(1, " + i + ") ";
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

	// FIXME: This doesn't work.
	private static void buildTreeDB(int i) {
		treeDB = new TestGraphDatabaseFactory().newImpermanentDatabase();
		treeEngine = new ExecutionEngine(treeDB);
		// Create a root.
		String query = "CREATE (root {count:0}) ";
		// Build the right number of nodes.
		query += "FOREACH (";
		query += "x in range(1, " + i + ") ";
		query += "| ";
		query += "CREATE (leaf {count:x}) ";
		query += ") ";
		// Connect nodes in the correct direction.
		query += "MATCH (a), (b) ";
		query += "WHERE (id(a) = 2 * id(b) OR id(a) = 2 * id(b) + 1) ";
		query += "CREATE (a)-[:X]->(b) ";
		query += "WITH a, b ";
		
		treeEngine.execute(query);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		// Clean up all our graphs.
		completeDB.shutdown();
		treeDB.shutdown();
	}

	@Test
	public void test() {
		Iterable<Node> nodes = GlobalGraphOperations.at(treeDB).getAllNodes();
		for (Node n : nodes)
		{
			System.out.println(CypherTraversals.getAncestors(n, treeEngine));
		}
		for (Node first : nodes)
		{
			long firstId = first.getId();
			for (Node second : nodes)
			{
				long secondId = second.getId();
				System.out.println(firstId + " " + secondId + ":");
				System.out.println(CypherTraversals.getCommonAncestors(first, second, treeEngine));
			}
		}
	}

}

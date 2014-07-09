package org.monarch.sim;

import static org.monarch.sim.Neo4jTraversals.getAncestors;
import static org.monarch.sim.Neo4jTraversals.getChildren;
import static org.monarch.sim.Neo4jTraversals.getCommonAncestors;
import static org.monarch.sim.Neo4jTraversals.getDescendants;
import static org.monarch.sim.Neo4jTraversals.getLCS;
import static org.monarch.sim.Neo4jTraversals.getParents;

import java.util.ArrayList;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.monarch.sim.Neo4jTraversals.RelTypes;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.test.TestGraphDatabaseFactory;
import org.neo4j.tooling.GlobalGraphOperations;

public class Neo4jTest {

	// ^-shaped graph.
	static GraphDatabaseService waterDB;
	// Complete graph.
	static GraphDatabaseService completeDB;
	// Balanced binary tree.
	static GraphDatabaseService treeDB;
	// Graph from monarchGraph folder.
	static GraphDatabaseService monarchDB;	

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		buildWaterDB();		
		buildCompleteDB(16);
		buildTreeDB(15);		
		buildMonarchDB();
	}

	private static void buildWaterDB() {
		// Build a database with three nodes in a ^ configuration.
		waterDB = new TestGraphDatabaseFactory().newImpermanentDatabase();
		Node waterA = addNode(waterDB, "A");
		Node waterB = addNode(waterDB, "B");
		Node waterC = addNode(waterDB, "C");
		addEdge(waterDB, waterA, waterB);
		addEdge(waterDB, waterC, waterB);
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
	}

	private static void buildMonarchDB() {
		// Build a database from the monarchGraph folder.
		monarchDB = new GraphDatabaseFactory().newEmbeddedDatabase("monarchGraph");
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		// Clean up the databases.
		waterDB.shutdown();
		completeDB.shutdown();
		treeDB.shutdown();
		monarchDB.shutdown();
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
	
	public void validateDB(GraphDatabaseService db) {
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
				System.out.println("Parents:");
				for (Node parent : getParents(node))
				{
					System.out.println(parent.getProperty("name"));
				}
				System.out.println("Children:");
				for (Node child : getChildren(node))
				{
					System.out.println(child.getProperty("name"));
				}
				System.out.println("Ancestors:");
				for (Node ancestor : getAncestors(node))
				{
					System.out.println(ancestor.getProperty("name"));
				}
				System.out.println("Descendants:");
				for (Node descendant : getDescendants(node))
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
				for (Node ancestor : getCommonAncestors(first, second))
				{
					System.out.println(ancestor.getProperty("name"));
				}
				System.out.println("LCS: " + getLCS(first, second).getProperty("name"));
				System.out.println();
			}
		}
	}
	
	@Test
	public void test() {
		validateDBPairwise(treeDB);
	}

}

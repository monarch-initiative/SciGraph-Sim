package org.monarch.sim;

import static org.monarch.sim.Neo4jTraversals.*;

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
	// Graph from monarchGraph folder.
	static GraphDatabaseService monarchDB;	

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		// Build a database with three nodes in a ^ configuration.
		waterDB = new TestGraphDatabaseFactory().newImpermanentDatabase();
		Node waterA = addNode(waterDB, "A");
		Node waterB = addNode(waterDB, "B");
		Node waterC = addNode(waterDB, "C");
		addRelationship(waterDB, waterA, waterB);
		addRelationship(waterDB, waterC, waterB);
		
		// Build a database from the monarchGraph folder.
		monarchDB = new GraphDatabaseFactory().newEmbeddedDatabase("monarchGraph");
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		// Clean up the databases.
		waterDB.shutdown();
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
	
	public static Relationship addRelationship(GraphDatabaseService db, Node first, Node second)
	{
		// Wrap a transaction around edge creation.
		Transaction tx = db.beginTx();
		Relationship newRel = first.createRelationshipTo(second, RelTypes.SUBCLASS);
		tx.success();
		tx.finish();
		return newRel;		
	}
	
	// FIXME: Make this actually check things.
	public void validateWaterDB() {	
		Iterable<Node> nodes = GlobalGraphOperations.at(waterDB).getAllNodes();
		for (Node node : nodes)
		{
			if (node.hasProperty("name"))
			{
				System.out.println("NODE: " + node.getProperty("name"));
				System.out.println("Parents:");
				for (Node parent : getParents(waterDB, node))
				{
					System.out.println(parent.getProperty("name"));
				}
				System.out.println("Children:");
				for (Node child : getChildren(waterDB, node))
				{
					System.out.println(child.getProperty("name"));
				}
				System.out.println("Ancestors:");
				for (Node ancestor : getAncestors(waterDB, node))
				{
					System.out.println(ancestor.getProperty("name"));
				}
				System.out.println("Descendants:");
				for (Node descendant : getDescendants(waterDB, node))
				{
					System.out.println(descendant.getProperty("name"));
				}
			}
		}
		System.out.println();
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
				for (Node ancestor : getCommonAncestors(waterDB, first, second))
				{
					System.out.println(ancestor.getProperty("name"));
				}
			}
		}
	}

	@Test
	public void test() {
//		validateWaterDB();
		for (Node first : GlobalGraphOperations.at(waterDB).getAllNodes())
		{
			if (!first.hasProperty("name"))
			{
				continue;
			}
			
			for (Node second : GlobalGraphOperations.at(waterDB).getAllNodes())
			{
				if (!second.hasProperty("name"))
				{
					continue;
				}
				
				if (first.getProperty("name").toString().compareTo(second.getProperty("name").toString()) > 0)
				{
					continue;
				}
				
				System.out.println(first.getProperty("name") + " " + second.getProperty("name"));
				System.out.println("LCS: " + getLCS(waterDB, first, second));
			}
		}
	}

}

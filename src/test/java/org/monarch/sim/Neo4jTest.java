package org.monarch.sim;

import static org.monarch.sim.Neo4jTraversals.addNode;
import static org.monarch.sim.Neo4jTraversals.addRelationship;
import static org.monarch.sim.Neo4jTraversals.getAncestors;
import static org.monarch.sim.Neo4jTraversals.getChildren;
import static org.monarch.sim.Neo4jTraversals.getCommonAncestors;
import static org.monarch.sim.Neo4jTraversals.getDescendants;
import static org.monarch.sim.Neo4jTraversals.getParents;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
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
		validateWaterDB();
	}

}

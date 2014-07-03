package org.monarch.sim;

import static org.junit.Assert.*;

import java.awt.List;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ReturnableEvaluator;
import org.neo4j.graphdb.StopEvaluator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.Traverser;
import org.neo4j.graphdb.Traverser.Order;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.neo4j.test.TestGraphDatabaseFactory;
import org.neo4j.tooling.GlobalGraphOperations;

public class InitialTest {
	
	// We have a very special relationship, you and I.
	static enum RelTypes implements RelationshipType {
		SUBCLASS,
	}
	
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
	
	// TODO: Move to implementation file.
	public static Node addNode(GraphDatabaseService db, String name) {
		// Wrap a transaction around node creation.
		Transaction tx = db.beginTx();
		Node newNode = db.createNode();
		newNode.setProperty("name", name);
		tx.success();
		tx.finish();
		return newNode;
	}
	
	// TODO: Move to implementation file.
	public static Relationship addRelationship(GraphDatabaseService db, Node first, Node second)
	{
		// Wrap a transaction around edge creation.
		Transaction tx = db.beginTx();
		Relationship newRel = first.createRelationshipTo(second, RelTypes.SUBCLASS);
		tx.success();
		tx.finish();
		return newRel;		
	}
	
	// TODO: Move to implementation file.
	public Iterable<String> getProperties(GraphDatabaseService db) {
		LinkedList<String> properties = new LinkedList<String>();
		
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
	
	// TODO: Move to implementation file.
	public Iterable<Node> getDirectedNeighbors(GraphDatabaseService db, Node node, Direction dir) {
		// Make the process a transaction.
		Transaction tx = db.beginTx();
		
		// Get the relationships.
		Iterable<Relationship> rels = node.getRelationships(dir, RelTypes.SUBCLASS);
		
		// Turn the relationships into neighbors.
		LinkedList<Node> neighbors = new LinkedList<Node>();
		for (Relationship rel : rels)
		{
			neighbors.add(rel.getOtherNode(node));
		}
		
		tx.success();
		tx.finish();
		return neighbors;
	}
	
	// TODO: Move to implementation file.
	public Iterable<Node> getChildren(GraphDatabaseService db, Node node) {
		return getDirectedNeighbors(db, node, Direction.INCOMING);
	}
	
	// TODO: Move to implementation file.
	public Iterable<Node> getParents(GraphDatabaseService db, Node node) {
		return getDirectedNeighbors(db, node, Direction.OUTGOING);
	}
	
	// TODO: Move to implementation file.
	public Iterable<Node> getDirectedDescendants(GraphDatabaseService db, Node node, Direction dir) {
		// Make the process a transaction.
		Transaction tx = db.beginTx();
		
		Set<Node> descendants = new HashSet<Node>();
		LinkedList<Node> toTry = new LinkedList<Node>();
		toTry.add(node);
		// Check nodes until we run out of possibilities.
		while (!toTry.isEmpty())
		{
			Node curNode = toTry.removeFirst();
			// We can skip nodes we've already seen.
			if (descendants.contains(curNode))
			{
				continue;
			}
			descendants.add(curNode);
			// We need to expand from each of the neighbors.
			for (Node neighbor : getDirectedNeighbors(db, node, dir))
			{
				toTry.add(neighbor);
			}
		}
		
		tx.success();
		tx.finish();
		return descendants;
	}
	
	// TODO: Move to implementation file.
	public Iterable<Node> getAncestors(GraphDatabaseService db, Node node) {
		return getDirectedDescendants(db, node, Direction.OUTGOING);
	}
	
	// TODO: Move to implementation file.
	public Iterable<Node> getDescendants(GraphDatabaseService db, Node node) {
		return getDirectedDescendants(db, node, Direction.INCOMING);
	}
	
	// TODO: Move to implementation file.
	// TODO: Write this using Neo4j more directly.
	public Iterable<Node> getCommonAncestors(GraphDatabaseService db, Node first, Node second) {
		Set<Node> firstAncestors = (HashSet<Node>)getAncestors(db, first);
		Set<Node> secondAncestors = (HashSet<Node>)getAncestors(db, second);
		firstAncestors.retainAll(secondAncestors);
		return firstAncestors;
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

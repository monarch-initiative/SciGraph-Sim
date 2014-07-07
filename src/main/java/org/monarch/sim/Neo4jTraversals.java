package org.monarch.sim;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.monarch.sim.InitialTest.RelTypes;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.tooling.GlobalGraphOperations;

public class Neo4jTraversals {
	
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

	public static Iterable<String> getProperties(GraphDatabaseService db) {
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
	
	public static Iterable<Node> getDirectedNeighbors(GraphDatabaseService db, Node node, Direction dir) {
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
	
	public static Iterable<Node> getChildren(GraphDatabaseService db, Node node) {
		return getDirectedNeighbors(db, node, Direction.INCOMING);
	}
	
	public static Iterable<Node> getParents(GraphDatabaseService db, Node node) {
		return getDirectedNeighbors(db, node, Direction.OUTGOING);
	}

	public static Iterable<Node> getDirectedDescendants(GraphDatabaseService db, Node node, Direction dir) {
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

	public static Iterable<Node> getAncestors(GraphDatabaseService db, Node node) {
		return getDirectedDescendants(db, node, Direction.OUTGOING);
	}

	public static Iterable<Node> getDescendants(GraphDatabaseService db, Node node) {
		return getDirectedDescendants(db, node, Direction.INCOMING);
	}

	// TODO: Write this using Neo4j more directly.
	public static Iterable<Node> getCommonAncestors(GraphDatabaseService db, Node first, Node second) {
		Set<Node> firstAncestors = (HashSet<Node>)getAncestors(db, first);
		Set<Node> secondAncestors = (HashSet<Node>)getAncestors(db, second);
		firstAncestors.retainAll(secondAncestors);
		return firstAncestors;
	}

}

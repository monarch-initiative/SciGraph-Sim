package org.monarch.sim;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.tooling.GlobalGraphOperations;

public class Neo4jTraversals {
	
	// Define the relationships we want to use.
	static enum RelTypes implements RelationshipType {
		SUBCLASS,
	}

	public static Iterable<String> getProperties(GraphDatabaseService db) {
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
	
	public static Iterable<Node> getDirectedNeighbors(Node node, Direction dir) {
		// Get the relationships.
		Iterable<Relationship> rels = node.getRelationships(dir);
		
		// Turn the relationships into neighbors.
		LinkedList<Node> neighbors = new LinkedList<>();
		for (Relationship rel : rels)
		{
			neighbors.add(rel.getOtherNode(node));
		}
		return neighbors;
	}
	
	public static Iterable<Node> getChildren(Node node) {
		return getDirectedNeighbors(node, Direction.INCOMING);
	}
	
	public static Iterable<Node> getParents(Node node) {
		return getDirectedNeighbors(node, Direction.OUTGOING);
	}

	public static Iterable<Node> getDirectedDescendants(Node node, Direction dir) {
		Set<Node> descendants = new HashSet<>();
		LinkedList<Node> toTry = new LinkedList<>();
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
			for (Node neighbor : getDirectedNeighbors(curNode, dir))
			{
				toTry.add(neighbor);
			}
		}

		return descendants;
	}

	public static Iterable<Node> getAncestors(Node node) {
		return getDirectedDescendants(node, Direction.OUTGOING);
	}

	public static Iterable<Node> getDescendants(Node node) {
		return getDirectedDescendants(node, Direction.INCOMING);
	}

	// TODO: Write this using Neo4j more directly.
	public static Iterable<Node> getCommonAncestors(Node first, Node second) {
		Set<Node> firstAncestors = (HashSet<Node>)getAncestors(first);
		Set<Node> secondAncestors = (HashSet<Node>)getAncestors(second);
		firstAncestors.retainAll(secondAncestors);
		return firstAncestors;
	}
	
	private static class LCSNode {
		public Node node;
		public Node subsumer;
		public boolean goingUp;
		
		public LCSNode(Node node_)
		{
			node = node_;
			goingUp = true;
		}
		
		public LCSNode(Node node_, Node subsumer_)
		{
			node = node_;
			subsumer = subsumer_;
			goingUp = false;
		}
	}
	
	// TODO: This can actually be horribly inefficient.
	// FIXME: This doesn't necessarily get the least common subsumer,
	// only the subsumer with the shortest combined path.
	// For instance, in cycleDB (Neo4jTest), the least common subsumer
	// of B and E is B, but there is a shorter path with subsumer A.
	// FIXME: Benchmarking shows this is terrible. Write a version that
	// just gets all subsumers and finds the one with the best IC.
	public static Node getLCS(Node first, Node second) {		
		if (first.equals(second))
		{
			return first;
		}

		HashSet<Node> upVisited = new HashSet<>();
		HashSet<Node> downVisited = new HashSet<>();
		LinkedList<LCSNode> toTry = new LinkedList<>();
		toTry.add(new LCSNode(first));
		toTry.add(new LCSNode(first, first));
		
		while (!toTry.isEmpty())
		{
			LCSNode next = toTry.removeFirst();
			
			// If we're headed up, we can either go up or down.
			if (next.goingUp)
			{
				if (!upVisited.add(next.node))
				{
					continue;
				}
				
				for (Node parent : getParents(next.node))
				{
					if (parent.equals(second))
					{
						return parent;
					}
					
					toTry.add(new LCSNode(parent));
					toTry.add(new LCSNode(parent, parent));
				}
			}
			
			// If we're headed down, we can only go down further.
			else
			{
				if (!downVisited.add(next.node))
				{
					continue;
				}
				
				for (Node child : getChildren(next.node))
				{
					if (child.equals(second))
					{
						return next.subsumer;
					}
					
					toTry.add(new LCSNode(child, next.subsumer));
				}
			}
		}
		
		// FIXME: This should throw an error or otherwise complain loudly.
		// Our ontologies are all rooted, so we should always have an LCS.
		return null;
	}

}

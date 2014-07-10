package org.monarch.sim;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipExpander;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.tooling.GlobalGraphOperations;
import org.neo4j.graphalgo.impl.ancestor.AncestorsUtil;
import org.neo4j.kernel.OrderedByTypeExpander;
import org.neo4j.kernel.StandardExpander;
import org.neo4j.kernel.Traversal;

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
	
//	private static class LCSNode {
//		public Node node;
//		public Node subsumer;
//		public boolean goingUp;
//		
//		public LCSNode(Node node_)
//		{
//			node = node_;
//			goingUp = true;
//		}
//		
//		public LCSNode(Node node_, Node subsumer_)
//		{
//			node = node_;
//			subsumer = subsumer_;
//			goingUp = false;
//		}
//	}
	
	public static Node getLCS(Node first, Node second) {
		List<Node> nodes = new ArrayList<Node>();
		nodes.add(first);
		nodes.add(second);
		RelationshipExpander expander = Traversal.expanderForAllTypes(Direction.OUTGOING);
		return AncestorsUtil.lowestCommonAncestor(nodes, expander);
	}
	
}

package org.monarch.sim;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.tooling.GlobalGraphOperations;

public class Neo4jTraversals {
	
	private static Iterable<Node> getDirectedNeighbors(Node node, Direction dir) {
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

	private static Iterable<Node> getDirectedDescendants(Node node, Direction dir) {
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
	
	public static void setAllIC(GraphDatabaseService db) {
		Transaction tx = db.beginTx();
		Iterable<Node> nodes = GlobalGraphOperations.at(db).getAllNodes();
		int totalNodes = IteratorUtil.count(nodes) - 1;
		for (Node n : nodes)
		{
			// FIXME: Bad IC scores for testing purposes.
			n.setProperty("IC", 1.0);
//			if (n.getId() == 0)
//			{
//				continue;
//			}
//			
//			// FIXME: When we have real data, we should calculate this better.
//			// NOTE: Node properties have to be primitives, so we can't use a
//			// map to represent multiple IC scores or a list to store descendants.
//			int nodesBelow = IteratorUtil.count(Neo4jTraversals.getDescendants(n));
//			double ic = -Math.log((double) nodesBelow / totalNodes) / Math.log(2);
//			n.setProperty("IC", ic);
		}
		tx.success();
		tx.finish();
	}
	
	public static double getIC(Node n) {
		// This should never happen.
		if (n == null)
		{
			return 0;
		}
		return (double) n.getProperty("IC");
	}
	
	public static Node getLCS(Node first, Node second) {
		// Start with the ancestors of the first node.
		HashSet<Node> firstAncestors = (HashSet<Node>)getAncestors(first);
		
		// We want to expand ancestors of the second node in order of
		// decreasing IC score. 
		Comparator<Node> icComparator = new Comparator<Node>() {
			public int compare(Node first, Node second) {
				double firstIC = getIC(first);
				double secondIC = getIC(second);
				if (firstIC < secondIC)
					return -1;
				else if (firstIC > secondIC)
					return 1;
				else
					return 0;
			}
		};
		// NOTE: The magic number 11 is the default heap capacity, but there
		// isn't a sensible constructor.
		PriorityQueue<Node> heap = new PriorityQueue<Node>(11, icComparator);
		heap.add(second);
		HashSet<Node> visited = new HashSet<>();
		
		// Expand outward from the second node.
		while (!heap.isEmpty())
		{
			Node toExpand = heap.remove();			
			if (!visited.add(toExpand))
			{
				continue;
			}
			// If we hit an ancestor of the first node, it must be our LCS.
			if (firstAncestors.contains(toExpand))
			{
				return toExpand;
			}
			for (Node parent : getParents(toExpand))
			{
				heap.add(parent);
			}
		}
		
		// FIXME: This should throw some sort of error.
		// Our graph is rooted, so this should never happen.
		return null;
	}
	
	public static List<Node> getShortestPath(Node first, Node second) {
		HashSet<Node> visited = new HashSet<>();
		Queue<LinkedList<Node>> paths = new LinkedList<>();
		
		// Expand paths starting with our base node. 
		LinkedList<Node> basePath = new LinkedList<>();
		basePath.add(first);
		paths.add(basePath);
		
		// Expand paths.
		while (!paths.isEmpty())
		{
			LinkedList<Node> nextPath = paths.remove();
			Node lastNode = nextPath.getLast();
			
			// If we've reached the end node, stop.
			if (lastNode.equals(second))
			{
				return nextPath;
			}
			
			// If we've been here before, don't expand.
			if (!visited.add(lastNode))
			{
				continue;
			}
			
			// If we've gone too far up, don't expand.
			if ((Double) lastNode.getProperty("IC") < (Double) second.getProperty("IC"))
			{
				continue;
			}
			
			// Extend the path to all parents.
			for (Node parent : getParents(lastNode))
			{
				LinkedList<Node> toAdd = new LinkedList<>(nextPath);
				toAdd.add(parent);
				paths.add(toAdd);
			}
			
		}
		
		// FIXME: This should probably return an Optional.
		// If we haven't found anything, give up.
		return null;
	}
	
	public static double getMaxIC(Iterable<Node> firstNodes, Iterable<Node> secondNodes) {
		double maxIC = 0;
		
		// Check each pair of nodes.
		for (Node first : firstNodes)
		{
			for (Node second : secondNodes)
			{
				double lcsIC = getIC(getLCS(first, second));
				if (lcsIC > maxIC)
				{
					maxIC = lcsIC;
				}
			}
		}
		
		return maxIC;
	}
	
	public static double getAverageIC(Iterable<Node> firstNodes, Iterable<Node> secondNodes) {
		double totalIC = 0;
		int count = 0;
		
		// Check each pair of nodes.
		for (Node first : firstNodes)
		{
			for (Node second : secondNodes)
			{
				double lcsIC = getIC(getLCS(first, second));
				totalIC += lcsIC;
				count++;
			}
		}
		
		return totalIC / count;
	}
	
}

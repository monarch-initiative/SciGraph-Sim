package org.monarch.sim;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.tooling.GlobalGraphOperations;

public class NaiveTraverser {
	
	private GraphDatabaseService db;
	private int totalNodes;

	// This traverser should be able to handle arbitrary inclusion or exclusion
	// of edge types.
	// FIXME: Handle direction.
	private Map<String, RelationshipType> edgeTypeMap;
	private Set<RelationshipType> relevantEdgeTypes;
	private boolean includeEdges;
	
	private Map<Node, Double> icMap;
	
	/**
	 * Constructs a traverser to walk through a Neo4j database.
	 * By default, this traverses all types of edges.
	 * 
	 * The name allows multiple traversers to use the same graph without
	 * naming conflicts for node properties.
	 * 
	 * @param db	The database to traverse
	 * @param name	The name of the traverser
	 */
	public NaiveTraverser(GraphDatabaseService db, String name) {
		this.db = db;
		
		Iterable<Node> nodes = GlobalGraphOperations.at(this.db).getAllNodes();
		// Neo4j uses a dummy node. Ignore it.
		totalNodes = -1;
		icMap = new HashMap<>();
		for (Node n : nodes)
		{
			totalNodes++;
			icMap.put(n, 0.0);
		}
		
		edgeTypeMap = new HashMap<>();
		for (RelationshipType edgeType : GlobalGraphOperations.at(this.db).getAllRelationshipTypes())
		{
			edgeTypeMap.put(edgeType.name(), edgeType);
		}
		includeAllEdgeTypes();
	}
	
	/**
	 * Limits the edges this traverser follows to the types given.
	 * 
	 * @param edgeTypes	The names of the types of edges we want to include
	 */
	public void includeEdgeTypes(Collection<String> edgeTypes) {
		includeEdges = true;
		relevantEdgeTypes = new HashSet<>();
		for (String edgeType : edgeTypes)
		{
			if (edgeTypeMap.containsKey(edgeType))
			{
				relevantEdgeTypes.add(edgeTypeMap.get(edgeType));
			}
		}
	}
	
	/**
	 * Allows the traverser to follow all types of edges.
	 */
	public void includeAllEdgeTypes() {
		excludeEdgeTypes(new ArrayList<String>());
	}
	
	/**
	 * Limits the edges this traverser follows to the types not given.
	 * 
	 * @param edgeTypes	The names of the types of edges we want to exclude
	 */
	public void excludeEdgeTypes(Collection<String> edgeTypes) {
		includeEdges = false;
		relevantEdgeTypes = new HashSet<>();
		for (String edgeType : edgeTypes)
		{
			if (edgeTypeMap.containsKey(edgeType))
			{
				relevantEdgeTypes.add(edgeTypeMap.get(edgeType));
			}
		}
	}
	
	private Set<Node> getDirectedNeighbors(Node n, Direction dir) {
		Set<Node> neighbors = new HashSet<>();
		for (Relationship edge : n.getRelationships(dir))
		{
			if (relevantEdgeTypes.contains(edge.getType()) == includeEdges)
			{
				neighbors.add(edge.getOtherNode(n));
			}
		}
		
		return neighbors;
	}
	
	/**
	 * Finds all nodes immediately below a given node.
	 * 
	 * @param n	The node whose children we want.
	 */
	public Set<Node> getChildren(Node n) {
		return getDirectedNeighbors(n, Direction.INCOMING);
	}

	/**
	 * Finds all nodes immediately above a given node.
	 * 
	 * @param n	The node whose parents we want.
	 */
	public Set<Node> getParents(Node n) {
		return getDirectedNeighbors(n, Direction.OUTGOING);
	}
	
	private Set<Node> getDirectedDescendants(Node n, Direction dir) {
		Set<Node> descendants = new HashSet<>();
		descendants.add(n);
		
		Queue<Node> toExpand = new ArrayDeque<>();
		toExpand.add(n);
		
		while (! toExpand.isEmpty())
		{
			Node next = toExpand.remove();
			for (Node child : getDirectedNeighbors(next, dir))
			{
				if (descendants.add(child))
				{
					toExpand.add(child);
				}
			}
		}
		
		return descendants;
	}
	
	/**
	 * Finds all nodes anywhere below a given node (inclusive).
	 * 
	 * @param n	The node whose descendants we want.
	 */
	public Set<Node> getDescendants(Node n) {
		return getDirectedDescendants(n, Direction.INCOMING);
	}
	
	/**
	 * Finds all nodes anywhere above a given node (inclusive).
	 * 
	 * @param n	The node whose ancestors we want.
	 */
	public Set<Node> getAncestors(Node n) {
		return getDirectedDescendants(n, Direction.OUTGOING);
	}
	
	/**
	 * Counts the nodes anywhere below each node.
	 * 
	 * @param ignoredEdgeTypes	Any additional edge types we don't want to use
	 */
	public void pushAllNodes(Collection<String> ignoredEdgeTypes) {
		// To temporarily change the valid edge types, we need to save the old values.
		Set<RelationshipType> oldRelevantEdgeTypes = relevantEdgeTypes;
		
		// Set the edge types temporarily.
		Set<RelationshipType> newRelevantEdgeTypes = new HashSet<>();
		for (String typeName : ignoredEdgeTypes)
		{
			if (edgeTypeMap.containsKey(typeName))
			{
				newRelevantEdgeTypes.add(edgeTypeMap.get(typeName));
			}
		}
		
		relevantEdgeTypes = new HashSet<>(oldRelevantEdgeTypes);
		if (includeEdges)
		{
			relevantEdgeTypes.removeAll(newRelevantEdgeTypes);
		}
		else
		{
			relevantEdgeTypes.addAll(newRelevantEdgeTypes);
		}
		
		// Sort the nodes topologically.
		Map<Node, PushNodesInfo> topSortMap = new HashMap<>();
		Queue<Node> toExpand = new ArrayDeque<>();

		// Preprocess the nodes.
		for (Node n : GlobalGraphOperations.at(db).getAllNodes())
		{
			Set<Node> children = getChildren(n);
			Set<Node> parents = getParents(n);
			PushNodesInfo info = new PushNodesInfo();
			int unpushedChildren = children.size();
			info.unpushedChildrern = unpushedChildren;
			info.unpushedParents = parents.size();
			info.nodesBelow.add(n);
			topSortMap.put(n, info);
			
			// The nodes without children are our base nodes.
			if (unpushedChildren == 0)
			{
				toExpand.add(n);
			}
		}
		
		// FIXME: Remove this.
		int count = 0;
		
		// Expand until we run out of nodes.
		while (! toExpand.isEmpty())
		{
			// FIXME: Remove this.
			count++;
			
			Node next = toExpand.poll();

			// If there's still something with no descendants, expand.
			if (next != null)
			{

				for (Node parent : getParents(next))
				{
					PushNodesInfo parentInfo = topSortMap.get(parent);
					parentInfo.unpushedChildrern--;

					// Check if any of the parents are now leaves.
					if (parentInfo.unpushedChildrern == 0)
					{
						toExpand.add(parent);
					}
				}

				// Take the union of all the children's descendants.
				for (Node child : getChildren(next))
				{
					PushNodesInfo childInfo = topSortMap.get(child);
					topSortMap.get(next).nodesBelow.addAll(childInfo.nodesBelow);

					// If we no longer need the node, clean up.
					childInfo.unpushedParents--;
					if (childInfo.unpushedParents == 0)
					{
						topSortMap.remove(child);
					}
				}

				// Save the IC.
				int nodesBelow = topSortMap.get(next).nodesBelow.size();
				double ic = (Math.log(totalNodes) - Math.log(nodesBelow)) / Math.log(2); 
				icMap.put(next, ic);
			}
		}
		
		System.out.println(totalNodes - count + " never enqueued");
		
		// Restore the old edge types.
		relevantEdgeTypes = oldRelevantEdgeTypes;
	}
	
	private class PushNodesInfo {
		public Set<Node> nodesBelow = new HashSet<>();
		public int unpushedChildrern = 0;
		public int unpushedParents = 0;
	}

	/**
	 * Find the IC score for a given node.
	 * 
	 * @param n	The node whose IC score we want.
	 */
	public double getIC(Node n) {
		return icMap.get(n);
	}
	
	// FIXME: The version below should be faster, but can't handle linked nodes.
	public Node getLCS(Node first, Node second) {
		Set<Node> ancestors = getAncestors(first);
		ancestors.retainAll(getAncestors(second));
		
		Node lcs = null;
		double ic = 0;
		for (Node ancestor : ancestors)
		{
			if (ancestor.hasProperty("fragment"))
			{
				String fragment = (String) ancestor.getProperty("fragment");
				if (fragment.matches("-?\\d*"))
				{
					continue;
				}
			}
			
			double ancestorIC = getIC(ancestor);
			if (ancestorIC > ic)
			{
				lcs = ancestor;
				ic = ancestorIC;
			}
		}
		
		return lcs;
	}
	
//	public Node getLCS(Node first, Node second) {
//		// Start with the ancestors of the first node.
//		Set<Node> firstAncestors = getAncestors(first);
//		
//		// We want to expand ancestors of the second node in order of
//		// decreasing IC score. 
//		Comparator<Node> icComparator = new Comparator<Node>() {
//			public int compare(Node first, Node second) {
//				double firstIC = getIC(first);
//				double secondIC = getIC(second);
//				if (firstIC < secondIC)
//					return -1;
//				else if (firstIC > secondIC)
//					return 1;
//				else
//					return (int) (first.getId() - second.getId());
//			}
//		};
//		
//		// NOTE: The magic number 11 is the default heap capacity, but there
//		// isn't a sensible constructor.
//		PriorityQueue<Node> heap = new PriorityQueue<Node>(11, icComparator);
//		heap.add(second);
//		HashSet<Node> visited = new HashSet<>();
//		
//		// Expand outward from the second node.
//		while (! heap.isEmpty())
//		{
//			Node toExpand = heap.remove();
//			if (! visited.add(toExpand))
//			{
//				continue;
//			}
//			
//			// If we hit an ancestor of the first node, it must be our LCS.
//			if (firstAncestors.contains(toExpand))
//			{
//				return toExpand;
//			}
//			
//			// Expand outward.
//			for (Node parent : getParents(toExpand))
//			{
//				heap.add(parent);
//			}
//		}
//		
//		// FIXME: This should throw some sort of error.
//		// Our graph is rooted, so this should never happen.
//		return null;
//	}
	
}

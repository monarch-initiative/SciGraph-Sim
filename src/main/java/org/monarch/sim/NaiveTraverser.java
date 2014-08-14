package org.monarch.sim;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.tooling.GlobalGraphOperations;

public class NaiveTraverser {
	
	private GraphDatabaseService db;
	private int nodeCount;

	// This traverser should be able to handle arbitrary inclusion or exclusion
	// of edge types.
	// FIXME: Handle direction.
	private Map<String, RelationshipType> edgeTypeMap;
	private Set<RelationshipType> relevantEdgeTypes;
	private boolean includeEdges;
	
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
		nodeCount = IteratorUtil.count(GlobalGraphOperations.at(this.db).getAllNodes());
		
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
			relevantEdgeTypes.add(edgeTypeMap.get(edgeType));
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
			relevantEdgeTypes.add(edgeTypeMap.get(edgeType));
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
	
	// FIXME: Remove this.
	public Node getDummyLCS(Node first, Node second) {
		Set<Node> firstAncestors = getAncestors(first);		
		Set<Node> commonAncestors = new HashSet<>();
		
		Set<Node> visited = new HashSet<>();
		Queue<Node> toExpand = new ArrayDeque<>();
		toExpand.add(second);
		while (! toExpand.isEmpty())
		{
			Node next = toExpand.remove();
			if (! visited.add(next))
			{
				continue;
			}
			
			for (Node parent : getParents(next))
			{
				if (firstAncestors.contains(parent))
				{
					commonAncestors.add(parent);
				}
				else
				{
					toExpand.add(parent);
				}
			}
		}
		
		Node lcs = null;
		int minDist = Integer.MAX_VALUE;
		for (Node ancestor : commonAncestors)
		{
			int dist = Neo4jTraversals.getShortestPath(first, ancestor).size() +
					Neo4jTraversals.getShortestPath(second, ancestor).size();
			if (dist < minDist)
			{
				lcs = ancestor;
				minDist = dist;
			}
		}
		
		return lcs;
	}

}

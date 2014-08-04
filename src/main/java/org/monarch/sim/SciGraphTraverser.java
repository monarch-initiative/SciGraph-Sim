package org.monarch.sim;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.Traversal;
import org.neo4j.kernel.Uniqueness;
import org.neo4j.tooling.GlobalGraphOperations;

public class SciGraphTraverser {

	private GraphDatabaseService db;
	private Set<RelationshipType> edgeTypes = new HashSet<>();
	// FIXME: In Neo4j 2.x, the traversal should come from the DB.
	private TraversalDescription basicTraversal = Traversal.traversal()
			.breadthFirst()
			.uniqueness(Uniqueness.NODE_GLOBAL)
			;
	private boolean edgeTypesDefined = false;
	
	/**
	 * Constructs a traverser to walk through a Neo4j database.
	 * By default, this traverses all types of edges.
	 * 
	 * @param db	The database to traverse
	 */
	public SciGraphTraverser(GraphDatabaseService db) {
		this.db = db;
		for (RelationshipType edgeType : GlobalGraphOperations.at(db).getAllRelationshipTypes())
		{
			edgeTypes.add(edgeType);
		}
	}
	
	/**
	 * Adds a type of relationship to traverse along.
	 * If the graph does not contain the given type of edge, does nothing.
	 * 
	 * @param typeName	The name of the type of relationship
	 * @param dir		Which direction is 'up'
	 */
	public void relationships(String typeName, Direction dir) {
		// Find the relationship type with the right name.
		for (RelationshipType edgeType : edgeTypes)
		{
			if (edgeType.toString().equals(typeName))
			{
				basicTraversal = basicTraversal.relationships(edgeType, dir);
				edgeTypesDefined = true;
				return;
			}
		}
		
		// FIXME: This should throw some sort of error.
	}
	
	/**
	 * Adds a type of relationship to traverse along.
	 * If the graph does not contain the given type of edge, does nothing.
	 * 
	 * @param typeName	The name of the type of relationship
	 * @param dir		Which direction is 'up'
	 */
	public void relationships(String typeName) {
		relationships(typeName, Direction.BOTH);
	}
	
	private Iterable<Node> traversalHelper(Node n, boolean up, boolean oneStep) {
		TraversalDescription td = basicTraversal;
		
		if (!edgeTypesDefined)
		{
			for (RelationshipType edgeType : edgeTypes)
			{
				td = td.relationships(edgeType, Direction.INCOMING);
			}
		}
		
		if (up)
		{
			td = td.reverse();
		}
		
		if (oneStep)
		{
			td = td
					.evaluator(Evaluators.fromDepth(1))
					.evaluator(Evaluators.toDepth(1))
					;
		}
		
		return td
				.traverse(n)
				.nodes()
				;
	}

	/**
	 * Finds all nodes immediately above a given node.
	 * 
	 * @param n	The node whose parents we want
	 */
	public Iterable<Node> getParents(Node n) {
		return traversalHelper(n, true, true);
	}
	
	/**
	 * Finds all nodes immediately below a given node.
	 * 
	 * @param n	The node whose children we want
	 */
	public Iterable<Node> getChildren(Node n) {
		return traversalHelper(n, false, true);
	}
	
	/**
	 * Finds all nodes anywhere above a given node (inclusive).
	 * 
	 * @param n	The node whose ancestors we want
	 */
	public Iterable<Node> getAncestors(Node n) {
		return traversalHelper(n, true, false);
	}
	
	/**
	 * Finds all nodes anywhere above a given node (inclusive).
	 * 
	 * @param n	The node whose descendants we want
	 */
	public Iterable<Node> getDescendants(Node n) {
		return traversalHelper(n, false, false);
	}
	
}

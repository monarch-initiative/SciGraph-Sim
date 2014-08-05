package org.monarch.sim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.kernel.Traversal;
import org.neo4j.kernel.Uniqueness;
import org.neo4j.tooling.GlobalGraphOperations;

public class SciGraphTraverser {

	private GraphDatabaseService db;
	private int nodeCount;
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
		
		nodeCount = IteratorUtil.count(GlobalGraphOperations.at(db).getAllNodes()) - 1;
		
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
	
	private Collection<Node> traversalHelper(Node n, boolean up, boolean oneStep) {
		TraversalDescription td = basicTraversal;
		
		// If we haven't set any edge types, assume all edges point up.
		if (!edgeTypesDefined)
		{
			for (RelationshipType edgeType : edgeTypes)
			{
				td = td.relationships(edgeType, Direction.INCOMING);
			}
		}
		
		// Handle the separate cases.
		if (!up)
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
		
		// Traverse.
		Iterable<Node> iter = td
				.traverse(n)
				.nodes()
				;
		
		// Iterables are hard to work with, so convert.
		Collection<Node> nodes = new ArrayList<>();
		for (Node found : iter)
		{
			nodes.add(found);
		}
		return nodes;
	}

	/**
	 * Finds all nodes immediately above a given node.
	 * 
	 * @param n	The node whose parents we want
	 */
	public Collection<Node> getParents(Node n) {
		return traversalHelper(n, false, true);
	}
	
	/**
	 * Finds all nodes immediately below a given node.
	 * 
	 * @param n	The node whose children we want
	 */
	public Collection<Node> getChildren(Node n) {
		return traversalHelper(n, true, true);
	}
	
	/**
	 * Finds all nodes anywhere above a given node (inclusive).
	 * 
	 * @param n	The node whose ancestors we want
	 */
	public Collection<Node> getAncestors(Node n) {
		return traversalHelper(n, false, false);
	}
	
	/**
	 * Finds all nodes anywhere above a given node (inclusive).
	 * 
	 * @param n	The node whose descendants we want
	 */
	public Collection<Node> getDescendants(Node n) {
		return traversalHelper(n, true, false);
	}
	
	private Iterable<Node> getUnpushedDescendants(Node n) {
		// We want to ignore nodes that have already been pushed.
		Evaluator evaluator = new Evaluator() {
			@Override
			public Evaluation evaluate(Path path) {
				if (path.endNode().hasProperty("pushed"))
				{
					return Evaluation.EXCLUDE_AND_PRUNE;
				}
				else
				{
					return Evaluation.INCLUDE_AND_CONTINUE;
				}
			}
		};
		
		// Traverse.
		return basicTraversal
				.evaluator(evaluator)
				.traverse(n)
				.nodes()
				;
	}
	
	private void addDescendant(Node n) {
		Transaction tx = db.beginTx();
		
		String key = "descendants";
		if (!n.hasProperty(key))
		{
			n.setProperty(key, 1);
		}
		else
		{
			n.setProperty(key, (int) n.getProperty(key) + 1);
		}
		
		tx.success();
		tx.finish();
	}
	
	private void pushUp(Node n) {
		String key = "pushed";
		
		// If we've already pushed, do nothing.
		if (n.hasProperty(key))
		{
			return;
		}
		
		// Mark the node as pushed.
		Transaction tx = db.beginTx();
		n.setProperty(key, true);
		tx.success();
		tx.finish();
		
		// Add a descendant to each ancestor.
		for (Node ancestor : getAncestors(n))
		{
			addDescendant(ancestor);
		}
	}
	
	/**
	 * Finds the IC score of a given node.
	 * 
	 * @param n	The node whose IC we want.
	 */
	public double getIC(Node n) {
		// Check if we've already done the work.
		String key = "IC";
		if (n.hasProperty(key))
		{
			return (double) n.getProperty(key);
		}
		
		// Make all the descendants push up.
		for (Node descendant : getUnpushedDescendants(n))
		{
			pushUp(descendant);
		}
		
		// TODO: Everything related to IC will need to be recalculated to
		// handle annotations.
		int descendants = (int) n.getProperty("descendants");
		double ic = (Math.log(nodeCount) - Math.log(descendants)) / Math.log(2);
		
		Transaction tx = db.beginTx();
		n.setProperty(key, ic);
		tx.success();
		tx.finish();
		
		return ic;
	}
	
}

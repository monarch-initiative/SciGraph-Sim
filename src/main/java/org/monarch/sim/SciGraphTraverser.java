package org.monarch.sim;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotFoundException;
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
	
	private static final int traversalCachedNodes = 10000;

	private String PUSHED_KEY;
	private String DESCENDANT_KEY;
	private String IC_KEY;
	
	private GraphDatabaseService db;
	private int nodeCount;
	private Set<RelationshipType> edgeTypes;
	// FIXME: In Neo4j 2.x, the traversal should come from the DB.
	private TraversalDescription basicDownwardTraversal = Traversal
			.traversal()
			.breadthFirst()
			.uniqueness(Uniqueness.NODE_RECENT, traversalCachedNodes)
			;
	private boolean edgeTypesDefined = false;
	
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
	public SciGraphTraverser(GraphDatabaseService db, String name) {
		this.db = db;
		
		nodeCount = IteratorUtil.count(GlobalGraphOperations.at(db).getAllNodes()) - 1;
		
		edgeTypes = new HashSet<>();
		for (RelationshipType edgeType : GlobalGraphOperations.at(db).getAllRelationshipTypes())
		{
			edgeTypes.add(edgeType);
		}
		
		PUSHED_KEY = name + "_pushed";
		DESCENDANT_KEY = name + "_descendnts";
		IC_KEY = name + "_ic";
	}
	
	/**
	 * Adds a type of relationship to traverse along.
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
				basicDownwardTraversal = basicDownwardTraversal.relationships(edgeType, dir);
				edgeTypesDefined = true;
				return;
			}
		}
		
		throw new NotFoundException("Relationship type \"" + typeName + "\" not found");
	}
	
	/**
	 * Adds a type of relationship to traverse along.
	 * 
	 * @param typeName	The name of the type of relationship
	 * @param dir		Which direction is 'up'
	 */
	public void relationships(String typeName) {
		relationships(typeName, Direction.BOTH);
	}
	
	private Collection<Node> traversalHelper(Node n, boolean up, boolean oneStep) {
		TraversalDescription td = basicDownwardTraversal;
		
		// If we haven't set any edge types, assume all edges point up.
		if (!edgeTypesDefined)
		{
			for (RelationshipType edgeType : edgeTypes)
			{
				td = td.relationships(edgeType, Direction.INCOMING);
			}
		}
		
		// Handle the separate cases.
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
		
		// Traverse.
		Iterable<Node> iter = td
				.traverse(n)
				.nodes()
				;
		
		// Iterables are hard to work with, so convert to a Collection.
		Collection<Node> nodes = new HashSet<>();
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
		return traversalHelper(n, true, true);
	}
	
	/**
	 * Finds all nodes immediately below a given node.
	 * 
	 * @param n	The node whose children we want
	 */
	public Collection<Node> getChildren(Node n) {
		return traversalHelper(n, false, true);
	}
	
	/**
	 * Finds all nodes anywhere above a given node (inclusive).
	 * 
	 * @param n	The node whose ancestors we want
	 */
	public Collection<Node> getAncestors(Node n) {
		return traversalHelper(n, true, false);
	}
	
	/**
	 * Finds all nodes anywhere above a given node (inclusive).
	 * 
	 * @param n	The node whose descendants we want
	 */
	public Collection<Node> getDescendants(Node n) {
		return traversalHelper(n, false, false);
	}
	
	private Iterable<Node> getUnpushedDescendants(Node n) {
		// We want to ignore nodes that have already been pushed.
		Evaluator evaluator = new Evaluator()
		{
			@Override
			public Evaluation evaluate(Path path) {
				if (path.endNode().hasProperty(PUSHED_KEY))
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
		return basicDownwardTraversal
				.evaluator(evaluator)
				.traverse(n)
				.nodes()
				;
	}
	
	private void addDescendant(Node n) {
		Transaction tx = db.beginTx();
		
		if (!n.hasProperty(DESCENDANT_KEY))
		{
			n.setProperty(DESCENDANT_KEY, 1);
		}
		else
		{
			n.setProperty(DESCENDANT_KEY, (int) n.getProperty(DESCENDANT_KEY) + 1);
		}
		
		tx.success();
		tx.finish();
	}
	
	private void pushUp(Node n) {
		// If we've already pushed, do nothing.
		if (n.hasProperty(PUSHED_KEY))
		{
			return;
		}
		
		// Mark the node as pushed.
		Transaction tx = db.beginTx();
		n.setProperty(PUSHED_KEY, true);
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
		// TODO: This should probably be an error, but it's convenient.
		if (n == null)
		{
			return -1;
		}
		
		// Check if we've already done the work.
		if (n.hasProperty(IC_KEY))
		{
			return (double) n.getProperty(IC_KEY);
		}
		
		// Make all the descendants push up.
		for (Node descendant : getUnpushedDescendants(n))
		{
			pushUp(descendant);
		}
		
		// TODO: Everything related to IC will need to be recalculated to
		// handle annotations.
		int descendants = (int) n.getProperty(DESCENDANT_KEY);
		double ic = (Math.log(nodeCount) - Math.log(descendants)) / Math.log(2);
		
		Transaction tx = db.beginTx();
		n.setProperty(IC_KEY, ic);
		tx.success();
		tx.finish();
		
		return ic;
	}
	
	/**
	 * Finds the least common subsumer of two nodes.
	 * 
	 * @param first		A node in the graph
	 * @param second	Another node
	 */
	public Node getLCS(Node first, final Node second) {
		final Set<Node> firstAncestors = (Set<Node>) getAncestors(first);
		System.out.println(firstAncestors);
		
		// FIXME: We don't do this yet.
//		// We traverse the ancestors of the second node in order of decreasing IC,
//		// so the first time we run across an ancestor of the first, we have the
//		// LCS.
		
		// Find the first ancestor of the first node along each path from the second.
		final Node [] lcs = {null};
		Evaluator evaluator = new Evaluator()
		{
			@Override
			public Evaluation evaluate(Path path) {
				System.out.println(path);
				Node endNode = path.endNode();
				if (getIC(lcs[0]) >= getIC(endNode))
				{
					return Evaluation.EXCLUDE_AND_PRUNE;
				}
				else if (firstAncestors.contains(endNode))
				{
					lcs[0] = endNode;
					return Evaluation.INCLUDE_AND_PRUNE;
				}
				else
				{
					return Evaluation.EXCLUDE_AND_CONTINUE;
				}
			}
		};
		
		Iterable<Path> iter = basicDownwardTraversal
				.reverse()
				.evaluator(evaluator)
				.traverse(second)
				;
		
		// The traversal is lazy, so we need to force evaluation.
		for (@SuppressWarnings("unused") Path p : iter)
		{
			continue;
		}
		
		return lcs[0];
	}
	
}

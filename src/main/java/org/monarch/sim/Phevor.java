package org.monarch.sim;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotFoundException;

public class Phevor {
	
	private PhevorDB ontology;
	private HashMap<String, Node> fragmentMap;
	// FIXME: Remove this magic number.
	private static final int RELEVANT_STEPS = 15;
	private double totalScore = 0;
	private HashMap<String, Double> scoreMap = new HashMap<>();
	
	/**
	 * Sets up the Phevor algorithm with the given linked ontologies.
	 * 
	 * @param db		The ontology to use
	 * @param edgeTypes	The names of valid types of edges
	 */
	public Phevor(GraphDatabaseService db, Collection<String> edgeTypes)
	{
		ontology = new PhevorDB(db, edgeTypes);
		fragmentMap = ontology.fragmentMap;
	}
	
	private String makeCanonicalFragment(String fragment) {
		return ontology.makeCanonicalFragment(fragment);
	}
	
	private double getScore(String fragment) {
		String canonical = makeCanonicalFragment(fragment);
		if (ontology.containsFragment(canonical))
		{
			return ontology.getScore(canonical);
		}
		
		throw new NotFoundException("Node " + canonical + " not found in the ontology.");
	}
	
	/**
	 * Finds the node associated with a fragment.
	 * 
	 * @param fragment	The fragment associated with the node
	 */
	public Node getNodeByFragment(String fragment) {
		String canonical = makeCanonicalFragment(fragment);		
		return fragmentMap.get(canonical);
	}
	
	/**
	 * Sets the nodes to expand with the Phevor algorithm.
	 * 
	 * @param fragments	The fragments associated with the nodes.
	 */
	public void setBaseNodes(Collection<String> fragments) {
		// Reset scores.
		totalScore = 0;
		scoreMap = new HashMap<>();
		
		// Find the nodes in the given ontology.
		LinkedList<Node> relevantNodes = new LinkedList<>();
		for (String fragment : fragments)
		{
			String canonical = makeCanonicalFragment(fragment);
			if (ontology.containsFragment(canonical))
			{
				relevantNodes.add(fragmentMap.get(canonical));
			}
		}

		// Set the base nodes appropriately.
		ontology.setBaseNodes(relevantNodes);
		totalScore += ontology.totalScore;
		
		// Fill the score map.
		for (String fragment : fragmentMap.keySet())
		{
			String canonical = makeCanonicalFragment(fragment);
			if (!scoreMap.containsKey(canonical))
			{
				scoreMap.put(canonical, getScore(canonical));
			}
			
			scoreMap.put(canonical, scoreMap.get(canonical) / totalScore);
		}
	}
	
	/**
	 * Finds how closely related a set of nodes is to the set of base nodes.
	 * This assumes base nodes have already been set.
	 * 
	 * @param fragments	The fragments associated with the nodes
	 */
	public double compareOtherNodes(Collection<String> fragments) {
		double maxScore = 0;
		for (String fragment : fragments)
		{
			double score = scoreMap.get(makeCanonicalFragment(fragment));
			if (score > maxScore)
			{
				maxScore = score;
			}
		}
		return maxScore;
	}
	
	/**
	 * Closes all the open resources.
	 */
	public void close() {
		ontology.close();
	}
	
	private class PhevorDB extends MappedDB {

		double totalScore = 0;
		HashMap<Node, Double> scoreMap = new HashMap<>();
		SciGraphTraverser traverser = new SciGraphTraverser(db, "phevor");

		public PhevorDB(String url, String graphLocation, boolean forceLoad) {
			super(url, graphLocation, forceLoad);
		}

		public PhevorDB(GraphDatabaseService db) {
			super(db);
		}
		
		public PhevorDB(GraphDatabaseService db, Collection<String> edgeTypes) {
			super(db);
			for (String edgeType : edgeTypes)
			{
				try
				{
					// FIXME: There should be a way to specify direction.
					traverser.relationships(edgeType, Direction.INCOMING);
				}
				catch (NotFoundException e)
				{
					// Some edge types may be in only one ontology, so
					// we shouldn't worry if an edge type isn't found.
				}
			}
		}

		private void updateHashMap(HashMap<Node, Double> map, Node n, double change) {
			// Support the equivalent of Python's defaultdict.
			if (map.containsKey(n))
			{
				map.put(n, map.get(n) + change);
			}
			else
			{
				map.put(n, change);
			}
		}

		private void updateScore(Node n, double change) {
			updateHashMap(scoreMap, n, change);
			totalScore += change;
		}

		private void expand(Node n) {
			updateScore(n, 1);

			int stepsLeft = RELEVANT_STEPS;

			HashMap<Node, Double> parents = new HashMap<>();
			HashMap<Node, Double> children = new HashMap<>();
			parents.put(n, 1.0);
			children.put(n, 1.0);
			while (stepsLeft > 0)
			{
				// TODO: This could be much cleaner.
				// Iterate out in both directions.
				HashMap<Node, Double> newParents = new HashMap<>();
				HashMap<Node, Double> newChildren = new HashMap<>();
				for (Node parent : parents.keySet())
				{
					double nextLevelScore = parents.get(parent) / 2;
					Iterable<Node> grandparents = traverser.getParents(parent);
					for (Node grandparent : grandparents)
					{
						updateHashMap(newParents, grandparent, nextLevelScore);
						updateScore(grandparent, nextLevelScore);
					}
				}
				for (Node child : children.keySet())
				{
					double nextLevelScore = children.get(child) / 2;
					Iterable<Node> grandchildren = traverser.getChildren(child);
					for (Node grandchild : grandchildren)
					{
						updateHashMap(newChildren, grandchild, nextLevelScore);
						updateScore(grandchild, nextLevelScore);
					}
				}

				// Set up the next iteration.
				parents = newParents;
				children = newChildren;
				stepsLeft--;
			}
		}

		private void expandAll(Collection<Node> nodes) {
			for (Node node : nodes)
			{
				expand((Node)node);
			}
		}

		private void rescaleAll() {
			for (Node n : scoreMap.keySet())
			{
				scoreMap.put(n, scoreMap.get(n) / totalScore);
			}
			totalScore = 1;
		}

		public void reset() {
			scoreMap = new HashMap<>();
			totalScore = 0;
		}

		public void setBaseNodes(Collection<Node> nodes) {
			reset();
			expandAll(nodes);
			if (totalScore != 0)
			{
				rescaleAll();
			}
		}
		
		public HashMap<String, Node> getFragmentMap() {
			return fragmentMap;
		}
		
		public boolean containsFragment(String fragment) {
			return fragmentMap.containsKey(fragment);
		}
		
		public double getScore(String fragment) {
			Node node = fragmentMap.get(fragment);
			if (scoreMap.containsKey(node))
			{
				return scoreMap.get(node);
			}
			return 0;
		}

	}
}
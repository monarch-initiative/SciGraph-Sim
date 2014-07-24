package org.monarch.sim;

import java.util.Collection;
import java.util.HashMap;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;

public class Phevor extends MappedDB {
	
	private double totalScore = 0;
	private HashMap<Node, Double> scoreMap = new HashMap<>(); 
	
	public Phevor(String url, String graphLocation, boolean forceLoad) {
		super(url, graphLocation, forceLoad);
	}
	
	public Phevor(GraphDatabaseService oldDB) {
		super(oldDB);
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
		
		// FIXME: Remove magic number.
		int relevantLevels = 5;
		
		HashMap<Node, Double> parents = new HashMap<>();
		HashMap<Node, Double> children = new HashMap<>();
		parents.put(n, 1.0);
		children.put(n, 1.0);
		while (relevantLevels > 0)
		{
			// TODO: This could be much cleaner.
			// Iterate out in both directions.
			HashMap<Node, Double> newParents = new HashMap<>();
			HashMap<Node, Double> newChildren = new HashMap<>();
			for (Node parent : parents.keySet())
			{
				double nextLevelScore = parents.get(parent) / 2;
				Iterable<Node> grandparents = Neo4jTraversals.getParents(parent);
				for (Node grandparent : grandparents)
				{
					updateHashMap(newParents, grandparent, nextLevelScore);
					updateScore(grandparent, nextLevelScore);
				}
			}
			for (Node child : children.keySet())
			{
				double nextLevelScore = children.get(child) / 2;
				Iterable<Node> grandchildren = Neo4jTraversals.getChildren(child);
				for (Node grandchild : grandchildren)
				{
					updateHashMap(newChildren, grandchild, nextLevelScore);
					updateScore(grandchild, nextLevelScore);
				}
			}
			
			// Set up the next iteration.
			parents = newParents;
			children = newChildren;
			relevantLevels--;
		}
	}
	
	private void expand(String fragment) {
		expand(getNodeByFragment(fragment));
	}
	
	private void expandAll(Collection<?> nodes) {
		for (Object node : nodes)
		{
			// Handle nodes and fragments.
			if (node instanceof Node)
			{
				expand((Node)node);
			}
			else if (node instanceof String)
			{
				expand((String)node);
			}
			else
			{
				// FIXME: This should throw an error.
			}
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
	
	public void setBaseNodes(Collection<?> nodes) {
		reset();
		expandAll(nodes);
		rescaleAll();
	}
	
	public double compareOtherNodes(Collection<?> nodes) {
		double maxScore = 0;
		
		for (Object node : nodes)
		{
			double score;
			Node toLookup;
			// Handle nodes and fragments.
			if (node instanceof Node)
			{
				toLookup = (Node)node;
			}
			else if (node instanceof String)
			{
				toLookup = fragmentMap.get((String)node);
			}
			else
			{
				// This should throw an error.
				toLookup = null;
			}
			
			// Look up the score.
			if (scoreMap.containsKey(toLookup))
			{
				score = scoreMap.get(toLookup);
			}
			else
			{
				score = 0;
			}
						
			if (score > maxScore)
			{
				maxScore = score;
			}
		}
		
		return maxScore;
	}
	
}

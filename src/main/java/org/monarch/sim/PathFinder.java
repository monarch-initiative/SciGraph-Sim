package org.monarch.sim;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;

public class PathFinder extends MappedDB {
	
	public PathFinder(String url, String graphLocation, boolean forceLoad) {
		super(url, graphLocation, forceLoad);
	}
	
	public PathFinder(GraphDatabaseService oldDB) {
		super(oldDB);
	}
	
	public Iterable<String []> getPairs(String filename) {
		ArrayList<String []> pairs = new ArrayList<>();
		
		try
		{
			// Get the lines from the appropriate file.
			BufferedReader reader = new BufferedReader(new FileReader(filename));
			String line;
			while ((line = reader.readLine()) != null)
			{
				// Pull out the two columns we care about.
				String [] pieces = line.split("\t");
				String [] pair = new String [2];
				pair[0] = pieces[0];
				pair[1] = pieces[3];
				pairs.add(pair);
			}
			reader.close();
		}
		catch (Exception e)
		{
			System.out.println(e.getMessage());
		}
		
		return pairs;
	}
	
	public Node getLCS(String [] pair) {
		Node first = fragmentMap.get(makeCanonical("fragment", pair[0]));
		Node second = fragmentMap.get(makeCanonical("fragment", pair[1]));
		
		// Check that both nodes exist.
		boolean failed = false;
		if (first == null)
		{
			System.out.println("Node " + pair[0] + " does not exist.");
			failed = true;
		}
		if (second == null)
		{
			System.out.println("Node " + pair[1] + " does not exist.");
			failed = true;
		}
		if (failed)
		{
			return null;
		}
		
		System.out.println(nodeToString(first) + " ancestors:");
		for (Node n : Neo4jTraversals.getAncestors(first))
		{
			System.out.println(nodeToString(n));
		}
		System.out.println();
		System.out.println(nodeToString(second) + " ancestors:");
		for (Node n : Neo4jTraversals.getAncestors(second))
		{
			System.out.println(nodeToString(n));
		}
		System.out.println();
		
		return Neo4jTraversals.getLCS(first, second);
	}

}

package org.monarch.sim;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;

import com.tinkerpop.pipes.util.structures.Pair;

@Deprecated
public class PathFinder extends MappedDB {
	
	public PathFinder(String url, String graphLocation, boolean forceLoad) {
		super(url, graphLocation, forceLoad);
	}
	
	public PathFinder(GraphDatabaseService oldDB) {
		super(oldDB);
	}
	
	public Collection<Pair<String, String>> getPairs(String filename) {
		ArrayList<Pair<String, String>> pairs = new ArrayList<>();
		
		try
		{
			// Get the lines from the appropriate file.
			BufferedReader reader = new BufferedReader(new FileReader(filename));
			String line;
			while ((line = reader.readLine()) != null)
			{
				// Pull out the two columns we care about.
				String [] pieces = line.split("\t");
				Pair<String, String> pair = new Pair<>(pieces[0], pieces[3]);
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
	
	public Node getLCS(Pair<String, String> pair) {
		Node first = getNodeByFragment(pair.getA());
		Node second = getNodeByFragment(pair.getB());
		
		// Check that both nodes exist.
		boolean failed = false;
		if (first == null)
		{
			System.out.println("Node " + pair.getA() + " does not exist.");
			failed = true;
		}
		if (second == null)
		{
			System.out.println("Node " + pair.getB() + " does not exist.");
			failed = true;
		}
		if (failed)
		{
			return null;
		}
				
		return Neo4jTraversals.getLCS(first, second);
	}
	
	public List<String> getShortestPath(String first, String second) {
		List<Node> path = Neo4jTraversals.getShortestPath(getNodeByFragment(first), getNodeByFragment(second));
		List<String> fragmentPath = new LinkedList<>();
		for (Node n : path)
		{
			fragmentPath.add((String) n.getProperty("fragment"));
		}
		return fragmentPath;
	}

}

package org.monarch.sim;

import static org.monarch.sim.GremlinTraversals.getAncestors;
import static org.monarch.sim.GremlinTraversals.getCommonAncestors;
import static org.monarch.sim.GremlinTraversals.getParents;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.tooling.GlobalGraphOperations;

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;

public class GremlinTest {
	
	// A simple tree with only upward edges.
	static Graph tree;
	// The graph from the monarchGraph folder.
	static Graph monarch;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
//		buildTree();
//		buildMonarchGraph();
	}

	private static void buildTree() {
		// Plant a tree.
		tree = new TinkerGraph();
		ArrayList<Vertex> vertices = new ArrayList<>();
		vertices.add(null);
		for (int i = 1; i < 256; i++)
		{
			Vertex newVertex = tree.addVertex(i);
			newVertex.setProperty("index", i);
			vertices.add(newVertex);
			if (i != 1)
			{
				tree.addEdge(null, vertices.get(i), vertices.get(i / 2), "child");
			}
		}
	}
	
	private static void buildMonarchGraph() {
		// Build a database from the monarchGraph folder.
		monarch = new TinkerGraph();
		
		// All the edges in the given graph are undirected, so we need to rebuild.
		GraphDatabaseService tempDB = new GraphDatabaseFactory().newEmbeddedDatabase("monarchGraph");
		for (Node n : GlobalGraphOperations.at(tempDB).getAllNodes())
		{
			if (n.getId() == 0)
			{
				continue;
			}
			Vertex v = monarch.addVertex(n.getId());
			for (String property : n.getPropertyKeys())
			{
				v.setProperty(property, n.getProperty(property));
			}
		}
		
		// Expand outward starting with node 1.
		HashSet<Node> visited = new HashSet<>();
		LinkedList<Node> toExpand = new LinkedList<>();
		toExpand.add(tempDB.getNodeById(1));
		while (!toExpand.isEmpty())
		{
			Node next = toExpand.removeFirst();
			if (!visited.add(next))
			{
				continue;
			}

			for (Relationship edge : next.getRelationships(Direction.INCOMING))
			{
				Node child = edge.getStartNode();
				Node parent = edge.getEndNode();
				if (!visited.contains(child))
				{
					monarch.addEdge(null, monarch.getVertex(child.getId()),
									monarch.getVertex(parent.getId()), "child");
					toExpand.add(child);
				}
			}
		}
	}
	
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		// Clean up existing graphs.
//		tree.shutdown();
//		monarch.shutdown();
	}
	
	// Make sure getParents works.
	public void checkParents() {
		// Check that our tree works.
		for (Vertex child : tree.getVertices())
		{
			// The root of our tree has no parents.
			if ((Integer)child.getProperty("index") == 0)
			{
				continue;
			}
			
			Iterable<Vertex> parents = getParents(child);
			int count = 0;
			// In our tree, the parent index is always half the child index.
			for (Vertex parent : parents)
			{
				count++;
				assert (Integer)parent.getProperty("index") == (Integer)child.getProperty("index") / 2;
			}
			// In our tree, each node has only one parent.
			assert count == 1;
		}
	}
	
	// Make sure getAncestors works.
	public void checkAncestors(){
		// Check that our tree works.
		for (Vertex child : tree.getVertices())
		{
			// In our tree, the parent index is always half the child index,
			// so we can find the index of each ancestor.
			ArrayList<Integer> expected = new ArrayList<>();
			int index = (Integer)child.getProperty("index");
			while (index > 0)
			{
				expected.add(index);
				index /= 2;
			}
			
			int count = 0;
			// Make sure each ancestor has an expected index.
			for (Vertex ancestor : getAncestors(child))
			{
				count++;
				assert expected.contains((Integer)ancestor.getProperty("index"));
			}
			// Make sure we have the right number of ancestors.
			assert count == expected.size();
		}
	}
	
	public void printGraph(Graph g) {
		System.out.println("Vertices:");
		for (Vertex v : g.getVertices())
		{
			System.out.println(v.toString() + ": " + v.getProperty("index"));
		}
		System.out.println("Edges:");
		for (Edge e : g.getEdges())
		{
			System.out.println(e);
		}
	}
	
	private void validateMonarch() {
		// Keep track of some statistics.
		int trials = 1000;
		int totalCommonAncestors = 0;
//		HashMap<Vertex, Integer> subsumerCounts = new HashMap<>();
//		int relevantSubsumerCount = 5;
		
		// We should get a sense of which subsumer method is faster.
		long ancestorTime = 0;
//		long lcsTime = 0;
		long start, end;
		
		// We need to know how many nodes there are to choose one uniformly.
		int count = 0;
		for (@SuppressWarnings("unused") Vertex v : monarch.getVertices())
		{
			count++;
		}
		
		for (int i = 0; i < trials; i++)
		{
			// Pick any two nodes.
			Random rand = new Random();
			int j, k;
			Vertex m = null;
			Vertex n = null;
			while (m == null || n == null)
			{
				j = rand.nextInt(count);
				k = rand.nextInt(count);
				m = monarch.getVertex(j);
				n = monarch.getVertex(k);
			}
//			System.out.println("VERTICES: " + m.getId() + " " + n.getId());
			
			// Find all common ancestors.
//			System.out.println("Common Ancestors:");
			start = System.nanoTime();
			Iterable<Vertex> ancestors = getCommonAncestors(m, n);
			end = System.nanoTime();
			ancestorTime += (end - start);
			for (Vertex ancestor : ancestors)
			{
//				System.out.println(ancestor.getId());
				totalCommonAncestors++;
			}			
			
//			// Find the LCS.
//			start = System.nanoTime();
//			Node lcs = getLCS(m, n);
//			end = System.nanoTime();
//			lcsTime += (end - start);
//			if (lcs == null)
//			{
//				System.out.println("LCS: Null");
//				continue;
//			}
//			if (!subsumerCounts.containsKey(lcs))
//			{
//				subsumerCounts.put(lcs, 0);
//			}
//			subsumerCounts.put(lcs, subsumerCounts.get(lcs) + 1);
//			System.out.println("LCS: " + lcs.getProperty("name"));
//			System.out.println();
		}
		
		// Process statistics.
		double averageCommonAncestors = totalCommonAncestors * 1.0 / trials;
		// This would be trivial in a sensible language.
//		List<Entry<Node, Integer>> entries = new LinkedList<>(subsumerCounts.entrySet());
//		Collections.sort(entries, new Comparator<Entry<Node, Integer>>()
//				{
//			public int compare(Entry<Node, Integer> first, Entry<Node, Integer> second) {
//				return first.getValue().compareTo(second.getValue());
//			}
//				});
//		Collections.reverse(entries);

		// Report to the user.
		System.out.println("Trials: " + trials);
		System.out.println("Milliseconds to compute ancestors: " + ancestorTime / 1000000);
//		System.out.println("Milliseconds to compute LCS: " + lcsTime / 1000000);
		System.out.println("Average common ancestors: " + averageCommonAncestors);
//		System.out.println("Common LCS nodes:");
//		for (Entry<Node, Integer> entry : entries)
//		{
//			if (entry.getValue() == 1)
//			{
//				break;
//			}
//			System.out.println("Node " + entry.getKey().getProperty("name") + ": " + entry.getValue() + " times");
//			System.out.println(entry.getKey().getProperty("label"));
//			relevantSubsumerCount--;
//			if (relevantSubsumerCount == 0)
//			{
//				break;
//			}
//		}
	}

	@Test
	public void test() {
//		checkParents();
//		checkAncestors();
//		validateMonarch();
	}

}

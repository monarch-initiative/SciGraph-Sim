package org.monarch.sim;

import static org.junit.Assert.*;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.apache.tools.ant.types.selectors.modifiedselector.Algorithm;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;
import com.tinkerpop.gremlin.java.GremlinPipeline;
import com.tinkerpop.pipes.PipeFunction;
import com.tinkerpop.pipes.branch.LoopPipe;
import com.tinkerpop.pipes.branch.LoopPipe.LoopBundle;

public class GremlinTest {
	
	// A simple tree with only upward edges.
	static Graph tree;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
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

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		// Clean up existing graphs.
		tree.shutdown();
	}
	
	// TODO: Move to implementation file.
	public Iterable<Vertex> getParents(Vertex v) {
		LinkedList<Vertex> parents = new LinkedList<>();
		// Follow each outgoing edge to find a parent.
		for (Edge e : v.getEdges(Direction.OUT))
		{
			parents.add(e.getVertex(Direction.IN));
		}
		return parents;
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
	
	// TODO: Move to implementation file.
	public Iterable<Vertex> getAncestors(Vertex v) {
		// Keep track of the ancestors we've found.
		HashSet<Vertex> ancestors = new HashSet<>();
		
		GremlinPipeline<Vertex, Vertex> pipe = new GremlinPipeline<>();
		pipe.start(v)
			.as("x")
			// Look at all the neighbors we haven't seen yet.
			.out()
			.except(ancestors)
			.store(ancestors)
			// Repeat until we run out of neighbors.
			.loop("x", new PipeFunction<LoopBundle<Vertex>, Boolean>()
				{
					public Boolean compute(LoopBundle<Vertex> bundle)
					{
						return true;
					}
				})
			.iterate()
			;
		
		return ancestors;
	}
	
	// TODO: Move to implementation file.
	public ArrayList<HashSet<Vertex>> getAncestorsByDistance(Vertex v) {
//		// Keep track of the paths and vertices we've seen.
//		Collection<List<Vertex>> paths = new LinkedList<>();
//		HashSet<Vertex> visited = new HashSet<>();
//		visited.add(v);
//		
//		GremlinPipeline<Vertex, ArrayList<Vertex>> pipe = new GremlinPipeline<>();
//		pipe.start(v)
//			.as("x")
//			// Look at all the neighbors we haven't seen yet.
//			.out()
//			.except(visited)
//			.store(visited)
//			.path()
//			.store((Collection)paths)
//			// FIXME: Get the last node in the path.
//			// Repeat until we run out of neighbors.
//			.loop("x", new PipeFunction<LoopBundle<Vertex>, Boolean>()
//				{
//					public Boolean compute(LoopBundle<Vertex> bundle)
//					{
//						return true;
//					}
//				})
//			;
//
//		for (ArrayList<Vertex> path : pipe)
//		{
//			System.out.println(path);
//		}
//		
//		// Turn our paths into our list of sets of vertices.
//		ArrayList<HashSet<Vertex>> ancestors = new ArrayList<>();
//		for (List<Vertex> path : paths)
//		{
//			// Pad the list to the size we need.
//			int length = path.size();
//			while (length > ancestors.size())
//			{
//				ancestors.add(new HashSet<Vertex>());
//			}
//			
//			ancestors.get(length - 1).add((Vertex)path.get(length - 1));
//		}
//		
//		return ancestors;
		// FIXME: Make this work.
		return null;
	}
	
	// TODO: Move to implementation file.
	public ArrayList<Vertex> shortestPath(Vertex start, final Vertex end) {
		// Keep track of the vertices we've seen.
		HashSet<Vertex> visited = new HashSet<>();
		visited.add(start);
		
		GremlinPipeline<Vertex, ArrayList<Vertex>> pipe = new GremlinPipeline<>();
		pipe.start(start)
			.as("x")
			// Look at all the neighbors we haven't seen yet.
			.out()
			.except(visited)
			.store(visited)
			// Repeat until we find what we're looking for or run out of neighbors.
			.loop("x", new PipeFunction<LoopBundle<Vertex>, Boolean>()
				{
					public Boolean compute(LoopBundle<Vertex> bundle)
					{
						return bundle.getObject().getId() != end.getId();
					}
				})
			.path()
			;
		
		// Pull out the shortest path.
		for (ArrayList<Vertex> path : pipe)
		{
			return path;
		}
		
		// If this wasn't a toy example, this would horrify me.
		return null;
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

	@Test
	public void test() {
		checkParents();
		checkAncestors();
//		int count = 0;
//		ArrayList<HashSet<Vertex>> ancestors = getAncestorsByDistance(tree.getVertex(42));
//		for (HashSet<Vertex> group : ancestors)
//		{
//			System.out.println("Distance " + count);
//			count++;
//			for (Vertex v : group)
//			{
//				System.out.println(v);
//			}
//			System.out.println();
//		}
	}

}

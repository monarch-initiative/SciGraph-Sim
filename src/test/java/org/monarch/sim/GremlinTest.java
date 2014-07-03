package org.monarch.sim;

import static org.junit.Assert.*;

import java.awt.List;
import java.util.ArrayList;
import java.util.Collections;
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

public class GremlinTest {
	
	// A simple tree with only upward edges.
	static Graph tree;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		// Plant a tree.
		tree = new TinkerGraph();
		ArrayList<Vertex> vertices = new ArrayList<Vertex>();
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
		LinkedList<Vertex> parents = new LinkedList<Vertex>();
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
		// GremlinPipeline<Vertex, Vertex> pipe = new GremlinPipeline<Vertex, Vertex>(v);
		// FIXME: Write this.
		return null;
	}
	
	// TODO: Move to implementation file.
	public ArrayList<Set<Vertex>> getAncestorsByDistance(Vertex v) {
		// FIXME: Write this.
		return null;
	}
	
	// TODO: Move to implementation file.
	public int directedDistance(Vertex start, Vertex end) {
		// FIXME: Write this
		return -1;
	}
	
	// Make sure getAncestors works.
	public void checkAncestors(){
		// Check that our tree works.
		for (Vertex child : tree.getVertices())
		{
			// In our tree, the parent index is always half the child index,
			// so we can find the index of each ancestor.
			ArrayList<Integer> expected = new ArrayList<Integer>();
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
	}

}

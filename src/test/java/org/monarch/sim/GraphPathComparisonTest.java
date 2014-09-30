package org.monarch.sim;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import junit.framework.Assert;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.test.TestGraphDatabaseFactory;
import org.neo4j.tooling.GlobalGraphOperations;

public class GraphPathComparisonTest extends TestGraphFactory {
	
	// Define the relationships we want to use.
	enum RelTypes implements RelationshipType {
		SUBCLASS_OF,
		GOOD,
		BAD,
	}

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}
	
	public void checkEdges(GraphDatabaseService db, Collection<Node> first, Collection<Node> second) {
		NaiveTraverser traverser = new NaiveTraverser(db);
		traverser.pushAllNodes(new ArrayList<String>());
		GraphPathComparison comparison = new GraphPathComparison(traverser);
		
		Collection<Relationship> edges = comparison.getMinimalChange(first, second);
		for (Relationship edge : GlobalGraphOperations.at(db).getAllRelationships())
		{
			if (edges.contains(edge))
			{
				assertTrue(edge.getType().equals(RelTypes.GOOD));
			}
			else
			{
				assertFalse(edge.getType().equals(RelTypes.GOOD));
			}
		}
	}

	@Test
	public void trivialTest() {
		// Test the comparison on two disjoint ^ shaped components.
		GraphDatabaseService db = new TestGraphDatabaseFactory().newImpermanentDatabase();
		Node a = addNode(db);
		Node b = addNode(db);
		Node c = addNode(db);
		Node d = addNode(db);
		Node e = addNode(db);
		Node f = addNode(db);
		addEdge(db, a, b, RelTypes.GOOD);
		addEdge(db, c, b, RelTypes.GOOD);
		addEdge(db, d, e, RelTypes.GOOD);
		addEdge(db, f, e, RelTypes.GOOD);
		
		Collection<Node> first = Arrays.asList(a, d);
		Collection<Node> second = Arrays.asList(c, f);
		
		checkEdges(db, first, second);
	}
	
	@Test
	public void joinTest() {
		// Test the comparison when two paths join to get a shorter combined route.
		GraphDatabaseService db = new TestGraphDatabaseFactory().newImpermanentDatabase();
		Node a = addNode(db);
		Node b = addNode(db);
		Node c = addNode(db);
		Node d = addNode(db);
		Node e = addNode(db);
		
		addEdge(db, a, d, RelTypes.GOOD);
		addEdge(db, b, e, RelTypes.GOOD);
		addEdge(db, c, e, RelTypes.GOOD);
		addEdge(db, e, d, RelTypes.GOOD);
		
		addEdge(db, b, d, RelTypes.BAD);
		addEdge(db, c, d, RelTypes.BAD);

		Collection<Node> first = Arrays.asList(a);
		Collection<Node> second = Arrays.asList(b, c);
		
		checkEdges(db, first, second);
	}
	
	@Test
	public void splitTest() {
		// Test that the correspondence doesn't need to be unique in either direction.
		GraphDatabaseService db = new TestGraphDatabaseFactory().newImpermanentDatabase();
		Node a = addNode(db);
		Node b = addNode(db);
		Node c = addNode(db);
		Node d = addNode(db);
		Node e = addNode(db);
		Node f = addNode(db);
		Node g = addNode(db);
		Node h = addNode(db);
		
		addEdge(db, a, g, RelTypes.GOOD);
		addEdge(db, b, g, RelTypes.GOOD);
		addEdge(db, c, g, RelTypes.GOOD);
		addEdge(db, d, h, RelTypes.GOOD);
		addEdge(db, e, h, RelTypes.GOOD);
		addEdge(db, f, h, RelTypes.GOOD);
		
		List<Node> first = Arrays.asList(a, d, e);
		List<Node> second = Arrays.asList(b, c, f);
		
		checkEdges(db, first, second);
	}
	
	@Test
	public void irrelevantTest() {
		// Test that edges which don't connect nodes in question are ignored.
		GraphDatabaseService db = new TestGraphDatabaseFactory().newImpermanentDatabase();
		Node a = addNode(db);
		Node b = addNode(db);
		Node c = addNode(db);
		Node d = addNode(db);
		Node e = addNode(db);
		
		Node ab = addNode(db);
		Node cd = addNode(db);
		Node root = addNode(db);
		
		addEdge(db, a, ab, RelTypes.GOOD);
		addEdge(db, b, ab, RelTypes.GOOD);
		addEdge(db, c, cd, RelTypes.GOOD);
		addEdge(db, d, cd, RelTypes.GOOD);
		
		addEdge(db, ab, root, RelTypes.BAD);
		addEdge(db, cd, root, RelTypes.BAD);
		
		addEdge(db, e, root, RelTypes.BAD);
		addEdge(db, e, ab, RelTypes.BAD);
		addEdge(db, e, cd, RelTypes.BAD);
		
		List<Node> first = Arrays.asList(a, c);
		List<Node> second = Arrays.asList(b, d);
		
		checkEdges(db, first, second);
	}
	
}

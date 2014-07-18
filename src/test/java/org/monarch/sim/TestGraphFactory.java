package org.monarch.sim;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import net.lingala.zip4j.core.ZipFile;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.test.TestGraphDatabaseFactory;
import org.neo4j.tooling.GlobalGraphOperations;

import com.google.common.io.Files;

public class TestGraphFactory {
	
	// Define the relationships we want to use.
	enum RelTypes implements RelationshipType {
		SUBCLASS,
	}
	
	private static Node addNode(GraphDatabaseService db) {
		// Wrap a transaction around node creation.
		Transaction tx = db.beginTx();
		Node newNode = db.createNode();
		tx.success();
		tx.finish();
		return newNode;
	}
	
	private static Node addNode(GraphDatabaseService db, String name) {
		// Wrap a transaction around node creation.
		Transaction tx = db.beginTx();
		Node newNode = db.createNode();
		newNode.setProperty("name", name);
		tx.success();
		tx.finish();
		return newNode;
	}
	
	private static Relationship addEdge(GraphDatabaseService db, Node first, Node second)
	{
		// Wrap a transaction around edge creation.
		Transaction tx = db.beginTx();
		Relationship newRel = first.createRelationshipTo(second, RelTypes.SUBCLASS);
		tx.success();
		tx.finish();
		return newRel;		
	}
	
	public GraphDatabaseService buildWaterDB() {
		// Build a database with three nodes in a ^ configuration.
		GraphDatabaseService waterDB = new TestGraphDatabaseFactory().newImpermanentDatabase();
		Node waterA = addNode(waterDB, "A");
		Node waterB = addNode(waterDB, "B");
		Node waterC = addNode(waterDB, "C");
		addEdge(waterDB, waterA, waterB);
		addEdge(waterDB, waterC, waterB);
		
		Neo4jTraversals.setAllIC(waterDB);
		
		return waterDB;
	}
	
	public GraphDatabaseService buildCompleteDB(int numNodes) {
		// Build a complete graph with edges directed toward the lower indices.
		GraphDatabaseService completeDB = new TestGraphDatabaseFactory().newImpermanentDatabase();
		ArrayList<Long> ids = new ArrayList<>();
		for (int i = 1; i <= numNodes; i++)
		{
			Node newNode = addNode(completeDB, "" + i);
			for (Long id : ids)
			{
				addEdge(completeDB, newNode, completeDB.getNodeById(id));
			}
			ids.add(newNode.getId());
		}
		
		Neo4jTraversals.setAllIC(completeDB);
		
		return completeDB;
	}

	public GraphDatabaseService buildTreeDB(int numNodes) {
		// Build a balanced binary tree with edges directed toward the lower indices.
		GraphDatabaseService treeDB = new TestGraphDatabaseFactory().newImpermanentDatabase();
		for (int i = 1; i <= numNodes; i++)
		{
			Node newNode = addNode(treeDB, "" + i);
			if (i != 1)
			{
				addEdge(treeDB, newNode, treeDB.getNodeById(i / 2));
			}
		}
		
		Neo4jTraversals.setAllIC(treeDB);
		
		return treeDB;
	}
	
	public GraphDatabaseService buildCycleDB() {
		// Build a small pathological graph.
		GraphDatabaseService cycleDB = new TestGraphDatabaseFactory().newImpermanentDatabase();
		Node a = addNode(cycleDB, "A");
		Node b = addNode(cycleDB, "B");
		Node c = addNode(cycleDB, "C");
		Node d = addNode(cycleDB, "D");
		Node e = addNode(cycleDB, "E");
		addEdge(cycleDB, b, a);
		addEdge(cycleDB, c, b);
		addEdge(cycleDB, d, c);
		addEdge(cycleDB, e, d);
		addEdge(cycleDB, e, a);
		
		Neo4jTraversals.setAllIC(cycleDB);
		
		return cycleDB;
	}
	
	public GraphDatabaseService buildMonarchDB() {
		// Build a database from the monarchGraph folder.
		GraphDatabaseService monarchDB = new TestGraphDatabaseFactory().newImpermanentDatabase();

		// All the edges in the given graph are undirected, so we need to rebuild.
		Transaction tx = monarchDB.beginTx();
		
		// Copy data into a test folder.
		String tempPath = "";
		try
		{
			File tempFolder = Files.createTempDir();
			tempPath = tempFolder.getAbsolutePath();
			ZipFile zipped = new ZipFile(new File("monarchGraph.zip").getAbsolutePath());
			zipped.extractAll(tempPath);
			tempPath += "/monarchGraph";
			String lockFile = tempPath + "/lock";
			java.nio.file.Files.deleteIfExists(Paths.get(lockFile));
		}
		catch (Exception e)
		{
			// FIXME: This should probably do something better.
			System.out.println(e.getMessage());
		}
		
		// Build the database.
		GraphDatabaseService tempDB = new GraphDatabaseFactory().newEmbeddedDatabase(tempPath);		
		
		HashMap<Node, Node> map = new HashMap<>();
		Node tempRoot = null;
		for (Node n : GlobalGraphOperations.at(tempDB).getAllNodes())
		{
			if (n.getId() == 0)
			{
				continue;
			}
			if (tempRoot == null || n.getId() < tempRoot.getId())
			{
				tempRoot = n;
			}
			Node newNode = addNode(monarchDB, "" + n.getId());
			for (String property : n.getPropertyKeys())
			{
				newNode.setProperty(property, n.getProperty(property));
			}
			map.put(n, newNode);
		}
		tx.success();
		tx.finish();
		
		// Expand outward starting with the root node.
		HashSet<Node> visited = new HashSet<>();
		LinkedList<Node> toExpand = new LinkedList<>();
		toExpand.add(tempRoot);
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
				if (!visited.contains(child))
				{
					addEdge(monarchDB, map.get(child), map.get(next));
					toExpand.add(child);
				}
			}
		}
		
		// Point all nodes without edges to the root node.
		for (Node n : GlobalGraphOperations.at(monarchDB).getAllNodes())
		{
			boolean found = false;
			for (@SuppressWarnings("unused") Relationship edge : n.getRelationships())
			{
				found = true;
				break;
			}
			if (!found)
			{
				addEdge(monarchDB, n, map.get(tempRoot));
			}
		}
		
		Neo4jTraversals.setAllIC(monarchDB);
		
		return monarchDB;
	}

	public GraphDatabaseService buildWineDB() {
		OwlTestUtil.loadOntology("http://www.w3.org/TR/owl-guide/wine.rdf", "target/wine");
		GraphDatabaseService wineDB = new GraphDatabaseFactory().newEmbeddedDatabase("target/wine");
		
		Neo4jTraversals.setAllIC(wineDB);
		
		return wineDB;
	}

}

package org.monarch.sim;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;

import net.lingala.zip4j.core.ZipFile;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.test.TestGraphDatabaseFactory;
import org.neo4j.tooling.GlobalGraphOperations;

import com.google.common.io.Files;

public class TestGraphFactory extends GraphFactory {
	
	public GraphDatabaseService buildWaterDB() {
		// Build a database with three nodes in a ^ configuration.
		GraphDatabaseService waterDB = new GraphDatabaseFactory()
			.newEmbeddedDatabaseBuilder("target/water")
			.newGraphDatabase()
			;
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
		GraphDatabaseService completeDB = new GraphDatabaseFactory()
			.newEmbeddedDatabaseBuilder("target/complete")
			.newGraphDatabase()
			;
		ArrayList<Long> ids = new ArrayList<>();
		for (int i = 1; i <= numNodes; i++)
		{
			Node newNode = addNode(completeDB, "" + i);
			Transaction tx = completeDB.beginTx();
			newNode.setProperty("fragment", "COMPLETE:" + newNode.getId());
			tx.success();
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
		GraphDatabaseService treeDB = new GraphDatabaseFactory()
			.newEmbeddedDatabaseBuilder("target/tree")
			.newGraphDatabase()
			;
		for (int i = 1; i <= numNodes; i++)
		{
			Node newNode = addNode(treeDB, "" + i);
			Transaction tx = treeDB.beginTx();
			newNode.setProperty("fragment", "TREE:" + newNode.getId());
			tx.success();
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
		GraphDatabaseService cycleDB = new GraphDatabaseFactory()
			.newEmbeddedDatabaseBuilder("target/cycle")
			.newGraphDatabase()
			;
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
		GraphDatabaseService monarchDB = new GraphDatabaseFactory().newEmbeddedDatabase(tempPath);
		removeUnlabeledEdges(monarchDB);
		
		// Remove all nodes without edges.
		Transaction tx = monarchDB.beginTx();
		for (Node n : GlobalGraphOperations.at(monarchDB).getAllNodes())
		{
			if (!n.getRelationships().iterator().hasNext() && n.getId() != 0)
			{
				n.delete();
			}
		}
		tx.success();
		
		Neo4jTraversals.setAllIC(monarchDB);
		
		return monarchDB;
	}

	public GraphDatabaseService buildWineDB() {
		
		GraphDatabaseService db = buildOntologyDB("http://www.w3.org/TR/owl-guide/wine.rdf", "target/wine");
		removeUnlabeledEdges(db);
		return db;
	}
	
	public GraphDatabaseService buildMPSubsetDB() {
		GraphDatabaseService db = buildOntologyDB("./src/test/resources/ontologies/mp-subset.owl", "target/mp-subset");
		removeUnlabeledEdges(db);
		return db;
	}

}

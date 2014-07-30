package org.monarch.sim;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Logger;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.tooling.GlobalGraphOperations;

public class GraphFactory {
	
  private static final Logger logger = Logger.getLogger(GraphFactory.class.getName());

	// Define the relationships we want to use.
	enum RelTypes implements RelationshipType {
		SUBCLASS,
	}
	
	protected static Node addNode(GraphDatabaseService db) {
		// Wrap a transaction around node creation.
		Transaction tx = db.beginTx();
		Node newNode = db.createNode();
		tx.success();
		tx.finish();
		return newNode;
	}
	
	protected static Node addNode(GraphDatabaseService db, String name) {
		// Wrap a transaction around node creation.
		Transaction tx = db.beginTx();
		Node newNode = db.createNode();
		newNode.setProperty("name", name);
		tx.success();
		tx.finish();
		return newNode;
	}
	
	protected static Relationship addEdge(GraphDatabaseService db, Node first, Node second)
	{
		// Wrap a transaction around edge creation.
		Transaction tx = db.beginTx();
		Relationship newRel = first.createRelationshipTo(second, RelTypes.SUBCLASS);
		tx.success();
		tx.finish();
		return newRel;		
	}

	protected void removeUnlabeledEdges(GraphDatabaseService db) {
		Transaction tx = db.beginTx();
		
		// Check if each edge has a label.
		for (Relationship edge : GlobalGraphOperations.at(db).getAllRelationships())
		{
			if (!edge.hasProperty("fragment"))
			{
				edge.delete();
			}
		}
		
		tx.success();
		tx.finish();
	}
	
	private GraphDatabaseService setAllIC(GraphDatabaseService db)
	{
		logger.info("Starting setAllIC");
		Neo4jTraversals.setAllIC(db);
		logger.info("Finished setAllIC");
		return db;
	}
	
	// TODO: Set IC scores as we need them.
	public GraphDatabaseService loadOntologyDB(String graphLocation) {
		GraphDatabaseService db = loadOntologyDBHelper(graphLocation);
		return setAllIC(db);
	}

	private GraphDatabaseService loadOntologyDBHelper(String graphLocation) {
		GraphDatabaseService db = new GraphDatabaseFactory().newEmbeddedDatabase(graphLocation);
		return db;
	}
	
	public GraphDatabaseService buildOntologyDB(String url, String graphLocation) {
		return buildOntologyDB(url, graphLocation, true);
	}
	
	public GraphDatabaseService buildOntologyDB(String url, String graphLocation, boolean forceRebuild) {
		GraphFactory factory = new GraphFactory();
		GraphDatabaseService db;
		
		String absolutePath = new File(graphLocation).getAbsolutePath();
		String flagPath = absolutePath + "/buildCompletedFlag";
		
		// If we were told to rebuild or the flag isn't set, build from
		// the given url.
		if (forceRebuild || !Files.exists(Paths.get(flagPath)))
		{
			OwlUtil.loadOntology(url, graphLocation);
			db = loadOntologyDBHelper(graphLocation);
			try {
				// Create the flag once the ontology is built.
				new File(flagPath).createNewFile();
			} catch (IOException e) {
				// FIXME: This should probably be handled better.
				System.out.println(e.getMessage());
			}
		}
		// Otherwise, we've already got the graph stored, and we can use that.
		else
		{
			db = factory.loadOntologyDBHelper(graphLocation);
		}
		
		return setAllIC(db);
	}
	
}

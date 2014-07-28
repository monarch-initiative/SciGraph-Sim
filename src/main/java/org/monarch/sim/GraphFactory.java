package org.monarch.sim;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.tooling.GlobalGraphOperations;

public class GraphFactory {
	
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
	
	public GraphDatabaseService buildOntologyDB(String url, String graphLocation) {
		OwlUtil.loadOntology(url, graphLocation);
		
		return loadOntologyDB(graphLocation);
	}
	
	public GraphDatabaseService loadOntologyDB(String graphLocation) {
		GraphDatabaseService db = new GraphDatabaseFactory().newEmbeddedDatabase(graphLocation);
		
		Neo4jTraversals.setAllIC(db);
		
		return db;
	}

}

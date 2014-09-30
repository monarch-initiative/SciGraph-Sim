package org.monarch.sim;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.tooling.GlobalGraphOperations;

/**
 * This class provides methods for creating Neo4j graphs.
 * 
 * @author spikeharris
 */
public class GraphFactory {
	
  private static final Logger logger = Logger.getLogger(GraphFactory.class.getName());

	// Define the relationships we want to use.
	enum RelTypes implements RelationshipType {
		SUBCLASS_OF,
	}
	
	protected Node addNode(GraphDatabaseService db) {
		// Wrap a transaction around node creation.
		Transaction tx = db.beginTx();
		Node newNode = db.createNode();
		tx.success();
		tx.finish();
		return newNode;
	}
	
	protected Node addNode(GraphDatabaseService db, String name) {
		// Wrap a transaction around node creation.
		Transaction tx = db.beginTx();
		Node newNode = db.createNode();
		newNode.setProperty("name", name);
		tx.success();
		tx.finish();
		return newNode;
	}
	
	protected Relationship addEdge(GraphDatabaseService db, Node first, Node second, RelationshipType edgeType)
	{
		// Wrap a transaction around edge creation.
		Transaction tx = db.beginTx();
		Relationship newRel = first.createRelationshipTo(second, edgeType);
		tx.success();
		tx.finish();
		return newRel;		
	}
	
	protected Relationship addEdge(GraphDatabaseService db, Node first, Node second)
	{
		return addEdge(db, first, second, RelTypes.SUBCLASS_OF);
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
	
	// TODO: Some old test cases still assume this is used. They should be cleaned up.
	private GraphDatabaseService setAllIC(GraphDatabaseService db)
	{
		logger.info("Starting setAllIC");
		Neo4jTraversals.setAllIC(db);
		logger.info("Finished setAllIC");
		return db;
	}

	private GraphDatabaseService loadOntologyDB(String graphLocation) {
		GraphDatabaseService db = new GraphDatabaseFactory().newEmbeddedDatabase(graphLocation);
		return db;
	}
	
	public GraphDatabaseService buildOntologyDB(String url, String graphLocation) {
		return buildOntologyDB(url, graphLocation, true);
	}
	
	public GraphDatabaseService buildOntologyDB(String url, String graphLocation, boolean forceRebuild) {
		GraphFactory factory = new GraphFactory();
		GraphDatabaseService db = null;
		
		String absolutePath = new File(graphLocation).getAbsolutePath();
		String flagPath = absolutePath + "/buildCompletedFlag";
		
		// If we were told to rebuild or the flag isn't set, build from
		// the given url.
		if (forceRebuild || !Files.exists(Paths.get(flagPath)))
		{
			try {
				// Make sure there's nothing there to get in the way.
				FileUtils.deleteDirectory(new File(absolutePath));
				
				OwlUtil.loadOntology(url, graphLocation);
				db = loadOntologyDB(graphLocation);
				
				// Create the flag once the ontology is built.
				new File(flagPath).createNewFile();
			} catch (IOException e) {
				// FIXME: This should be handled better.
				System.out.println(e.getMessage());
			}
		}
		// Otherwise, we've already got the graph stored, and we can use that.
		else
		{
			db = factory.loadOntologyDB(graphLocation);
		}
		
		return db;
	}
	
	public GraphDatabaseService buildOntologyDB(Collection<String> urls, String graphLocation) {
		return buildOntologyDB(urls, graphLocation, true);
	}
	
	public GraphDatabaseService buildOntologyDB(Collection<String> urls, String graphLocation, boolean forceRebuild) {
		GraphDatabaseService db = null;
		
		String absolutePath = new File(graphLocation).getAbsolutePath();
		String flagPath = absolutePath + "/buildCompletedFlag";
		
		// If we were told to rebuild or the flag isn't set, build from
		// the given url.
		if (forceRebuild || !Files.exists(Paths.get(flagPath)))
		{
			try {
				// Make sure there's nothing there to get in the way.
				FileUtils.deleteDirectory(new File(absolutePath));
				
				OwlUtil.loadOntology(urls, graphLocation);
				db = loadOntologyDB(graphLocation);
				
				// Create the flag once the ontology is built.
				new File(flagPath).createNewFile();
			} catch (IOException e) {
				// FIXME: This should be handled better.
				System.out.println(e.getMessage());
			}
		}
		// Otherwise, we've already got the graph stored, and we can use that.
		else
		{
			db = loadOntologyDB(graphLocation);
		}
		
		return db;
	}
	
	/**
	 * Constructs a graph isomorphic to the induced subgraph of the given graph.
	 * 
	 * @param db			The original graph
	 * @param nodes			The nodes whose induced subgraph we want
	 * @param graphLocation	The directory where the resulting graph should be stored
	 */
	public GraphDatabaseService buildSubgraphDB(GraphDatabaseService db,
			Collection<Node> nodes, String graphLocation) {
		String absolutePath = new File(graphLocation).getAbsolutePath();
		// Clean up the target location.
		try {
			FileUtils.deleteDirectory(new File(absolutePath));
		} catch (IOException e) {
			// FIXME: This should be handled better.
			System.out.println(e.getMessage());
		}
		
		// Create a separate Neo4j graph to hold the subgraph.
		GraphDatabaseService subDB = new GraphDatabaseFactory().newEmbeddedDatabase(graphLocation);
		
		// Keep track of corresponding nodes.
		Map<Node, Node> nodeMap = new HashMap<>();
		
		Transaction tx = subDB.beginTx();
		
		// Copy each node.
		for (Node orig : nodes)
		{
			Node copy = addNode(subDB);
			for (String key : orig.getPropertyKeys())
			{
				copy.setProperty(key, orig.getProperty(key));
			}
			
			nodeMap.put(orig, copy);
		}
		
		// Copy each edge.
		for (Node orig : nodes)
		{
			for (Relationship edge : orig.getRelationships(Direction.OUTGOING))
			{
				Node other = edge.getEndNode();
				if (nodes.contains(other))
				{
					Relationship newEdge = addEdge(subDB, nodeMap.get(orig), nodeMap.get(other), edge.getType());
					for (String key : edge.getPropertyKeys())
					{
						newEdge.setProperty(key, edge.getProperty(key));
					}
				}
			}
		}
		
		tx.success();
		tx.finish();
		
		return subDB;
	}
	
}

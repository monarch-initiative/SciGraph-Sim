package org.monarch.sim;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.tooling.GlobalGraphOperations;

public class GraphUtil {

	/**
	 * Removes the given types of edges from a graph.
	 * 
	 * @param db		The database containing the graph
	 * @param edgeTypes	The types of edges to remove
	 */
	public static void cleanEdges(GraphDatabaseService db, Collection<String> edgeTypes) {
		Transaction tx = db.beginTx();

		for (Relationship edge : GlobalGraphOperations.at(db).getAllRelationships())
		{
			if (edgeTypes.contains(edge.getType().name()))
			{
				edge.delete();
			}
		}

		tx.success();
		tx.finish();
	}

	/**
	 * Removes the given type of edge from a graph.
	 * 
	 * @param db		The database containing the graph
	 * @param edgeTypes	The type of edges to remove
	 */
	public static void cleanEdges(GraphDatabaseService db, String edgeType) {
		Collection<String> edgeTypes = new HashSet<>();
		edgeTypes.add(edgeType);
		cleanEdges(db, edgeTypes);
	}

//	/**
//	 * Removes edges of type "EQUIVALENT_TO" from the graph and connects one
//	 * incident node to the other's neighbors.
//	 * 
//	 * @param db	The database containing the graph
//	 */
//	public static void fixEquivalent(GraphDatabaseService db)
//	{
//		// Someone thought using RelationshipType as a wrapper around a string
//		// was a good idea.
//		RelationshipType equivalentType = new RelationshipType() {
//			@Override
//			public String name() {
//				return "EQUIVALENT_TO";
//			}
//		};
//		RelationshipType shortType = new RelationshipType() {
//			@Override
//			public String name() {
//				return "SHORT_EQUIVALENCE";
//			}
//		};
//
//		// Store all the edges and find the ones we care about.
//		Set<Relationship> edges = new HashSet<>();
//		Set<Relationship> equivalentEdges = new HashSet<>();
//		for (Relationship edge : GlobalGraphOperations.at(db).getAllRelationships())
//		{
//			edges.add(edge);
//			if (edge.isType(equivalentType))
//			{
//				equivalentEdges.add(edge);
//			}
//		}
//
//		// Clean up EQUIVALENT_TO edges.
//		while (! equivalentEdges.isEmpty())
//		{
//			Transaction tx = db.beginTx();
//			
//			// Take some EQUIVALENT_TO edge which hasn't been cleared.
//			Relationship edge = equivalentEdges.iterator().next();
//			
//			// Find the endpoint with garbage data.
//			Node dummy = edge.getStartNode();
//			if (! (dummy.hasProperty("fragment") &&
//					((String) dummy.getProperty("fragment")).matches("-?\\d+")))
//			{
//				dummy = edge.getEndNode();
//			}
//			
//			// Clean up all the EQUIVALENT_TO edges which connect to the same
//			// garbage data.
//			Set<Node> startNodes = new HashSet<>();
//			for (Relationship equivalentEdge : dummy.getRelationships(equivalentType))
//			{
//				startNodes.add(equivalentEdge.getOtherNode(dummy));
//				equivalentEdges.remove(equivalentEdge);
//				edges.remove(equivalentEdge);
//				equivalentEdge.delete();
//			}
//			
//			// FIXME: AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
//			
//			tx.success();
//			tx.finish();
//		}
//	}

}

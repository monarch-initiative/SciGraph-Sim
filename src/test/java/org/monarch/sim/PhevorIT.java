package org.monarch.sim;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.tooling.GlobalGraphOperations;

public class PhevorIT {
	
	// FIXME: Remove magic constant.
	final static String FAKE_EDGE = "PHEVOR_SUBCLASS_OF";
	
	static Phevor phevor;
	static GraphDatabaseService db;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		Collection<String> urls = new ArrayList<>();
		urls.add(new File("src/test/resources/ontologies/mouse-go-importer.owl").getAbsolutePath());
		db = new GraphFactory().buildOntologyDB(urls, "target/slowGraph", false);
//		System.out.println("Cleaning up extra edges");
//		postprocessDB();
//		System.out.println("Preprocessing");
//		preprocessDB();
//		System.out.println("Finished preprocessing.");
		Collection<String> edgeTypes = new ArrayList<>();
//		Collection<String> excluded = new HashSet<>();
//		excluded.add("SUPERCLASS_OF");
//		excluded.add("PROPERTY");
//		excluded.add("inSubset");		
//		for (RelationshipType edgeType : GlobalGraphOperations.at(db).getAllRelationshipTypes())
//		{
//			String typeName = edgeType.name();
//			if (!excluded.contains(typeName))
//			{
//				edgeTypes.add(typeName);
//			}
//		}
		edgeTypes.add("SUBCLASS_OF");
		edgeTypes.add(FAKE_EDGE);
		phevor = new Phevor(db, edgeTypes);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		phevor.close();
//		postprocessDB();
	}
	
	private static void preprocessDB() {
		// Get the type of edge we actually care about.
		RelationshipType edgeType = null;
		for (RelationshipType type : GlobalGraphOperations.at(db).getAllRelationshipTypes())
		{
			if (type.name().equals("RO_0002200"))
			{
				edgeType = type;
				break;
			}
		}
		
		// Find all GO and MP nodes annotated with the same gene.
		// TODO: Make this less horrifying.
		for (Node go : GlobalGraphOperations.at(db).getAllNodes())
		{
			if (go.getId() == 0)
			{
				continue;
			}
			
			if (! ((String) go.getProperty("fragment")).startsWith("GO"))
			{
				continue;
			}
			
			for (Relationship goEdge : go.getRelationships(Direction.INCOMING, edgeType))
			{
				
				Node gene = goEdge.getStartNode();
				
				for (Relationship mpEdge : gene.getRelationships(Direction.OUTGOING, edgeType))
				{
					Node mp = mpEdge.getEndNode();
					
					if (! ((String) mp.getProperty("fragment")).startsWith("MP"))
					{
						continue;
					}
					
					RelationshipType extraEdgeType = new RelationshipType() {						
						@Override
						public String name() {
							return FAKE_EDGE;
						}
					};
					Transaction tx = db.beginTx();
					go.createRelationshipTo(mp, extraEdgeType);
					mp.createRelationshipTo(go, extraEdgeType);
					tx.success();
					tx.finish();
				}
			}
		}
	}
	
	private static void postprocessDB() {
		// Clean up all the extra edges we've created.
		Transaction tx;
		for (Relationship edge : GlobalGraphOperations.at(db).getAllRelationships())
		{
			if (edge.getType().name().equals(FAKE_EDGE))
			{
				tx = db.beginTx();
				edge.delete();
				tx.success();
				tx.finish();
			}
		}
	}
	
	private Collection<String> getSampleFragments(int count) {
		// List all the fragments.
		ArrayList<String> fragments = new ArrayList<>();
		for (Node n : GlobalGraphOperations.at(db).getAllNodes())
		{
			if (n.getId() != 0)
			{
				fragments.add((String) n.getProperty("fragment"));
			}
		}
		
		// Shuffle the fragments and take the first few.
		Collections.shuffle(fragments);
		ArrayList<String> first = new ArrayList<>();
		for (String fragment : fragments)
		{
			if (count == 0)
			{
				break;
			}
			
			// FIXME: This should be more general.
			if (fragment.startsWith("GO"))
			{
				first.add(fragment);
				count--;
			}
		}
		
		return first;
	}
	
	private String nodeToString(Node n) {
		return phevor.nodeToString(n);
	}
	
	private class Pair {
		public Node node;
		public double score;
		
		public Pair(Node node) {
			this.node = node;
			Collection<String> fragments = new ArrayList<>();
			fragments.add((String) node.getProperty("fragment"));
			score = phevor.compareOtherNodes(fragments);
		}
	}
	
	private ArrayList<Pair> getSortedScores() {
		// Score all the nodes.
		ArrayList<Pair> scores = new ArrayList<>();
		for (Node n : GlobalGraphOperations.at(db).getAllNodes())
		{
			if (n.getId() != 0)
			scores.add(new Pair(n));
		}
		
		// Order the nodes by score.
		Collections.sort(scores, new Comparator<Pair>() {
			@Override
			public int compare(Pair first, Pair second) {
				if (first.score < second.score)
					return 1;
				else if (second.score < first.score)
					return -1;
				else
					return 0;
			}			
		});
		
		return scores;
	}

	@Test
	public void test() {
		int baseFragments = 5;
		int relevantNodes = 50;
		int relevantGenes = 10;
		
		Map<String, RelationshipType> edgeTypes = new HashMap<>();
		System.out.println("~~ Edge types ~~");
		for (RelationshipType edgeType : GlobalGraphOperations.at(db).getAllRelationshipTypes())
		{
			String typeName = edgeType.toString();
			System.out.println(typeName);
			edgeTypes.put(typeName, edgeType);
		}
		System.out.println("~~~~~~~~~~~~~~~~");
		System.out.println();
		
		Collection<String> fragments = getSampleFragments(baseFragments);
		System.out.println("~~ Base nodes ~~");
		for (String fragment : fragments)
		{
			System.out.println(nodeToString(phevor.getNodeByFragment(fragment)));
		}
		System.out.println("~~~~~~~~~~~~~~~~");
		System.out.println();
		
		phevor.setBaseNodes(fragments);
		
		System.out.println("~~ Related nodes ~~");
		ArrayList<Pair> scores = getSortedScores();
		for (Pair pair : scores)
		{
			if (relevantNodes == 0)
			{
				break;
			}
			relevantNodes--;
			
			Node node = pair.node;
			System.out.println(nodeToString(node) + " --  " + (pair.score * 1_000_000));
			int geneCount = 0;
			for (Relationship edge : node.getRelationships(Direction.INCOMING, edgeTypes.get("RO_0002200")))
			{
				if (geneCount == relevantGenes)
				{
					System.out.println("\t...");
					break;
				}
				System.out.println("\tGENE: " + nodeToString(edge.getStartNode()));
				geneCount++;
			}
		}
		System.out.println("~~~~~~~~~~~~~~~~~~~");
	}

}

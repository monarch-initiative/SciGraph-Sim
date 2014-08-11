package org.monarch.sim;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.tooling.GlobalGraphOperations;

public class PhevorIT {
	
	static Phevor phevor;
	static GraphDatabaseService db;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
//		db = new GraphFactory().buildOntologyDB(
//				new File("src/test/resources/ontologies/retina-test.owl").getAbsolutePath(),
//				"target/retina", true);
		db = new GraphFactory().buildOntologyDB(
			new File("src/test/resources/ontologies/mouse-go-plus-phenotype-importer.owl").getAbsolutePath(),
			"target/mouse-go-plus-phenotype-importer", false);
		System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
		Collection<GraphDatabaseService> dbs = new ArrayList<>();
		dbs.add(db);
		Collection<String []> links = new ArrayList<>();
		Collection<String> edgeTypes = new ArrayList<>();
		Collection<String> excluded = new HashSet<>();
		excluded.add("SUPERCLASS_OF");
		excluded.add("PROPERTY");
		excluded.add("inSubset");		
		for (RelationshipType edgeType : GlobalGraphOperations.at(db).getAllRelationshipTypes())
		{
			String typeName = edgeType.name();
			if (!excluded.contains(typeName))
			{
				edgeTypes.add(typeName);
			}
		}
		phevor = new Phevor(dbs, links, edgeTypes);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		phevor.close();
	}
	
	private Collection<String> getSampleFragments(int count) {
		ArrayList<String> fragments = new ArrayList<>();
		for (Node n : GlobalGraphOperations.at(db).getAllNodes())
		{
			if (n.getId() != 0)
			{
				fragments.add((String) n.getProperty("fragment"));
			}
		}
		Collections.shuffle(fragments);
		
		ArrayList<String> first = new ArrayList<>();
		for (String fragment : fragments)
		{
			if (count == 0)
			{
				break;
			}
			
			first.add(fragment);
			count--;
		}
		
		return first;
	}
	
	private String nodeToString(Node n) {
		String str = "";
		if (n.hasProperty("fragment"))
		{
			str += n.getProperty("fragment") + " ";
		}
		if (n.hasProperty("label"))
		{
			Object label = n.getProperty("label");
			if (label instanceof String [])
			{
				label = ((String []) label)[0];
			}
			str += label + " ";
		}
		else if (n.hasProperty("http://www.w3.org/2000/01/rdf-schema#label"))
		{
			Object label = n.getProperty("http://www.w3.org/2000/01/rdf-schema#label");
			if (label instanceof String [])
			{
				label = ((String []) label)[0];
			}
			str += label + " ";
		}
		
		if (str.equals(""))
		{
			str = n.toString();
		}
		
		return str;
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
		ArrayList<Pair> scores = new ArrayList<>();
		for (Node n : GlobalGraphOperations.at(db).getAllNodes())
		{
			if (n.getId() != 0)
			scores.add(new Pair(n));
		}
		
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
		Collection<String> fragments = getSampleFragments(5);
		System.out.println("~~ Base nodes ~~");
		for (String f : fragments)
		{
			System.out.println(nodeToString(phevor.getNodeByFragment(f)));
		}
		System.out.println("~~~~~~~~~~~~~~~~");
		System.out.println();
		
		phevor.setBaseNodes(fragments);
		
		int relevant = 10;
		ArrayList<Pair> scores = getSortedScores();
		for (Pair pair : scores)
		{
			if (relevant == 0)
			{
				break;
			}
			relevant--;
			
			System.out.println(nodeToString(pair.node) + " --  " + pair.score);
		}
	}

}

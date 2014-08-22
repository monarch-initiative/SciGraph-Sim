package org.monarch.sim;

import java.util.HashSet;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.tooling.GlobalGraphOperations;

public class NaiveTraverserIT {

	static GraphDatabaseService db;
	static MappedDB mapped;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		db = new GraphFactory().buildOntologyDB("src/test/resources/ontologies/mouse-go-importer.owl",
				"target/slowGraph", false);
		mapped = new MappedDB(db);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		mapped.close();
	}

	@Test
	public void test() {
		System.out.println("~~~ EDGE TYPES ~~~");
		for (RelationshipType type : GlobalGraphOperations.at(db).getAllRelationshipTypes())
		{
			System.out.println(type.name());
		}
		System.out.println("~~~~~~~~~~~~~~~~~~");
		
		long start, end;
		NaiveTraverser traverser = new NaiveTraverser(db, "test");
		Set<String> excluded = new HashSet<>();
		excluded.add("SUPERCLASS_OF");
		excluded.add("DISJOINT_WITH");
		excluded.add("PROPERTY");
		excluded.add("PHEVOR_SUBCLASS_OF");
		traverser.excludeEdgeTypes(excluded);
		
		System.out.println("STARTING");
		start = System.nanoTime();
		Set<String> ignored = new HashSet<>();
		ignored.add("EQUIVALENT_TO");
		traverser.pushAllNodes(ignored);
		end = System.nanoTime();
		System.out.println((end - start) / 1_000_000);
		System.out.println("FINISHED");
		
		System.out.println("STARTING");
		start = System.nanoTime();
		traverser.pushAllNodes(new HashSet<String>());
		end = System.nanoTime();
		System.out.println((end - start) / 1_000_000);
		System.out.println("FINISHED");
//		
//		System.out.println("CHILDREN:");
//		for (Node n : traverser.getChildren(base))
//		{
//			System.out.println(mapped.nodeToString(n));
//		}
//		System.out.println();
//		
//		System.out.println("PARENTS:");
//		for (Node n : traverser.getParents(base))
//		{
//			System.out.println(mapped.nodeToString(n));
//		}
//		System.out.println();
//		
//		System.out.println("DESCENDANTS:");
//		for (Node n : traverser.getDescendants(base))
//		{
//			System.out.println(mapped.nodeToString(n));
//		}
//		System.out.println();
//		
//		System.out.println("ANCESTORS:");
//		for (Node n : traverser.getAncestors(base))
//		{
//			System.out.println(mapped.nodeToString(n));
//		}
//		System.out.println();
//		
//		Node other = mapped.getNodeByFragment("COMPLETE:10");
//		System.out.println("LCS:");
//		System.out.println(mapped.nodeToString(traverser.getDummyLCS(base, other)));
	}

}

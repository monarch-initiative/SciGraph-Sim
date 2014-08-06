package org.monarch.sim;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.tooling.GlobalGraphOperations;

public class TraverserTest {

	static GraphDatabaseService db;
	static MappedDB mapped;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		db = new GraphFactory().buildOntologyDB("http://purl.obolibrary.org/obo/upheno/monarch.owl",
				"target/monarch", false);
		mapped = new MappedDB(db);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		mapped.close();
		db.shutdown();
	}

	@Test
	public void test() {
		SciGraphTraverser traverser = new SciGraphTraverser(db, "test");
		for (RelationshipType rt : GlobalGraphOperations.at(db).getAllRelationshipTypes())
		{
			if (!rt.name().equals("SUPERCLASS_OF"))
			{
				traverser.relationships(rt.name(), Direction.INCOMING);
			}
		}
		Node first = mapped.getNodeByFragment("MP:0020184");
		Node second = mapped.getNodeByFragment("GO:0009608");
		System.out.println(traverser.getLCS(first, second));
		System.out.println(traverser.getLCS(second, first));
	}

}

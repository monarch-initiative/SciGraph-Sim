package org.monarch.sim;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.graphdb.Node;

public class CypherTraversals {

	// FIXME: This is horribly slow.
	public static HashSet<Node> getAncestors(Node n, ExecutionEngine engine)
	{
		// Build the appropriate query.
		HashMap<String, Object> params = new HashMap<>();
		params.put("id", n.getId());
		String query = "START ";
		query += "n=node({id}) ";
		query += "MATCH ";
		query += "n-[*0..]->m ";
		query += "RETURN DISTINCT m ";
		
		HashSet<Node> ancestors = new HashSet<>();
		for (Map<String, Object> map : engine.execute(query, params))
		{
			ancestors.add((Node)map.get("m"));
		}
		
		return ancestors;
	}
	
	// FIXME: This is horribly slow.
	public static HashSet<Node> getCommonAncestors(Node first, Node second, ExecutionEngine engine)
	{
		// Build the appropriate query.
		HashMap<String, Object> params = new HashMap<>();
		params.put("firstId", first.getId());
		params.put("secondId", second.getId());
		String query = "START ";
		query += "m=node({firstId}), n=node({secondId}) ";
		query += "MATCH ";
		query += "m-[*0..]->s, n-[*0..]->s ";
		query += "RETURN DISTINCT s ";
		
		HashSet<Node> ancestors = new HashSet<>();
		for (Map<String, Object> map : engine.execute(query, params))
		{
			ancestors.add((Node)map.get("s"));
		}
		
		return ancestors;
	}
	
}

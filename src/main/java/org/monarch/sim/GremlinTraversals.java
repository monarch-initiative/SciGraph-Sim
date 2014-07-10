package org.monarch.sim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;

import com.google.common.base.Optional;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.gremlin.java.GremlinPipeline;
import com.tinkerpop.pipes.PipeFunction;
import com.tinkerpop.pipes.branch.LoopPipe.LoopBundle;

public class GremlinTraversals {

	public static Iterable<Vertex> getParents(Vertex v) {
		LinkedList<Vertex> parents = new LinkedList<>();
		// Follow each outgoing edge to find a parent.
		for (Edge e : v.getEdges(Direction.OUT))
		{
			parents.add(e.getVertex(Direction.IN));
		}
		return parents;
	}
	
	public static HashSet<Vertex> getAncestors(Vertex v) {
		// Keep track of the ancestors we've found.
		HashSet<Vertex> ancestors = new HashSet<>();
		
		GremlinPipeline<Vertex, Vertex> pipe = new GremlinPipeline<>();
		pipe.start(v)
			.as("x")
			// Look at all the neighbors we haven't seen yet.
			.out()
			.except(ancestors)
			.store(ancestors)
			// Repeat until we run out of neighbors.
			.loop("x",
				new PipeFunction<LoopBundle<Vertex>, Boolean>()
				{
					public Boolean compute(LoopBundle<Vertex> bundle)
					{
						return true;
					}
				})
			.iterate()
			;
		
		return ancestors;
	}
	
	// FIXME: This doesn't work.
	public static Iterable<Vertex> getCommonAncestors2(Vertex first, Vertex second) {
		// Pass the vertices in together.
		ArrayList<Vertex> vertices = new ArrayList<>();
		vertices.add(first);
		vertices.add(second);
		
		final Map<Vertex, Number> counts = new HashMap<>();
		
		// We want to loop forever, and we always want to emit our answer.
		PipeFunction<LoopBundle<Vertex>, Boolean> loopFunction =
			new PipeFunction<LoopBundle<Vertex>, Boolean>()
			{
				public Boolean compute(LoopBundle<Vertex> bundle)
				{
					return true;
				}
			};
			
		// Common ancestors show up more than once.
		PipeFunction<Vertex, Boolean> filterFunction =
			new PipeFunction<Vertex, Boolean>()
			{
				public Boolean compute(Vertex v)
				{
					return counts.get(v).intValue() > 1;
				}
			};
		
		GremlinPipeline<ArrayList<Vertex>, Vertex> pipe = new GremlinPipeline<>();
		pipe.start(vertices)
			.as("x")
			.in()
			.loop("x", loopFunction, loopFunction)
			.groupCount(counts)
			.cast(Vertex.class)
			.filter(filterFunction)
			;
		
		return pipe;
	}
	
	public static Iterable<Vertex> getCommonAncestors(Vertex first, Vertex second) {
		HashSet<Vertex> firstAncestors = getAncestors(first);
		HashSet<Vertex> secondAncestors = getAncestors(second);
		firstAncestors.retainAll(secondAncestors);
		return firstAncestors;
	}
	
	public static ArrayList<HashSet<Vertex>> getAncestorsByDistance(Vertex v) {
//		// Keep track of the paths and vertices we've seen.
//		Collection<List<Vertex>> paths = new LinkedList<>();
//		HashSet<Vertex> visited = new HashSet<>();
//		visited.add(v);
//		
//		GremlinPipeline<Vertex, ArrayList<Vertex>> pipe = new GremlinPipeline<>();
//		pipe.start(v)
//			.as("x")
//			// Look at all the neighbors we haven't seen yet.
//			.out()
//			.except(visited)
//			.store(visited)
//			.path()
//			.store((Collection)paths)
//			// FIXME: Get the last node in the path.
//			// Repeat until we run out of neighbors.
//			.loop("x", new PipeFunction<LoopBundle<Vertex>, Boolean>()
//				{
//					public Boolean compute(LoopBundle<Vertex> bundle)
//					{
//						return true;
//					}
//				})
//			;
//
//		for (ArrayList<Vertex> path : pipe)
//		{
//			System.out.println(path);
//		}
//		
//		// Turn our paths into our list of sets of vertices.
//		ArrayList<HashSet<Vertex>> ancestors = new ArrayList<>();
//		for (List<Vertex> path : paths)
//		{
//			// Pad the list to the size we need.
//			int length = path.size();
//			while (length > ancestors.size())
//			{
//				ancestors.add(new HashSet<Vertex>());
//			}
//			
//			ancestors.get(length - 1).add((Vertex)path.get(length - 1));
//		}
//		
//		return ancestors;
		// FIXME: Make this work.
		return null;
	}
	
	public static Optional<ArrayList<Vertex>> shortestPath(Vertex start, final Vertex end) {
		// Keep track of the vertices we've seen.
		HashSet<Vertex> visited = new HashSet<>();
		visited.add(start);
		
		GremlinPipeline<Vertex, ArrayList<Vertex>> pipe = new GremlinPipeline<>();
		pipe.start(start)
			.as("x")
			// Look at all the neighbors we haven't seen yet.
			.out()
			.except(visited)
			.store(visited)
			// Repeat until we find what we're looking for or run out of neighbors.
			.loop("x", new PipeFunction<LoopBundle<Vertex>, Boolean>()
				{
					public Boolean compute(LoopBundle<Vertex> bundle)
					{
						return bundle.getObject().getId() != end.getId();
					}
				})
			.path()
			;
		
		// Pull out the shortest path.
		for (ArrayList<Vertex> path : pipe)
		{
			return Optional.of(path);
		}
		
		// If we no such path exists, return a sentinel value.
		return Optional.absent();
	}
	
}

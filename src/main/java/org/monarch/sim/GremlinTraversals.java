package org.monarch.sim;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;

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
		
		ancestors.add(v);		
		return ancestors;
	}
	
	public static Iterable<Vertex> getCommonAncestors(Vertex first, Vertex second) {
		HashSet<Vertex> firstAncestors = getAncestors(first);
		HashSet<Vertex> secondAncestors = getAncestors(second);
		firstAncestors.retainAll(secondAncestors);
		return firstAncestors;
	}
	
	// FIXME: This doesn't work.
//	public static Iterable<Vertex> getCommonAncestors2(Vertex first, Vertex second) {
//		// Pass the vertices in together.
//		final ArrayList<Vertex> vertices = new ArrayList<>();
//		vertices.add(first);
//		vertices.add(second);
//		
//		final Map<Vertex, Number> counts = new HashMap<>();
//		
//		// We want to loop forever, and we always want to emit our answer.
//		PipeFunction<LoopBundle<Vertex>, Boolean> trueFunction =
//			new PipeFunction<LoopBundle<Vertex>, Boolean>()
//			{
//				public Boolean compute(LoopBundle<Vertex> bundle)
//				{
//					return true;
//				}
//			};
//			
//		// Common ancestors show up more than once.
//		PipeFunction<Vertex, Boolean> filterFunction =
//			new PipeFunction<Vertex, Boolean>()
//			{
//				public Boolean compute(Vertex v)
//				{
//					boolean repeated = counts.get(v).intValue() > 1;
//					boolean startNode = vertices.contains(v);
//					return repeated || startNode;
//				}
//			};
//		
//		GremlinPipeline<ArrayList<Vertex>, Vertex> pipe = new GremlinPipeline<>();
//		pipe.start(vertices)
//			// Expand out from each of the starting vertices.
//			.as("x")
//			.out()
//			.loop("x", trueFunction, trueFunction)
//			// Every node that shows up more than once is a common ancestor.
//			.groupCount(counts)
//			.cast(Vertex.class)
//			.filter(filterFunction)
//			;
//		
//		if (!first.equals(second))
//		{
//			return pipe;
//		}
//		
//		// TODO: If there's a way to put a node directly into a pipe, we should do that instead.
//		// The above doesn't handle the case where the nodes are the same.
//		// This horrible hack does.
//		List<Vertex> ancestors = new LinkedList<>();
//		for (Vertex v : pipe)
//		{
//			ancestors.add(v);
//		}
//		ancestors.add(first);
//		return ancestors;
//	}
	
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

package org.monarch.sim;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

/**
 * This class allows comparisons between collections of nodes.
 * It find a collection of edges of minimal total length which transforms
 * one collection of nodes into another.
 * 
 * @author spikeharris
 */
public class GraphPathComparison {
	
	private NaiveTraverser traverser;

	public GraphPathComparison(NaiveTraverser traverser) {
		
		this.traverser = traverser;
		
	}
	
	/**
	 * Finds the edges in the minimal transformation from one collection of nodes to another.
	 * 
	 * @param first		The starting collection of nodes
	 * @param second	The target collection of nodes
	 */
	public Collection<Relationship> getMinimalChange(Collection<Node> first, Collection<Node> second) {		
		Map<Node, RootPath> shortestPaths = new HashMap<>();
		
		// Find the nearest pairs of nodes.
		for (Node firstNode : first)
		{
			for (Node secondNode : second)
			{
				RootPath path = new RootPath(firstNode, secondNode);

				if (! shortestPaths.containsKey(firstNode) || path.length < shortestPaths.get(firstNode).length)
				{
					shortestPaths.put(firstNode, path);
				}
				
				if (! shortestPaths.containsKey(secondNode) || path.length < shortestPaths.get(secondNode).length)
				{
					shortestPaths.put(secondNode, path);
				}
			}
		}
		
		Collection<RootPath> untangled = new HashSet<>();
		Queue<RootPath> toUntangle = new ArrayDeque<>();
		toUntangle.addAll(shortestPaths.values());
		
		// Untangle all crossed paths.
		untangleAll(untangled, toUntangle);
		
		// FIXME: Clean unnecessary paths.

		Collection<Relationship> edges = new HashSet<>();
		
		// Pull edges out of paths.
		for (RootPath path : untangled)
		{
			edges.addAll(path.getEdges());
		}
		return edges;
	}
	
	private void untangleAll(Collection<RootPath> untangled, Queue<RootPath> toUntangle) {
		while (! toUntangle.isEmpty())
		{
			untangleNext(untangled, toUntangle);
		}
	}
	
	private void untangleNext(Collection<RootPath> untangled, Queue<RootPath> toUntangle) {
		boolean intersect = false;
		RootPath next = toUntangle.remove();
		
		// If we've got a trivial path, throw it out.
		if (next.first.equals(next.second))
		{
			return;
		}
		
		// Check for intersections.
		// FIXME: There are lots of types of intersection not considered here.
		for (RootPath prev : untangled)
		{
			if (prev.root.equals(next.root))
			{
				// If we've already got this path, throw one copy out.
				if (prev.first.equals(next.first) && prev.second.equals(next.second))
				{
					untangled.remove(prev);
					break;
				}
				if (prev.first.equals(next.second) && prev.second.equals(next.first))
				{
					untangled.remove(prev);
					break;
				}
				
				Collection<Node> nodesAbove = new ArrayList<>();
				nodesAbove.add(prev.root);
				
				// If two paths share a leg, try to merge the other two legs.
				if (prev.first.equals(next.first))
				{
					intersect = true;
					untangled.remove(prev);
					RootPath middle = new RootPath(prev.second, next.second, nodesAbove);
					toUntangle.add(middle);
					toUntangle.add(new RootPath(prev.first, middle.root, nodesAbove));
					break;
				}
				if (prev.first.equals(next.second))
				{
					intersect = true;
					untangled.remove(prev);
					RootPath middle = new RootPath(prev.second, next.first, nodesAbove);
					toUntangle.add(middle);
					toUntangle.add(new RootPath(prev.first, middle.root, nodesAbove));
					break;
				}
				if (prev.second.equals(next.first))
				{
					intersect = true;
					untangled.remove(prev);
					RootPath middle = new RootPath(prev.first, next.second, nodesAbove);
					toUntangle.add(middle);
					toUntangle.add(new RootPath(prev.second, middle.root, nodesAbove));
					break;
				}
				if (prev.second.equals(next.second))
				{
					intersect = true;
					untangled.remove(prev);
					RootPath middle = new RootPath(prev.first, next.first, nodesAbove);
					toUntangle.add(middle);
					toUntangle.add(new RootPath(prev.second, middle.root, nodesAbove));
					break;
				}
			}
			
			// Otherwise, we don't care about the cases where endpoints overlap.
			// FIXME: We probably should.
			if (prev.contains(next.first) || prev.contains(next.second) || prev.contains(next.root))
			{
				continue;
			}
			if (next.contains(prev.first) || next.contains(prev.second) || next.contains(prev.root))
			{
				continue;
			}

			Collection<Node> nodesAbove = new ArrayList<>();
			nodesAbove.add(prev.root);
			nodesAbove.add(next.root);
			
			// If we find an intersection, we want to make three paths from the two.
			if (intersect(prev.firstPath, next.firstPath))
			{
				intersect = true;
				untangled.remove(prev);
				RootPath middle = new RootPath(prev.first, next.first, nodesAbove);
				toUntangle.add(middle);
				toUntangle.add(new RootPath(prev.second, middle.root, Arrays.asList(prev.root)));
				toUntangle.add(new RootPath(next.second, middle.root, Arrays.asList(next.root)));
				break;
			}			
			if (intersect(prev.firstPath, next.secondPath))
			{
				intersect = true;
				untangled.remove(prev);
				RootPath middle = new RootPath(prev.first, next.second, nodesAbove);
				toUntangle.add(middle);
				toUntangle.add(new RootPath(prev.second, middle.root, Arrays.asList(prev.root)));
				toUntangle.add(new RootPath(next.first, middle.root, Arrays.asList(next.root)));
				break;
			}			
			if (intersect(prev.secondPath, next.firstPath))
			{
				intersect = true;
				untangled.remove(prev);
				RootPath middle = new RootPath(prev.second, next.first, nodesAbove);
				toUntangle.add(middle);
				toUntangle.add(new RootPath(prev.first, middle.root, Arrays.asList(prev.root)));
				toUntangle.add(new RootPath(next.second, middle.root, Arrays.asList(next.root)));
				break;
			}			
			if (intersect(prev.secondPath, next.secondPath))
			{
				intersect = true;
				untangled.remove(prev);
				RootPath middle = new RootPath(prev.second, next.second, nodesAbove);
				toUntangle.add(middle);
				toUntangle.add(new RootPath(prev.first, middle.root, Arrays.asList(prev.root)));
				toUntangle.add(new RootPath(next.first, middle.root, Arrays.asList(next.root)));
				break;
			}
		}
		
		// If we didn't find any intersections, save the path.
		if (! intersect)
		{
			untangled.add(next);
		}
	}
	
	private boolean intersect(List<Node> firstPath, List<Node> secondPath) {
		for (Node n : firstPath)
		{
			if (secondPath.contains(n))
			{
				return true;
			}
		}
		return false;
	}

	private class RootPath {
		public Node first;
		public Node second;
		public Node root;
		public List<Node> firstPath;
		public List<Node> secondPath;
		public double length = Double.MAX_VALUE;
		
		public RootPath(Node first, Node second) {
			this.first = first;
			this.second = second;
			root = traverser.getLCS(first, second);
			firstPath = traverser.getShortestPath(first, root);
			secondPath = traverser.getShortestPath(second, root);
			if (root != null)
			{
				length = traverser.getIC(first) + traverser.getIC(second) - 2 * traverser.getIC(root);
			}
		}
		
		public RootPath(Node first, Node second, Collection<Node> nodesAbove) {
			this.first = first;
			this.second = second;
			Collection<Node> ancestors = traverser.getAncestors(first);
			ancestors.retainAll(traverser.getAncestors(second));
			
			root = null;
			double ic = -1;
			
			// Find the least subsumer with the given nodes as ancestors.
			outer:
				for (Node ancestor : ancestors)
				{
					double ancestorIC = traverser.getIC(ancestor);
					if (ancestorIC < ic)
					{
						continue;
					}
					
					Collection<Node> farAncestors = traverser.getAncestors(ancestor);
					
					for (Node above : nodesAbove)
					{
						if (! farAncestors.contains(above))
						{
							continue outer;
						}
					}
					
					root = ancestor;
					ic = ancestorIC;
				}
			
			firstPath = traverser.getShortestPath(first, root);
			secondPath = traverser.getShortestPath(second, root);
			if (root != null)
			{
				length = traverser.getIC(first) + traverser.getIC(second) - 2 * traverser.getIC(root);
			}
		}
		
		public boolean contains(Node n) {
			return firstPath.contains(n) || secondPath.contains(n);
		}
		
		public Collection<Relationship> getEdges() {
			Collection<Relationship> edges = new HashSet<>();

			edges.addAll(getEdgesFromNodes(firstPath));
			edges.addAll(getEdgesFromNodes(secondPath));
			
			return edges;
		}
		
		private Collection<Relationship> getEdgesFromNodes(List<Node> nodes) {
			Collection<Relationship> edges = new ArrayList<Relationship>();
			Iterator<Node> iter = nodes.iterator();			
			Node prev = null;
			Node next = iter.next();
			
			// Find the edge connecting each pair of adjacent nodes.
			while (iter.hasNext())
			{
				prev = next;
				next = iter.next();
				for (Relationship edge : prev.getRelationships(Direction.OUTGOING))
				{
					if (edge.getEndNode().equals(next))
					{
						edges.add(edge);
						break;
					}
				}
			}	
			
			return edges;
		}
	}
	
}

package qut.pm.prom.helpers;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.processmining.models.graphbased.directed.AbstractDirectedGraph;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.elements.Arc;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.TimedTransition;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.graphbased.directed.petrinet.impl.StochasticNetMappingCloner;
import org.processmining.models.graphbased.directed.utils.GraphIterator;
import org.processmining.models.graphbased.directed.utils.GraphIterator.EdgeAcceptor;
import org.processmining.models.graphbased.directed.utils.GraphIterator.NodeAcceptor;
import org.processmining.models.semantics.petrinet.Marking;

public class StochasticPetriNetUtils {
	
	private static Logger LOGGER = LogManager.getLogger();
	
	private static final double EPSILON = 0.00001d;

	/**
	 * Doesn't work for nets with duplicate labels. 
	 * 
	 * @param o1
	 * @param o2
	 * @return
	 */
	public static boolean areEqual(StochasticNet o1, StochasticNet o2) {
		if (o1 == null && o2 == null)
			return true;
		if (o1 == null || o2 == null)
			return false;
		if (!checkPlaces(o1, o2))
			return false;
		return checkTransitions(o1, o2);		
	}

	private static boolean checkTransitions(StochasticNet o1, StochasticNet o2) {
		Map <String,Transition> o1TransitionMap = o1.getTransitions().stream().collect(
										Collectors.toMap(Transition::getLabel, Function.identity()));
		for (Transition t: o2.getTransitions()) {
			Transition o1Transition = o1TransitionMap.get(t.getLabel());
			if (o1Transition instanceof TimedTransition) {
				if (!areEqual((TimedTransition)o1Transition,(TimedTransition)t)) {
					LOGGER.debug("Not equal: transitions {} != {}",t,o1Transition);
					return false;
				}
			}else {
				if (!areEqual(o1Transition,t))
					return false;
			}
		}
		return true;
	}

	private static boolean checkPlaces(StochasticNet o1, StochasticNet o2) {
		Map<String,Place> o1PlaceMap = o1.getPlaces().stream().collect(
										Collectors.toMap(Place::getLabel, Function.identity()));
		for (Place p: o2.getPlaces()) {
			Place o1Place = o1PlaceMap.get(p.getLabel());
			if (!areEqual(o1Place,p)) {
				LOGGER.debug("Not equal: places {} != {}",p, o1Place);
				return false;
			}
		}
		return true;
	}

	public static boolean areEqual(Transition t1, Transition t2) {
		if (t1 == null && t2 == null)
			return true;
		if (t1 == null || t2 == null)
			return false;
		if (t1.isInvisible() != t2.isInvisible())
			return false;
		return areEqual( (PetrinetNode)t1, t2);		
	}
	
	public static boolean areEqual(TimedTransition t1, TimedTransition t2) {
		if (t1 == null && t2 == null)
			return true;
		if (t1 == null || t2 == null)
			return false;
		
		if ( Math.abs( t1.getWeight() - t2.getWeight() ) > EPSILON  
			|| (!t1.getDistributionType().equals(t2.getDistributionType()) ) )
		{
			return false;
		}
		return areEqual( (Transition)t1, t2);
	}
	
	public static boolean areEqual(PetrinetNode p1, PetrinetNode p2) {
		if (p1 == null && p2 == null)
			return true;
		if (p1 == null || p2 == null)
			return false;
		if (! p1.getLabel().equals(p2.getLabel()))
			return false;
		return areEqualForSharedNode( p1.getGraph().getInEdges(p1),
						 p2.getGraph().getInEdges(p2));

	}

	
	private static class EdgeKey{
		private String sourceLabel;
		private String targetLabel;
		
		
		public EdgeKey(PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge) {
			super();
			this.sourceLabel = edge.getSource().getLabel();
			this.targetLabel = edge.getTarget().getLabel();
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((sourceLabel == null) ? 0 : sourceLabel.hashCode());
			result = prime * result + ((targetLabel == null) ? 0 : targetLabel.hashCode());
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			EdgeKey other = (EdgeKey) obj;
			if (sourceLabel == null) {
				if (other.sourceLabel != null)
					return false;
			} else if (!sourceLabel.equals(other.sourceLabel))
				return false;
			if (targetLabel == null) {
				if (other.targetLabel != null)
					return false;
			} else if (!targetLabel.equals(other.targetLabel))
				return false;
			return true;
		}
		
		
	}

	/**
	 * Tests whether two sets of edges are equivalent. Each set of edges must be coming out of a 
	 * single node. Intended for use in testing the equivalence of two nodes in different nets.
	 * 
	 * Precondition: no duplicate labels - use areEqualWithDupes
	 * @param edges1
	 * @param edges2
	 * @return
	 */
	public static boolean areEqualForSharedNode(Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> edges1, 
								   Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> edges2) {
		if (edges1.size() != edges2.size())
			return false;
		
		Map<EdgeKey,  PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> edgeKeyMap1 = edges1.stream().collect(
				Collectors.toMap(e -> new EdgeKey(e), 
						         e -> e));
		for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge2: edges2) {
			EdgeKey key = new EdgeKey(edge2);
			PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge1 = edgeKeyMap1.get(key);
			if (edge1 == null)
				return false;
			if (edge1 instanceof Arc && edge2 instanceof Arc) {
				Arc arc1 = (Arc)edge1; Arc arc2 = (Arc)edge2;  
				if (arc1.getWeight() != arc2.getWeight()) {
					return false;
				}
			}
		}
		return true;
	}
	
	public static boolean areEqualWithDupes(StochasticNet o1, StochasticNet o2, 
										    NodeMapper nodeMapper1, NodeMapper nodeMapper2) 
	{
		if (o1 == null && o2 == null)
			return true;
		if (o1 == null || o2 == null)
			return false;
		// Yes, creating new nets is inefficient. But it's very awkward to constantly work around the label mapping as well
		StochasticNet new1 = StochasticNetMappingCloner.cloneFromStochasticNet(o1, nodeMapper1);
		StochasticNet new2 = StochasticNetMappingCloner.cloneFromStochasticNet(o2, nodeMapper2);
		return areEqual(new1, new2);
	}
	



	public static Collection<Transition> findAllSuccessors(Transition transition) {
	
		final NodeAcceptor<PetrinetNode> nodeAcceptor = new NodeAcceptor<PetrinetNode>() {
			public boolean acceptNode(PetrinetNode node, int depth) {
				return ((depth != 0) && (node instanceof Transition) );
			}
		};
	
		Collection<PetrinetNode> transitions = GraphIterator.getDepthFirstSuccessors(transition, transition.getGraph(),
				new EdgeAcceptor<PetrinetNode, PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>>() {
	
					public boolean acceptEdge(PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge,
							int depth) {
						return !nodeAcceptor.acceptNode(edge.getSource(), depth);
					}
				}, nodeAcceptor);
	
		return Arrays.asList(transitions.toArray(new Transition[0]));
	}

	public static Collection<Transition> findAllPredecessors(Transition transition) {
		
		final NodeAcceptor<PetrinetNode> nodeAcceptor = new NodeAcceptor<PetrinetNode>() {
			public boolean acceptNode(PetrinetNode node, int depth) {
				return ((depth != 0) && (node instanceof Transition) );
			}
		};
	
		Collection<PetrinetNode> transitions = GraphIterator.getDepthFirstPredecessors(transition, 
				transition.getGraph(),
				new EdgeAcceptor<PetrinetNode, PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>>() {
	
					public boolean acceptEdge(PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge,
							int depth) {
						return !nodeAcceptor.acceptNode(edge.getTarget(), depth);
					}
				}, nodeAcceptor);
	
		return Arrays.asList(transitions.toArray(new Transition[0]));
	}

	
	public static Collection<Transition> findAllSiblings(Transition transition) {
		Collection<PetrinetNode> transitions = new HashSet<PetrinetNode>();
		AbstractDirectedGraph<PetrinetNode, PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> net = 
				transition.getGraph();
		Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> incomingEdges = 
				net.getInEdges(transition);
		for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge: incomingEdges) {
			Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> incomingSiblingEdges = net.getOutEdges(edge.getSource());
			for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edgeSibling: incomingSiblingEdges) {
				transitions.add( edgeSibling.getTarget() );
			}
		}
		return Arrays.asList(transitions.toArray(new Transition[0]));
	}

	public static Collection<Transition> predecessors(Place place) {
		Collection<Transition> result = new HashSet<Transition>();
		for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge: place.getGraph().getInEdges(place)) {
			result.add((Transition)edge.getSource());
		}
		return result;
	}

	public static Collection<Transition> successors(Place place) {
		Collection<Transition> result = new HashSet<Transition>();
		for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge: place.getGraph().getOutEdges(place)) {
			result.add((Transition)edge.getTarget());
		}
		return result;
	}

	public static Collection<Place> predecessors(Transition transition) {
		Collection<Place> result = new HashSet<>();
		for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge: transition.getGraph().getInEdges(transition)) {
			result.add((Place)edge.getSource());
		}
		return result;
	}

	public static Collection<Place> successors(Transition transition) {
		Collection<Place> result = new HashSet<>();
		for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge: transition.getGraph().getOutEdges(transition)) {
			result.add((Place)edge.getTarget());
		}
		return result;
	}

	/**
	 * Pre: unique label equivalence for places between the two nets. This is needed because 
	 * a number of valid and interesting nets don't have input places with no incoming edges, ie,
	 * they are not WorkflowNets. InductiveMiner and other miners can produce such nets under
	 * important edge cases.
	 * 
	 * An earlier version of this checked for <code>net.getInEdges(newPlace).isEmpty()</code>.
	 * 
	 * @param initialMarking
	 * @param net
	 * @return
	 */
	public static Marking findEquivalentInitialMarking(Marking initialMarking, StochasticNet net) {
		Marking newMarking = new Marking();
		for (Place oldPlace: initialMarking) {
			for (Place newPlace: net.getPlaces()) {
				if (oldPlace.getLabel().equals(newPlace.getLabel())) {
					newMarking.add(newPlace);
					return newMarking;
				}
			}
		}
		return newMarking;
	}

	public static Set<Marking> findEquivalentFinalMarkings(Set<Marking> finalMarkings, StochasticNet net) {
		Set<Marking> newMarkings = new HashSet<>();
		for (Marking finalMarking: finalMarkings) {
			Marking newMarking = new Marking();
			for (Place oldPlace: finalMarking) {
				for (Place newPlace: net.getPlaces()) {
					if (oldPlace.getLabel().equals(newPlace.getLabel()) 
							&& net.getOutEdges(newPlace).isEmpty() ) {
						newMarking.add(newPlace);
					}
				}
			}
			newMarkings.add(newMarking);
		}
		return newMarkings;
	}

	/**
	 * StochasticPetriNet2StochasticDeterministicFiniteAutomatonPlugin.guessInitialMarking() by Leemans
	 * 
	 * @param net
	 * @return
	 */
	public static Marking guessInitialMarking(Petrinet net) {
		Marking result = new Marking();
		for (Place p : net.getPlaces()) {
			if (net.getInEdges(p).isEmpty()) {
				result.add(p);
			}
		}
		return result;
	}
	
	/**
	 * Health warning - this simply finds places with only incoming arcs. It will behave
	 * for sound workflow nets, but may be quite different from the set of all possible final 
	 * markings given particular initial markings. 
	 * 
	 * @param finalMarkings
	 * @param net
	 * @return
	 */
	public static Set<Marking> guessFinalMarkingsAsIfJustFinalPlaces(Petrinet net) {
		Set<Marking> newMarkings = new HashSet<>();
		Marking newMarking = new Marking();
		for (Place place: net.getPlaces()) {
			if ( net.getOutEdges(place).isEmpty() && 
					!net.getInEdges(place).isEmpty() ) 
			{
				newMarking.add(place);
			}
		}
		newMarkings.add(newMarking);
		return newMarkings;
	}
	
}

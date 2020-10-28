package org.processmining.models.graphbased.directed.petrinet.impl;

import java.util.HashMap;
import java.util.Map;

import org.processmining.models.graphbased.AttributeMap;
import org.processmining.models.graphbased.directed.DirectedGraphElement;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.elements.Arc;
import org.processmining.models.graphbased.directed.petrinet.elements.InhibitorArc;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.ResetArc;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;

import qut.pm.prom.helpers.NodeMapper;

/**
 * 
 * @author burkeat
 *
 */
public class StochasticNetMappingCloner extends StochasticNetImpl{
// Must be in this package due to needing the internals of AbstractResetInhibitorNet, eg net.arcs

	private NodeMapper nodeMapper;
	
	public StochasticNetMappingCloner(String label, NodeMapper nodeMapper) {
		super(label);
		this.nodeMapper = nodeMapper;
	}

	public static StochasticNet cloneFromStochasticNet(StochasticNet other, NodeMapper nodeMapper) {
		StochasticNetMappingCloner cloner = new StochasticNetMappingCloner(other.getLabel(), nodeMapper);
		cloner.cloneFrom((AbstractResetInhibitorNet)other, true, true, true, true, true);
		return cloner;
	}
	
	@Override
	protected synchronized Map<DirectedGraphElement, DirectedGraphElement> cloneFrom(AbstractResetInhibitorNet net,
			boolean transitions, boolean places, boolean arcs, boolean resets, boolean inhibitors) 
	{
		HashMap<DirectedGraphElement, DirectedGraphElement> mapping = new HashMap<DirectedGraphElement, DirectedGraphElement>();	

		if (transitions) {
			for (Transition t : net.transitions) {
				Transition copy = addTransition( nodeMapper.getId( t.getId() )  );
				copy.setInvisible(t.isInvisible());
				mapping.put(t, copy);
			}
		}
		if (places) {
			for (Place p : net.places) {
				Place copy = addPlace(p.getLabel());
				mapping.put(p, copy);
			}
		}
		if (arcs) {
			for (Arc a : net.arcs) {
				mapping.put(a, addArcPrivate((PetrinetNode) mapping.get(a.getSource()), (PetrinetNode) mapping.get(a
						.getTarget()), a.getWeight(), a.getParent()));
			}
		}
		if (inhibitors) {
			for (InhibitorArc a : net.inhibitorArcs) {
				mapping.put(a, addInhibitorArc((Place) mapping.get(a.getSource()), (Transition) mapping.get(a
						.getTarget()), a.getLabel()));
			}
		}
		if (resets) {
			for (ResetArc a : net.resetArcs) {
				mapping.put(a, addResetArc((Place) mapping.get(a.getSource()), (Transition) mapping.get(a.getTarget()),
						a.getLabel()));
			}
		}

		getAttributeMap().clear();
		AttributeMap map = net.getAttributeMap();
		for (String key : map.keySet()) {
			getAttributeMap().put(key, map.get(key));
		}

		return mapping;
	}
	
	
}
package qut.pm.spm;

import java.util.HashSet;
import java.util.Set;

import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.semantics.petrinet.Marking;

public class AcceptingStochasticNet {

	private String id;
	private StochasticNet net;
	private Marking initialMarking;
	private Set<Marking> finalMarkings;
	
	public AcceptingStochasticNet(String id, StochasticNet net, Marking initialMarking) {
		this(id,net,initialMarking,new HashSet<>());
	}
	
	public AcceptingStochasticNet(String id, StochasticNet net, Marking initialMarking,
			Set<Marking> finalMarkings) {
		super();
		this.id = id;
		this.net = net;
		this.initialMarking = initialMarking;
		this.finalMarkings = finalMarkings;
	}
	
	public String getId() {
		return id;
	}
	
	public StochasticNet getNet() {
		return net;
	}

	public Marking getInitialMarking() {
		return initialMarking;
	}

	public Set<Marking> getFinalMarkings() {
		return finalMarkings;
	}
	
}

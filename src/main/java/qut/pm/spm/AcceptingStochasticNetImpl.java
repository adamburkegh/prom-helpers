package qut.pm.spm;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.semantics.petrinet.Marking;

import qut.pm.prom.helpers.PetrinetExportUtils;

public class AcceptingStochasticNetImpl implements AcceptingStochasticNet{

	private static Logger LOGGER = LogManager.getLogger();
	
	private String id;
	private StochasticNet net;
	private Marking initialMarking;
	private Set<Marking> finalMarkings;
	
	public AcceptingStochasticNetImpl(String id, StochasticNet net, Marking initialMarking) {
		this(id,net,initialMarking,new HashSet<>());
	}

	public AcceptingStochasticNetImpl(StochasticNet net, Marking initialMarking,
			Set<Marking> finalMarkings) {
		this(net.getLabel(),net,initialMarking,finalMarkings);
	}

	
	public AcceptingStochasticNetImpl(String id, StochasticNet net, Marking initialMarking,
			Set<Marking> finalMarkings) {
		super();
		this.id = id;
		this.net = net;
		this.initialMarking = initialMarking;
		this.finalMarkings = finalMarkings;
	}
	
	@Override
	public String getId() {
		return id;
	}
	
	@Override
	public StochasticNet getNet() {
		return net;
	}

	@Override
	public Marking getInitialMarking() {
		return initialMarking;
	}

	@Override
	public Set<Marking> getFinalMarkings() {
		return finalMarkings;
	}


	@Override
	public void setInitialMarking(Marking initialMarking) {
		this.initialMarking = initialMarking;
	}

	@Override
	public void setFinalMarkings(Set<Marking> finalMarkings) {
		this.finalMarkings = finalMarkings;		
	}


	@Override
	public void exportToFile(PluginContext context, File file) throws IOException {
		try {
			PetrinetExportUtils.storePNMLModel(file,net);
		}catch(Exception e) {
			LOGGER.error("Unable to export",e);
			throw new IOException("Unable to export",e);
		}
		
	}
	
}
